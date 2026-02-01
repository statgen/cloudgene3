package cloudgene.mapred.steps;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.CloudgeneStep;
import cloudgene.mapred.jobs.Message;
import cloudgene.mapred.wdl.WdlStep;
import genepi.io.FileUtil;

public class JavaExternalStep extends CloudgeneStep {

	protected static final Logger log = LoggerFactory.getLogger(JavaExternalStep.class);

	public boolean run(WdlStep step, CloudgeneContext context) {
		context.createStep(step.getName());
		String javaPath = System.getProperty("java.home");

		File path = new File(javaPath);

		if (!path.exists()) {
			context.error("Java Binary was not found. Not found: " + javaPath);
			return false;
		}

		String javaBin;

		if (path.isDirectory()) {
			javaBin = FileUtil.path(javaPath, "bin", "java");
		} else {
			javaBin = javaPath;
		}

		File file = new File(javaBin);

		if (!file.exists()) {
			context.error("Java Binary was not found." + javaBin);
			return false;
		}

		if (!file.canExecute()) {
			context.error(
					"Java Binary was found (" + javaBin + ") but can not be executed. Please check the permissions.");
			return false;
		}

		String stdout = step.getString("stdout", "false");
		boolean streamStdout = stdout.equals("true");

		StringBuilder output = null;
		if (streamStdout) {
			output = new StringBuilder();
		}

		String jar = step.getJar();
		// params
		String paramsString = step.getString("params");
		String[] params = new String[] {};
		if (paramsString != null) {
			params = paramsString.split(" ");
		}

		List<String> command = new ArrayList<>();
		command.add(javaBin);
		command.add("-jar");
		command.add(jar);
		for (String param : params) {
			command.add(param.trim());
		}
		try {
			context.beginTask("Running Java Application...");
			boolean successful = executeCommand(command, context, output);
			if (successful) {
				if (streamStdout) {
					context.endTask(output.toString(), Message.OK);
				} else {
					context.endTask("Execution successful.", Message.OK);
				}
				return true;
			} else {
				if (streamStdout) {
					context.endTask(output.toString(), Message.ERROR);
				} else {
					context.endTask(
							"Execution failed. Please contact the server administrators for help if you believe this job should have completed successfully.",
							Message.ERROR);
				}
				return false;
			}

		} catch (Exception e) {
			context.log("Execution failed.", e);
			return false;
		}
	}

	public int getMapProgress() {
		return -1;
	}

	public int getReduceProgress() {
		return -1;
	}
}
