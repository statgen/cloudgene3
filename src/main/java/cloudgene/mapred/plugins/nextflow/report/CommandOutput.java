package cloudgene.mapred.plugins.nextflow.report;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.Step;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CommandOutput {

	public static final String DEFAULT_FILENAME = ".command.out";

	private List<Command> commands = new ArrayList<>();

	public CommandOutput(StringBuilder output) throws IOException {
		String str = output.toString();
		byte[] bytes = str.getBytes();
		loadFromInputStream(new ByteArrayInputStream(bytes));
	}

	public CommandOutput(String filename) throws IOException {
		loadFromFile(filename);
	}

	public CommandOutput(InputStream in) throws IOException {
		loadFromInputStream(in);
	}

	public void loadFromFile(String filename) throws IOException {
		loadFromInputStream(new FileInputStream(filename));
	}

	public void loadFromInputStream(InputStream in) throws IOException {
		commands = CommandOutputParser.parseOutput(in);
	}

	public void execute(CloudgeneContext context, Step step) throws IOException {

		if (step == null) {
			step = context.getCurrentStep();
		}

		for (Command command : commands) {
			switch (command.name()) {
				case "error":
					context.message(step, command.parameters().get("value"), CloudgeneContext.ERROR);
					break;
				case "warning":
					context.message(step, command.parameters().get("value"), CloudgeneContext.WARNING);
					break;
				case "message":
				case "notice":
					context.message(step, command.parameters().get("value"), CloudgeneContext.OK);
					break;
				case "log":
					context.log(command.parameters().get("value"));
					break;
				case "debug":
					context.println(command.parameters().get("value"));
					break;
				case "set-counter":
				case "inc-counter":
					context.incCounter(
							command.parameters().get("name"),
							Integer.parseInt(command.parameters().get("value")));
					break;
				case "submit-counter":
					context.submitCounter(command.parameters().get("name"));
					break;
				case "set-value":
					context.setValue(
							command.parameters().get("name"),
							command.parameters().get("value"));
					break;
				case "submit-value":
					context.submitValue(command.parameters().get("name"));
					break;
				case "set-value-and-submit":
					context.setValue(
							command.parameters().get("name"),
							command.parameters().get("value"));
					context.submitValue(command.parameters().get("name"));
					break;
				default:
					throw new IOException("Unknown command: " + command.name());
			}
		}
	}
}
