package cloudgene.mapred.util;

import org.junit.jupiter.api.Test;

import cloudgene.mapred.util.GitHubUtil.Repository;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class GitHubUtilTest {

	@Test
	public void testParseShorthand() {
		// Expected shorthand form: username/repo[/path][@ref]

		Repository repo;

		// Error: missing repo (no ref)
		assertThrows(
				IllegalArgumentException.class,
				() -> GitHubUtil.parseShorthand("genepi"));

		// Error: missing repo (with ref)
		assertThrows(
				IllegalArgumentException.class,
				() -> GitHubUtil.parseShorthand("genepi@1.1.0"));

		// Error: too many @ signs
		assertThrows(
				IllegalArgumentException.class,
				() -> GitHubUtil.parseShorthand("genepi/cloudgene-examples@1.1.0@foo"));

		// Correct shorthand, no tag or YAML
		repo = GitHubUtil.parseShorthand("genepi/cloudgene-examples");
		assertNotNull(repo);
		assertEquals("genepi", repo.user());
		assertEquals("cloudgene-examples", repo.repo());
		assertNull(repo.yaml());
		assertNull(repo.tag());

		// Correct shorthand, not tag, YAML set to 'fastqc'
		repo = GitHubUtil.parseShorthand("genepi/cloudgene-examples/fastqc");
		assertNotNull(repo);
		assertEquals("genepi", repo.user());
		assertEquals("cloudgene-examples", repo.repo());
		assertEquals("fastqc", repo.yaml());
		assertNull(repo.tag());

		// Correct shorthand, tag set to 1.1.0, no YAML
		repo = GitHubUtil.parseShorthand("genepi/cloudgene-examples@1.1.0");
		assertNotNull(repo);
		assertEquals("genepi", repo.user());
		assertEquals("cloudgene-examples", repo.repo());
		assertNull(repo.yaml());
		assertEquals("1.1.0", repo.tag());

		// Correct shorthand, tag set to 1.1.0, YAML set to fastqc.yaml
		repo = GitHubUtil.parseShorthand("genepi/cloudgene-examples/fastqc.yaml@1.1.0");
		assertNotNull(repo);
		assertEquals("genepi", repo.user());
		assertEquals("cloudgene-examples", repo.repo());
		assertEquals("fastqc.yaml", repo.yaml());
		assertEquals("1.1.0", repo.tag());

		// Correct shorthand, tag set to 1.1.0, YAML set to ngs/fastqc.yaml
		repo = GitHubUtil.parseShorthand("genepi/cloudgene-examples/ngs/fastqc.yaml@1.1.0");
		assertNotNull(repo);
		assertEquals("genepi", repo.user());
		assertEquals("cloudgene-examples", repo.repo());
		assertEquals("ngs/fastqc.yaml", repo.yaml());
		assertEquals("1.1.0", repo.tag());
	}

	@Test
	public void testBuildUrlFromRepository() throws GitHubException {
		Repository repository = new Repository("genepi", "cloudgene-examples", null, null);
		assertEquals(
				URI.create("https://api.github.com/repos/genepi/cloudgene-examples/zipball"),
				GitHubUtil.buildUrlFromRepository(repository));

		repository = new Repository("genepi", "imputationserver", "1.0.2", null);
		assertEquals(
				URI.create("https://api.github.com/repos/genepi/imputationserver/zipball/1.0.2"),
				GitHubUtil.buildUrlFromRepository(repository));

		repository = new Repository("genepi", "imputationserver", "1.0.2", "subdir/subdir2");
		assertEquals(
				URI.create("https://api.github.com/repos/genepi/imputationserver/zipball/1.0.2"),
				GitHubUtil.buildUrlFromRepository(repository));
	}

	@Test
	public void testGetLatestReleaseFromRepository() throws GitHubException {
		// Error: repository does not exist
		{
			Repository repository = new Repository("lukfor", "false-repo-1234", "latest", null);

			assertThrows(
					GitHubException.class,
					() -> GitHubUtil.getLatestReleaseFromRepository(repository));
			assertThrows(
					GitHubException.class,
					() -> GitHubUtil.buildUrlFromRepository(repository));
		}

		// Correctly resolves latest (assuming no new releases of hello-cloudgene)
		{
			Repository repository = new Repository("lukfor", "hello-cloudgene", "latest", null);

			String latest = GitHubUtil.getLatestReleaseFromRepository(repository);
			assertEquals("1.2.0", latest);

			URI repoUrl = GitHubUtil.buildUrlFromRepository(repository);
			assertEquals(
					URI.create("https://api.github.com/repos/lukfor/hello-cloudgene/zipball/1.2.0"),
					repoUrl);
		}
	}
}
