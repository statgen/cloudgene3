package cloudgene.mapred.plugins.nextflow.report;

import java.io.*;
import java.util.*;

// TODO(Marc): RENAME! This has nothing to do with GitHub Actions as far as I can tell.
//             Instead, it's used to parse logs from Nextflow.
public final class GitHubActionsParser {

    private GitHubActionsParser() {}

	public static List<Command> parseOutput(InputStream in) throws IOException {
		List<Command> commands = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			Command group = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("::")) {
					Command command = parseCommand(line);
					if (command != null) {
						if (command.getName().equals("group")) {
							group = command;
							commands.add(command);
						} else if (command.getName().equals("endgroup")) {
							if (group == null) {
								throw new IOException("Found ::endgroup:: without ::group::");
							}
							String type = group.getParameters().get("type");
							if (type == null) {
								type = "message";
							}
							group.setName(type.trim());
							group = null;
						} else {
							if (group != null) {
								throw new IOException("No ::endgroup:: found.");
							}
							commands.add(command);
						}
					}
				} else {
					if (group != null) {
						String value = group.getParameters().get("value");
						if (!value.trim().isEmpty()) {
							value += "\n";
						}
						value += line;
						group.getParameters().put("value", value.trim());
					}
				}
			}
		}
		return commands;
	}

	public static Command parseCommand(String line) {
		int firstSpace = line.indexOf("::");
		int secondSpace = line.indexOf("::", firstSpace + 1);

		if (firstSpace != 0 || secondSpace == -1) {
			System.out.println("Invalid syntax: " + line);
			return null; // Invalid command format
		}

		String[] commandNameAndValue = line.substring(2).trim().split("::", 2);
		String[] commandNameAndParams = commandNameAndValue[0].split(" ", 2);

		String commandName = commandNameAndParams[0].trim();
		String parameters = "";
		if (commandNameAndParams.length > 1) {
			parameters = commandNameAndParams[1].trim();
		}
		String commandValue = "";
		if (commandNameAndValue.length > 1) {
			commandValue = commandNameAndValue[1].trim();
		}

		Map<String, String> parameterMap = parseParameters(parameters);
		parameterMap.put("value", commandValue);

		return new Command(commandName.toLowerCase(), parameterMap);
	}

	private static Map<String, String> parseParameters(String parameters) {
		Map<String, String> paramMap = new HashMap<>();
		String[] pairs = parameters.split(",");
		for (String pair : pairs) {
			String[] keyValue = pair.split("=");
			if (keyValue.length == 2) {
				paramMap.put(keyValue[0].trim(), keyValue[1].trim());
			}
		}
		return paramMap;
	}

	public static class Command {
		private String name;
		private final Map<String, String> parameters;

		public Command(String name, Map<String, String> parameters) {
			this.name = name;
			this.parameters = parameters;
		}

		public String getName() {
			return name;
		}

		public Map<String, String> getParameters() {
			return parameters;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (other == null)
				return false;

			if (!(other instanceof Command))
				return false;
			Command command = (Command) other;

			return Objects.equals(name, command.name)
					&& Objects.equals(parameters, command.parameters);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, parameters);
		}
	}

	// TODO(Marc): Command should be a record, but requires refactoring parseCommand()
	// record Command(String name, Map<String, String> parameters) {}
}
