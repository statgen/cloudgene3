package cloudgene.mapred.plugins.nextflow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.jobs.*;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.plugins.nextflow.report.CommandOutput;
import cloudgene.mapred.util.MapValueParser;
import cloudgene.mapred.wdl.WdlParameterInput;
import cloudgene.mapred.wdl.WdlParameterInputType;
import cloudgene.mapred.wdl.WdlParameterOutput;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.util.config.Settings;
import cloudgene.mapred.wdl.WdlStep;
import groovy.json.JsonOutput;

public class NextflowStep extends CloudgeneStep {

	private static final Logger log = LoggerFactory.getLogger(NextflowStep.class);

	private static final String PROPERTY_PROCESS_CONFIG = "processes";
	private static final String PROPERTY_GROUPS_CONFIG = "groups";

	private CloudgeneContext context;
	private final Map<String, Message> messages = new HashMap<>();
	private final Map<String, NextflowProcessConfig> configs = new HashMap<>();
	private final NextflowCollector collector = NextflowCollector.getInstance();

	@Override
	public boolean run(@NonNull WdlStep step, @NonNull CloudgeneContext context) {

		this.context = context;

		Settings settings = context.getSettings();

		String script = step.getString("script");

		if (script == null) {
			// no script set. try to find a main.nf. IS the default convention of nf-core.
			script = "main.nf";
		}

		Path scriptPath = Path.of(script);
		if (!scriptPath.isAbsolute()) {
			scriptPath = Path.of(context.getWorkingDirectory(), script);
		}
		if (!Files.exists(scriptPath)) {
			context.log("Warning: Nextflow script '" + scriptPath + "' not found. "
					+ "Try to resolve it on github as '" + script + "'");
			scriptPath = Path.of(script);
		}

		String revision = step.getString("revision");

		// load process styling
		loadProcessConfigs(step.get(PROPERTY_PROCESS_CONFIG));
		Map<String, Step> groups = loadGroups(step.get(PROPERTY_GROUPS_CONFIG));
		if (groups.isEmpty()) {
			context.createStep(step.getName());
		}
		for (NextflowProcessConfig config : configs.values()) {
			if (config.getGroup() != null) {
				config.setStep(groups.get(config.getGroup()));
			}
		}

		NextflowBinary nextflow = NextflowBinary.build(settings);
		nextflow.setScript(scriptPath);
		nextflow.setRevision(revision);

		AbstractJob job = context.getJob();
		String appFolder = settings.getApplicationRepository().getConfigDirectory(job.getApplicationId());

		NextflowPlugin plugin = (NextflowPlugin) PluginManager.getInstance().getPlugin(NextflowPlugin.ID);

		Map<String, String> nextflowSettings;
		try {
			nextflowSettings = plugin.getConfig(context.getJob().getApp());
		} catch (Exception e) {
			log.error("Failed to load Nextflow settings.", e);
			return false;
		}

		// set profile
		String profile = nextflowSettings.get("nextflow.profile");
		nextflow.setProfile(profile);

		// set global configuration
		String globalConfig = plugin.getNextflowConfig();
		nextflow.addConfig(globalConfig);

		// set application specific configuration
		Path appConfig = Path.of(appFolder, "nextflow.config");
		nextflow.addConfig(appConfig);

		String globalEnv = plugin.getNextflowEnv();
		nextflow.addEnvScript(Path.of(globalEnv));

		Path appEnv = Path.of(appFolder, "nextflow.env");
		nextflow.addEnvScript(appEnv);

		// set work directory
		IWorkspace workspace = job.getWorkspace();

		// use workdir if set in settings
		String workDir = nextflowSettings.get("nextflow.work");
		if (workDir == null || workDir.isBlank()) {
			workDir = workspace.createTempFolder("nextflow");
		}

		// register job in web collector and set created url
		String weblogURL = collector.addContext(context, configs);

		String prefix = "step" + context.getStepCounter() + "-";

		// log files and reports
		String trace = workspace.createLogFile(prefix + "trace.csv");
		String report = workspace.createLogFile(prefix + "report.html");
		String timeline = workspace.createLogFile(prefix + "timeline.html");

		if (!writeParamsJson(prefix, step, nextflow)) return false;
		if (!writeNextflowConfig(prefix, workDir, weblogURL, trace, report, timeline, nextflow)) return false;

		Path logPath = Path.of(job.getLocalWorkspace(), prefix + "nextflow.log");
		nextflow.setLogFile(logPath);

		try {
			File executionDir = new File(context.getLocalOutput());
			StringBuilder output = new StringBuilder();

			int ret = executeCommand(nextflow.buildCommand(), context, output, executionDir);
			boolean success = (ret == 0);

			if (!success) {
				// set all running processes to failed
				List<NextflowProcess> processes = collector.getProcesses(context);
				for (NextflowProcess process : processes) {
					for (NextflowTask task : process.getTasks()) {
						String status = (String) task.getTrace().get("status");

						if (status.equals("RUNNING") || status.equals("SUBMITTED")) {
							task.getTrace().put("status", "KILLED");
						}
					}
				}
				updateProgress();

				if (killed) {
					context.error("Pipeline execution canceled.");
				} else {
					context.error("Pipeline execution failed.");
					Object stdout = step.get("stdout");
					if (stdout != null && stdout.toString().equalsIgnoreCase("true")) {
						context.error(output.toString());
					}
				}
			}

			updateProgress();
			collector.cleanProcesses(context);
			parseOutput(output);

			File outputFile = new File(executionDir, "cloudgene.out");
			if (!outputFile.exists()) {
				return success;
			}

			context.log("Load output file from '" + outputFile.getCanonicalPath() + "'");
			try {
				parseOutput(outputFile);
			} catch (Exception e) {
				log.error("[Job {}] Invalid output file.", context.getJobId(), e);
			}

			return success;
		} catch (Exception e) {
			log.error("[Job {}] Running nextflow script failed.", context.getJobId(), e);
			return false;
		} finally {
			try {
				workspace.uploadLog(logPath.toFile());
			} catch (Exception e) {
				log.error("[Job {}] Uploading Nextflow logs failed.", context.getJobId(), e);
			}
		}
	}

