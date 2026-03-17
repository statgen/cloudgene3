package cloudgene.mapred.jobs.workspace;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class S3WorkspaceTest {
	private static Stream<Arguments> provideForGetParent() {
		return Stream.of(
				// Error cases
				arguments("http://example.com", null), // Not an S3 URI.
				arguments("s3://", null), // Only protocol present.
				arguments("s3://foo/bar", null), // We don't allow empty keys even as the parent.
				arguments("s3:///", null), // Invalid URI

				// Success cases
				arguments("s3://foo/bar/baz", "s3://foo/bar"), // Simple example.
				arguments("s3://a/b/c/d/e/f", "s3://a/b/c/d/e"), // Deeply nested.
				arguments("s3://a/b/c", "s3://a/b") // Shortest example that makes sense.
		);
	}

	@ParameterizedTest
	@MethodSource("provideForGetParent")
	public void testGetParent(String uri, String parent) {
		S3Workspace workspace = new S3Workspace("s3://my-bucket/some-prefix");
		String observed = workspace.getParent(uri);
		assertEquals(parent, observed);
	}
}
