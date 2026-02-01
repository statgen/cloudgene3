package cloudgene.mapred.util;

import org.junit.jupiter.api.Test;

import cloudgene.mapred.util.GitHubUtil.Repository;

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
		assertEquals("genepi", repo.getUser());
		assertEquals("cloudgene-examples", repo.getRepo());
		assertNull(repo.getYaml());
		assertNull(repo.getTag());

		// Correct shorthand, not tag, YAML set to 'fastqc'
		repo = GitHubUtil.parseShorthand("genepi/cloudgene-examples/fastqc");
		assertNotNull(repo);
		assertEquals("genepi", repo.getUser());
		assertEquals("cloudgene-examples", repo.getRepo());
		assertEquals("fastqc", repo.getYaml());
		assertNull(repo.getTag());

		// Correct shorthand, tag set to 1.1.0, no YAML
		repo = GitHubUtil.parseShorthand("genepi/cloudgene-examples@1.1.0");
		assertNotNull(repo);
		assertEquals("genepi", repo.getUser());
		assertEquals("cloudgene-examples", repo.getRepo());
		assertNull(repo.getYaml());
		assertEquals("1.1.0", repo.getTag());

		// Correct shorthand, tag set to 1.1.0, YAML set to fastqc.yaml
		repo = GitHubUtil.parseShorthand("genepi/cloudgene-examples/fastqc.yaml@1.1.0");
		assertNotNull(repo);
		assertEquals("genepi", repo.getUser());
		assertEquals("cloudgene-examples", repo.getRepo());
		assertEquals("fastqc.yaml", repo.getYaml());
		assertEquals("1.1.0", repo.getTag());

		// Correct shorthand, tag set to 1.1.0, YAML set to ngs/fastqc.yaml
		repo = GitHubUtil.parseShorthand("genepi/cloudgene-examples/ngs/fastqc.yaml@1.1.0");
		assertNotNull(repo);
		assertEquals("genepi", repo.getUser());
		assertEquals("cloudgene-examples", repo.getRepo());
		assertEquals("ngs/fastqc.yaml", repo.getYaml());
		assertEquals("1.1.0", repo.getTag());
	}

	@Test
	public void testBuildUrlFromRepository() throws GitHubException {
		Repository repository = new Repository();
		repository.setUser("genepi");
		repository.setRepo("cloudgene-examples");
		assertEquals(
				"https://api.github.com/repos/genepi/cloudgene-examples/zipball",
				GitHubUtil.buildUrlFromRepository(repository));

		repository = new Repository();
		repository.setUser("genepi");
		repository.setRepo("imputationserver");
		repository.setTag("1.0.2");
		assertEquals(
				"https://api.github.com/repos/genepi/imputationserver/zipball/1.0.2",
				GitHubUtil.buildUrlFromRepository(repository));

		repository = new Repository();
		repository.setUser("genepi");
		repository.setRepo("imputationserver");
		repository.setYaml("subdir/subdir2");
		repository.setTag("1.0.2");
		assertEquals(
				"https://api.github.com/repos/genepi/imputationserver/zipball/1.0.2",
				GitHubUtil.buildUrlFromRepository(repository));
	}

	@Test
	public void testGetLatestReleaseFromRepository() throws GitHubException {
		// Error: repository does not exist
		{
			Repository repository = new Repository();
			repository.setUser("lukfor");
			repository.setRepo("false-repo-1234");
			repository.setTag("latest");

			assertThrows(
					GitHubException.class,
					() -> GitHubUtil.getLatestReleaseFromRepository(repository));
			assertThrows(
					GitHubException.class,
					() -> GitHubUtil.buildUrlFromRepository(repository));
		}

		// Correctly resolves latest (assuming no new releases of hello-cloudgene)
		{
			Repository repository = new Repository();
			repository.setUser("lukfor");
			repository.setRepo("hello-cloudgene");
			repository.setTag("latest");

			String latest = GitHubUtil.getLatestReleaseFromRepository(repository);
			assertEquals("1.2.0", latest);

			String repoUrl = GitHubUtil.buildUrlFromRepository(repository);
			assertEquals(
					"https://api.github.com/repos/lukfor/hello-cloudgene/zipball/1.2.0",
					repoUrl);
		}
	}
}
