package cloudgene.mapred.jobs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import cloudgene.mapred.wdl.WdlStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CloudgeneStep {

	private static final Logger log = LoggerFactory.getLogger(CloudgeneStep.class);

	private CloudgeneJob job;

	protected Process process;

	protected CloudgeneContext context;

	protected boolean killed = false;

	public CloudgeneStep() {

	}

	public String getFolder(Class<?> clazz) {
		return new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
	}

	public CloudgeneJob getJob() {
		return job;
	}

	public void setJob(CloudgeneJob job) {
		this.job = job;
	}

	public void setup(CloudgeneContext context) {
		this.context = context;
	}

	public boolean run(WdlStep step, CloudgeneContext context) {
		return true;
	}

	public void updateProgress() {

	}

	public String[] getRequirements() {
		return new String[] {};
	}

	// dummy for beam serialization (setup --> property up!)
	public CloudgeneContext getup() {
		return null;
	}

	protected int executeCommand(List<String> command, CloudgeneContext context)
			throws IOException, InterruptedException {
		return executeCommand(command, context, null);
	}

	protected int executeCommand(List<String> command, CloudgeneContext context, StringBuilder output)
			throws IOException, InterruptedException {
		File workDir = new File(context.getWorkingDirectory());
		return executeCommand(command, context, output, workDir);
	}

	protected int executeCommand(List<String> command, CloudgeneContext context, StringBuilder output, File workDir)
			throws IOException, InterruptedException {

		Environment environment = context.getSettings().buildEnvironment().addContext(context)
				.addApplication(job.getApp());

		// set global variables
		for (int j = 0; j < command.size(); j++) {
			String cmd = environment.resolve(command.get(j));
			command.set(j, cmd);
		}

		context.log("Command: " + command);
		context.log("Working Directory: " + workDir.getAbsolutePath());

		ProcessBuilder builder = new ProcessBuilder(command);
		builder.environment().putAll(environment.toMap());
		builder.directory(workDir);
		builder.redirectErrorStream(true);

		process = builder.start();

		try (InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr)) {
			String line;
			while ((line = br.readLine()) != null) {
				context.println(line);
				if (output != null) {
					output.append(line).append("\n");
				}
			}
		}

		process.waitFor();
		context.log("Exit Code: " + process.exitValue());

		return process.exitValue();
	}

	public void kill() {
		context.log("Process killed by used.");
		if (process != null && process.isAlive()) {
			killed = true;
			process.destroy();
			while (process.isAlive()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					log.error("Process killing failed.", e);
				}
			}
		}
	}
}
