package cloudgene.mapred.util.config;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;

@MicronautTest
public class SettingsTest {

	@Test
	public void testLoadFileBasic() throws Exception {
		Settings settings = withEnvironmentVariable("CG_CONFIG_DIRECTORY", "test-data/cloudgene-config-basic")
				.execute(Settings::load);
		assertEquals("Test Name 123", settings.getName());
	}

	@Test
	public void testLoadFileNoUrl() {
		assertThrows(IOException.class,
				() -> withEnvironmentVariable("CG_CONFIG_DIRECTORY", "test-data/cloudgene-config-no-url")
						.execute(Settings::load));
	}

	@Test
	public void testGetTempFilename() throws Exception {
		Settings settings = Settings.load();
		String filename = settings.getTempFilename("foo.txt");
		assertEquals("tmp" + File.separator + "foo.txt", filename);
	}

	@Test
	public void testGetExternalWorkspace() throws Exception {
		Settings settings = Settings.load();

		// External workspace initially unset
		assertNull(settings.getExternalWorkspace());

		String defaultLocation = settings.getExternalWorkspaceLocation();

		// Defaults to local workspace, which defaults to "workspace"
		assertEquals("workspace", defaultLocation);

		Map<String, String> externalWorkspace = settings.getExternalWorkspace();

		// Calling the method has initialized the external workspace to { type="local",
		// location="workspace" }
		assertNotNull(externalWorkspace);
		assertEquals("local", externalWorkspace.get("type"));
		assertEquals("workspace", externalWorkspace.get("location"));
	}

	private static Stream<Arguments> provideForSetServerUrl() {
		return Stream.of(
				// Parseable cases: domain + optional port.
				arguments("example.com", true),
				arguments("foo.bar", true),
				arguments("foo.bar.baz.edu", true),
				arguments("bar.it:123", true),
				arguments("hello.world:80", true),
				arguments("localhost", true),
				arguments("localhost:9753", true),
				arguments("127.0.0.1:8080", true),
				arguments("[::1]:9090", true),

				// Null or blank illegal.
				arguments(null, false),
				arguments("", false),
				arguments(" \t\n ", false),

				// Error cases.
				arguments(".com", false), // Leading dot
				arguments("foo.", false), // Trailing dot
				arguments("foo:", false), // Colon with no port
				arguments(":80", false), // Colon with no domain
				arguments("foo.bar.", false), // Trailing dot
				arguments("https://foo.com", false), // Protocol
				arguments("bar.it:123/foo", false), // Path
				arguments("localhost/baz", false), // Path
				arguments("user@example.com", false), // User info
				arguments("example.com?foo", false), // Query
				arguments("example.com#foo", false), // Location
				arguments("example.com:", false), // Colon with no port
				arguments("example.com:abc", false), // Port must be numeric
				arguments("example.com:99999", false) // Port must be <= 65,535
		);
	}

	@ParameterizedTest
	@MethodSource("provideForSetServerUrl")
	public void testSetServerUrl(String baseUrl, boolean success) {
		Settings settings = new Settings();
		assertEquals("localhost:8082", settings.getServerUrl());

		try {
			settings.setServerUrl(baseUrl);
			String observed = settings.getServerUrl();

			assertTrue(success);
			assertEquals(baseUrl, observed);
		} catch (IllegalArgumentException e) {
			assertFalse(success);
		}
	}

	private static Stream<Arguments> provideForSetBaseUrl() {
		return Stream.of(
				// Null or blank normalized to "/".
				arguments(null, "", true),
				arguments("", "", true),
				arguments(" \t\n ", "", true),

				// Parseable cases: path.
				arguments("/", "", true),
				arguments("//////", "", true),
				arguments("foo", "/foo", true),
				arguments("/foo//", "/foo", true),
				arguments("/bar/baz", "/bar/baz", true),
				arguments("foo/bar/baz", "/foo/bar/baz", true),
				arguments("/foo-bar/x_y", "/foo-bar/x_y", true),
				arguments("/foo%20bar////", "/foo%20bar", true),

				// Error cases.
				arguments("//example.com", null, false), // domain
				arguments("https://example.com", null, false), // protocol + domain
				arguments("/foo?bar", null, false), // has query
				arguments("/foo#section", null, false), // has location
				arguments("/foo bar", null, false) // has spaces
		);
	}

	@ParameterizedTest
	@MethodSource("provideForSetBaseUrl")
	public void testSetBaseUrl(String baseUrl, String expected, boolean success) {
		Settings settings = new Settings();
		assertEquals("", settings.getBaseUrl());

		try {
			settings.setBaseUrl(baseUrl);
			String observed = settings.getBaseUrl();

			assertTrue(success);
			assertEquals(expected, observed);
		} catch (IllegalArgumentException e) {
			assertFalse(success);
		}
	}
}
