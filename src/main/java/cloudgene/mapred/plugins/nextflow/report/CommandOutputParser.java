package cloudgene.mapred.plugins.nextflow.report;

import java.io.*;
import java.util.*;

public final class CommandOutputParser {

	private CommandOutputParser() {
	}

	/**
	 * Reads all lines in the provided {@code in} stream and parses any commands of
	 * form {@code ::<name> [k1=v1, k2=v2, ...]:: [value]}, returning them as a
	 * list.
	 * <p>
	 * Only lines starting in {@code ::} (no whitespace) are processed.
	 * <p>
	 * {@code ::group::} commands are a special case: they include all subsequent
	 * lines as their value until an {@code ::endgroup::} command is found. Nesting
	 * commands is considered an error. A {@code ::group::} command's name can be
	 * overwritten by passing a {@code type} parameter (defaults to
	 * {@code "message"}).
	 */
	public static List<Command> parseOutput(InputStream in) throws IOException {
		List<Command> commands = new ArrayList<>();

		InputStreamReader isr = new InputStreamReader(in);
		BufferedReader reader = new BufferedReader(isr);

		String line;
		while ((line = reader.readLine()) != null) {
			Command command = parseCommand(line);

			if (command != null) {
				if (command.name().equals("group")) {
					command = parseGroup(command, reader);
				}

				if (command.name().equals("endgroup")) {
					throw new IOException("Found ::endgroup:: outside of a ::group:: block");
				}

				commands.add(command);
			}
		}

		reader.close();
		isr.close();

		return commands;
	}

	/**
	 * Called by {@code parseOutput()} to process {@code ::group::} blocks. Reads
	 * all subsequent lines as one large value until an {@code ::endgroup::} command
	 * is found. Nested commands are not allowed and will raise an
	 * {@link IOException}. Blank lines are ignored. If a {@code type} parameter is
	 * provided, it substitutes the command {@code name} after parsing (defaults to
	 * {@code "message"}).
	 */
	private static Command parseGroup(Command head, BufferedReader reader) throws IOException {
		Map<String, String> parameters = head.parameters();
		String value = parameters.get("value");

		String name = "message";
		if (parameters.containsKey("type")) {
			name = parameters.get("type");
		}

		String line;
		while ((line = reader.readLine()) != null) {
			Command command = parseCommand(line);

			if (command != null) {
				if (command.name().equals("endgroup")) {
					parameters.put("value", value);
					return new Command(name, parameters);
				} else {
					throw new IOException("Found a command while parsing ::group::, before finding ::endgroup::");
				}
			}

			line = line.trim();
			if (!line.isEmpty()) {
				if (!value.isEmpty()) {
					value += "\n";
				}
				value += line;
			}
		}

		throw new IOException("Reached end-of-stream while reading ::group:: contents (no ::endgroup:: found)");
	}

	/**
	 * Called by {@code parseOutput()} and {@code parseGroup()} to parse a single
	 * line. Returns a parsed {@link Command} if the line is a valid command of form
	 * {@code ::<name> [k1=v1, k2=v2, ...]:: [value]}. Returns {@code null}
	 * otherwise.
	 */
	public static Command parseCommand(String line) {
		if (!line.startsWith("::")) {
			return null; // Not a command line.
		}

		int closeIdx = line.indexOf("::", 2);
		if (closeIdx == -1) {
			return null; // No closing :: found.
		}

		String nameAndParams = line.substring(2, closeIdx).trim();
		String value = line.substring(closeIdx + 2).trim();

		int spaceIdx = nameAndParams.indexOf(" ");
		String name;
		String rawParams;

		if (spaceIdx == -1) {
			name = nameAndParams;
			rawParams = "";
		} else {
			name = nameAndParams.substring(0, spaceIdx).trim();
			rawParams = nameAndParams.substring(spaceIdx).trim();
		}

		name = name.toLowerCase();

		Map<String, String> parameters = parseParameters(rawParams);
		parameters.put("value", value);

		return new Command(name, parameters);
	}

	/**
	 * Called by parseCommand(). Splits {@code parameters} into key-value pairs.
	 * Expects shape {@code k1=v1, k2=v2, ...}. Whitespace is ignored.
	 */
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
}