	private void parseOutput(@NonNull File file) throws IOException {
		CommandOutput output = new CommandOutput(file.getAbsolutePath());
		output.execute(context, context.getCurrentStep());
	}

	private void parseOutput(@NonNull StringBuilder content) throws IOException {
		CommandOutput output = new CommandOutput(content);
		output.execute(context, context.getCurrentStep());
	}

	@Override
	public void updateProgress() {
		List<NextflowProcess> processes = collector.getProcesses(context);

		for (NextflowProcess process : processes) {
			NextflowProcessConfig config = getNextflowProcessConfig(process);

			Message message = messages.get(process.getName());
			if (message == null) {
				Step step = config.getStep();
				if (step == null) {
					message = context.createTask("<b>" + process.getName() + "</b>");
				} else {
					message = context.createTask(step, "<b>" + process.getName() + "</b>");
				}
				messages.put(process.getName(), message);
			}

			NextflowProcessRenderer.render(config, process, message);
		}
	}

	private void loadProcessConfigs(@Nullable Object map) {
		if (map != null) {
			List<Map<String, Object>> processConfigs = (List<Map<String, Object>>) map;
			for (Map<String, Object> processConfig : processConfigs) {
				String process = processConfig.get("process").toString();
				NextflowProcessConfig config = new NextflowProcessConfig();
				if (processConfig.get("view") != null) {
					config.setView(processConfig.get("view").toString());
				}
				if (processConfig.get("label") != null) {
					config.setLabel(processConfig.get("label").toString());
				}
				if (processConfig.get("group") != null) {
					config.setGroup(processConfig.get("group").toString());
				}
				configs.put(process, config);
			}
		}
	}

