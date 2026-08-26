package cloudgene.mapred.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.CloudgeneStep;
import cloudgene.mapred.jobs.Message;
import cloudgene.mapred.wdl.WdlStep;
import io.micronaut.core.annotation.NonNull;

public class BashCommandStep extends CloudgeneStep {

	@Override
	public boolean run(@NonNull WdlStep step, @NonNull CloudgeneContext context) {
		context.createStep(step.getName());

		String originalCommand = step.getString("exec");
		if (originalCommand == null) {
			originalCommand = step.getString("cmd");
		}

		if (originalCommand == null) {
			context.error("No 'exec' or 'cmd' parameter found.");
			return false;
		}

		originalCommand = originalCommand.strip();

		if (originalCommand.isEmpty()) {
			context.error("'exec' or 'cmd' parameter cannot be an empty string.");
			return false;
		}

		String bash = step.getString("bash", "false");
		String stdout = step.getString("stdout", "false");

		boolean useBash = bash.equals("true");
		boolean streamStdout = stdout.equals("true");

		List<String> params = Arrays.asList(originalCommand.split(" "));
		List<String> executedCommand = new ArrayList<>();

		if (useBash) {
			executedCommand.add("/bin/bash");
			executedCommand.add("-c");
			executedCommand.add(originalCommand);
		} else {
			executedCommand.addAll(params);
		}

		StringBuilder output = null;
		if (streamStdout) {
			output = new StringBuilder();
		}

		try {
			context.beginTask("Running Command...");
			int ret = executeCommand(executedCommand, context, output);
			if (ret == 0) {
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
					context.endTask("Execution failed: the program returned error code " + ret, Message.ERROR);
				}
				return false;
			}
		} catch (Exception e) {
			context.endTask("Execution failed: could not run the program.", Message.ERROR);
			context.log("Execution failed.", e);
			return false;
		}
	}
}
