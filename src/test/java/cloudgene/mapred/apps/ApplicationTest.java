package cloudgene.mapred.apps;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ApplicationTest {

	private static Stream<Arguments> provideForGetPermissions() {
		return Stream.of(
				arguments("foo.txt", "", List.of()),
				arguments("foo.txt", "bar", List.of("bar")),
				arguments("foo.txt", "bar,baz", List.of("bar", "baz")));
	}

	@ParameterizedTest
	@MethodSource("provideForGetPermissions")
	public void testGetPermissions(String filename, String permissions, List<String> expected) {
		Application app = new Application(filename, permissions);
		assertEquals(permissions, app.getPermission()); // Singular form (original) -> get the weird string.
		assertEquals(expected, app.getPermissions()); // Plural form (new) -> separate into actual items.
	}

	private static Stream<Arguments> provideForLoadWdlApp() {
		return Stream.of(
				arguments("fake-path.yaml", "No such file or directory", null),
				arguments("test-data/foo.txt", "Expected data for a cloudgene.mapred.wdl.WdlApp field", null),
				arguments("test-data/no-workflow.yaml", null, "no-workflow@2.5.4"),
				arguments("test-data/return-true.yaml", null, "return-true-step-public@1.0.1"));
	}

	@ParameterizedTest
	@MethodSource("provideForLoadWdlApp")
	public void testLoadWdlApp(String filename, String error, String id) {
		Application app = new Application(filename, "user");
		try {
			app.loadWdlApp();
			assertNull(error);

			assertFalse(app.hasSyntaxError());
			assertNotNull(app.getWdlApp());
			assertNotNull(app.getErrorMessage());
			assertEquals("", app.getErrorMessage());

			assertEquals(id, app.getId());
		} catch (IOException e) {
			assertNotNull(error);
			assertTrue(e.getMessage().contains(error));

			assertTrue(app.hasSyntaxError());
			assertNull(app.getWdlApp());
			assertNotNull(app.getErrorMessage());
			assertEquals(app.getErrorMessage(), e.getMessage());
		}
	}

	private static Stream<Arguments> provideForGetType() {
		return Stream.of(
				arguments("test-data/return-true.yaml", "Application"), // Applies to all executable workflows.
				arguments("test-data/no-workflow.yaml", "cloudgene") // Non-Application with explicit category.
		);
	}

	@ParameterizedTest
	@MethodSource("provideForGetType")
	public void testGetType(String filename, String expected) throws IOException {
		Application app = new Application(filename, "user");
		assertEquals("-", app.getType());
		app.loadWdlApp();
		assertEquals(expected, app.getType());
	}
}