	private Map<String, Step> loadGroups(@Nullable Object map) {
		Map<String, Step> groups = new HashMap<>();
		if (map != null) {
			List<Map<String, Object>> groupConfigs = (List<Map<String, Object>>) map;
			for (Map<String, Object> groupConfig : groupConfigs) {
				String id = groupConfig.get("id").toString();
				String label = groupConfig.get("label").toString();
				Step step = context.createStep(label);
				groups.put(id, step);
			}
		}
		return groups;
	}

	private NextflowProcessConfig getNextflowProcessConfig(@NonNull NextflowProcess process) {
		NextflowProcessConfig config = configs.get(process.getName());
		return config != null ? config : new NextflowProcessConfig();
	}

	@Override
	public String[] getRequirements() {
		return new String[] { NextflowPlugin.ID };
	}

	@NonNull
	private Map<String, Object> createParamsMap(@NonNull WdlStep step) {
		Map<String, Object> params = new HashMap<>();

		// used to defined hard coded params
		if (step.get("params") != null) {
			Map<String, Object> paramsMap = (Map<String, Object>) step.get("params");
			params.putAll(MapValueParser.parseMap(paramsMap));
		}

		// add all inputs
		for (WdlParameterInput param : context.getJob().getApp().getWorkflow().getInputs()) {
			String name = param.getId();
			String value = context.getInput(name);
			if (!param.isSerialize()) {
				continue;
			}
			// resolve app links: use all properties as input parameters
			if (param.getTypeAsEnum() == WdlParameterInputType.APP_LIST) {
				Map<String, Object> linkedApp = (Map<String, Object>) context.getData(name);
				if (linkedApp != null) {
					params.put(name, MapValueParser.parseMap(linkedApp));
				}
			} else {
				params.put(name, MapValueParser.guessType(value));
			}
		}

		// add all outputs
		for (WdlParameterOutput param : context.getJob().getApp().getWorkflow().getOutputs()) {
			String name = param.getId();
			String value = context.getOutput(name);
			if (!param.isSerialize()) {
				continue;
			}
			params.put(name, value);
		}

		return params;
	}

	private boolean writeParamsJson(
			@NonNull String prefix,
			@NonNull WdlStep step,
			@NonNull NextflowBinary nextflow) {

		Path paramsFile = Path.of(context.getLocalOutput(), prefix + "params.json");

		try {
			Map<String, Object> paramMap = createParamsMap(step);
			String paramJson = JsonOutput.prettyPrint(JsonOutput.toJson(paramMap));

			Files.writeString(paramsFile, paramJson);
			context.log("Wrote params to file '" + paramsFile + "'");
		} catch (IOException e) {
			log.error("[Job {}] Writing params.json file failed.", context.getJobId(), e);
			return false;
		}

		nextflow.setParamsFile(paramsFile);
		return true;
	}

	private boolean writeNextflowConfig(
			@NonNull String prefix,
			@NonNull String workDir,
			@NonNull String weblogURL,
			@NonNull String trace,
			@NonNull String report,
			@NonNull String timeline,
			@NonNull NextflowBinary nextflow) {

		String contents = """
				plugins {
					id 'nf-weblog'
				}

				workDir = '%s'

				weblog {
					enabled = true
					url = '%s'
				}

				trace {
					enabled = true
					file = '%s'
					overwrite = true
				}

				report {
					enabled = true
					file = '%s'
					overwrite = true
				}

				timeline {
					enabled = true
					file = '%s'
					overwrite = true
				}
				""".formatted(workDir, weblogURL, trace, report, timeline);

		Path configFile = Path.of(context.getLocalOutput(), prefix + "nextflow.config");

		try {
			Files.writeString(configFile, contents);
		} catch (IOException e) {
			log.error("[Job {}] Writing nextflow.config file failed.", context.getJobId(), e);
			return false;
		}

		nextflow.addConfig(configFile);
		return true;
	}
}
