package cloudgene.mapred.plugins.nextflow.report;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CommandOutputParserTest {

	private static Stream<Arguments> provideForParseOutput() {
		return Stream.of(
				// Big example with all the correct use cases.
				arguments(
						"test-data/parse-logs/example.log",
						List.of(
								new Command(
										"foo",
										Map.of(
												"bar", "baz",
												"value", "Hello, world!")),
								new Command(
										"one",
										Map.of("value", "")),
								new Command(
										"two",
										Map.of("value", "three")),
								new Command(
										"four",
										Map.of(
												"five", "six",
												"value", "")),
								new Command(
										"a",
										Map.of(
												"b", "c",
												"d", "e",
												"f", "g",
												"value", "h")),
								new Command(
										"",
										Map.of("value", "")),
								new Command(
										"",
										Map.of("value", "")),
								new Command(
										"message",
										Map.of("value", "This is a group named 'message' (default value).\n" +
												"Everything between ::group:: and ::endgroup:: is parsed as the 'value'.")),
								new Command(
										"foo",
										Map.of(
												"type", "foo",
												"value",
												"This is a group named 'foo' (specified by the 'type' parameter).\n" +
														"Lines with content are kept separate, but empty lines are collapsed into a single newline."))),
						null),

				// Throws IOException on file not found.
				arguments(
						"fake-path/404",
						null,
						"fake-path/404 (No such file or directory)"),

				// Throws IOException on ::endgroup:: that doesn't match an open ::group::
				arguments(
						"test-data/parse-logs/endgroup-without-group.log",
						null,
						"Found ::endgroup:: outside of a ::group:: block"),

				// Throws IOException on unclosed ::group:: block.
				arguments(
						"test-data/parse-logs/group-without-endgroup.log",
						null,
						"Reached end-of-stream while reading ::group:: contents (no ::endgroup:: found)"),

				// Throws IOException on command inside ::group:: block.
				arguments(
						"test-data/parse-logs/command-inside-group.log",
						null,
						"Found a command while parsing ::group::, before finding ::endgroup::"));
	}

	@ParameterizedTest
	@MethodSource("provideForParseOutput")
	public void testParseOutput(
			String path,
			List<Command> expectedCommands,
			String errorMessage) {

		try (InputStream inputStream = new FileInputStream(path)) {
			List<Command> observedCommands = CommandOutputParser.parseOutput(inputStream);

			assertNull(errorMessage);
			assertEquals(expectedCommands, observedCommands);
		} catch (IOException e) {
			assertNotNull(errorMessage);
			assertTrue(e.getMessage().startsWith(errorMessage));
		}
	}

	private static Stream<Arguments> provideForParseCommand() {
		return Stream.of(
				// Given a correctly formatted command string, output the parsed command
				arguments(
						"::message::this is a message",
						new Command(
								"message",
								Map.of("value", "this is a message"))),

				// With one parameter, no value.
				arguments(
						"::foo bar=baz::",
						new Command(
								"foo",
								Map.of(
										"bar", "baz",
										"value", ""))),

				// With two (comma-separated) parameters and value,
				// with ignored whitespace all around (except beginning).
				arguments(
						":: qw er=ty , ui=op :: asdfg ",
						new Command(
								"qw",
								Map.of(
										"er", "ty",
										"ui", "op",
										"value", "asdfg"))),

				// Shouldn't have whitespace in the beginning.
				arguments(
						"     ::a::b",
						null),

				// On empty string, return null
				arguments("", null),

				// On unclosed command tag, return null
				arguments("::", null));
	}

	@ParameterizedTest
	@MethodSource("provideForParseCommand")
	public void testParseCommand(String line, Command expectedCommand) {
		Command observedCommand = CommandOutputParser.parseCommand(line);
		assertEquals(expectedCommand, observedCommand);
	}
}
