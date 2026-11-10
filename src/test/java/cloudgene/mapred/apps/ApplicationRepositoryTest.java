package cloudgene.mapred.apps;

import cloudgene.mapred.util.GitHubException;
import cloudgene.mapred.util.GitHubUtil;
import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

public class ApplicationRepositoryTest {
	// TODO: Test reload()
	// TODO: Test getByIdAndUser()
	// TODO: Test getAllByUser()
	// TODO: Test remove()
	// TODO: Test updateConfig()
	// TODO: Test install()
	// TODO: Test installFrom*()

	@BeforeEach
	@AfterEach
	public void clearAppFodler() throws IOException {
		PathUtils.delete(Path.of("apps"));
	}

	@Test
	public void testInstallFromYaml() throws IOException {
		ApplicationRepository repo = new ApplicationRepository();
		repo.installFromYaml(
				Path.of("test-data/return-true.yaml"),
				false);

		List<Application> apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(1, apps.size());
		assertEquals("return-true-step-public@1.0.1", apps.get(0).getId());

		repo.installFromYaml(
				Path.of("test-data/return-false.yaml"),
				false);

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(2, apps.size());
		assertEquals("return-true-step-public@1.0.1", apps.get(0).getId());
		assertEquals("return-false-step-public@1.0.1", apps.get(1).getId());
	}

	@Test
	public void testInstallFromDirectoryMoveFalse() throws IOException {
		List<Application> apps;
		Application app;

		Path dir = Path.of("test-data", "dir-app");
		ApplicationRepository repo = new ApplicationRepository();

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(0, apps.size());

		// Real app gets loaded -> default name cloudgene.yaml
		app = repo.installFromDirectory(dir, false);
		assertEquals("dir-app@9.7.5", app.getId());

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(1, apps.size());
		assertEquals("dir-app@9.7.5", apps.get(0).getId());

		// Trying to re-install the same app again throws.
		assertThrows(
				IOException.class,
				() -> repo.installFromDirectory(dir, false));

		// Dir not found -> throw IOException.
		assertThrows(
				IOException.class,
				() -> repo.installFromDirectory(Path.of("fake", "dir"), false));

		// We can get a different app from the same dir by specifying the YAML file.
		app = repo.installFromDirectory(
				dir,
				false,
				"dust-collector.yaml");
		assertEquals("dust-collector@2.4.6", app.getId());

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(2, apps.size());
		assertEquals("dir-app@9.7.5", apps.get(0).getId()); // Still first.
		assertEquals("dust-collector@2.4.6", apps.get(1).getId()); // New addition.
	}

	private Path makeDirCopy(Path og) throws IOException {
		String prefix = og.getFileName() + "-copy-";
		Path tmp = Files.createTempDirectory(prefix);
		PathUtils.copyDirectory(og, tmp);
		return tmp;
	}

	@Test
	public void testInstallFromDirectoryMoveTrue() throws IOException {
		List<Application> apps;
		Application app;
		Path dir;
		Path dst;

		Path og = Path.of("test-data", "dir-app");
		ApplicationRepository repo = new ApplicationRepository();

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(0, apps.size());

		// Real app gets loaded -> default name cloudgene.yaml
		dir = makeDirCopy(og);
		dst = Path.of("apps", "dir-app");

		// Initially: test data exists, app doesn't.
		assertTrue(Files.isDirectory(dir));
		assertFalse(Files.exists(dst));

		app = repo.installFromDirectory(dir, true);
		assertEquals("dir-app@9.7.5", app.getId());

		// After install: app exists, test data doesn't.
		assertFalse(Files.exists(dir));
		assertTrue(Files.isDirectory(dst));

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(1, apps.size());
		assertEquals("dir-app@9.7.5", apps.get(0).getId());
	}

	@Test
	public void testInstallFromZip() throws IOException {
		List<Application> apps;
		Application app;

		Path zipFile = Path.of("test-data", "zipped-app", "app.zip");
		ApplicationRepository repo = new ApplicationRepository();

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(0, apps.size());

		// Real app gets loaded -> default name cloudgene.yaml
		app = repo.installFromZipFile(zipFile);
		assertEquals("zipped-app@0.1.2", app.getId());

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(1, apps.size());
		assertEquals("zipped-app@0.1.2", apps.get(0).getId());

		// Trying to re-install the same app again throws.
		assertThrows(
				IOException.class,
				() -> repo.installFromZipFile(zipFile));

		// ZIP not found -> throw IOException.
		assertThrows(
				IOException.class,
				() -> repo.installFromZipFile(Path.of("fake", "dir", "does-not-exist.zip")));

		// We can get a different app from the same ZIP archive by specifying the YAML
		// file.
		app = repo.installFromZipFile(zipFile, "shadow.yaml");
		assertEquals("shadow-app@5.4.3", app.getId());

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(2, apps.size());
		assertEquals("zipped-app@0.1.2", apps.get(0).getId()); // Still first.
		assertEquals("shadow-app@5.4.3", apps.get(1).getId()); // New addition.
	}

	@Test
	public void testInstallFromGitHub() throws GitHubException, IOException {
		List<Application> apps;
		ApplicationRepository appRepo = new ApplicationRepository();

		apps = appRepo.getAll();
		assertNotNull(apps);
		assertEquals(0, apps.size());

		// It doesn't matter what we put here, since we're mocking the URL to load
		// zipped-app
		GitHubUtil.Repository githubRepo = GitHubUtil.parseShorthand("user/repo@ref");
		URI zipFile = Path.of("test-data/zipped-app/app.zip").toUri();

		try (MockedStatic<GitHubUtil> github = mockStatic(GitHubUtil.class)) {
			github.when(() -> GitHubUtil.buildUrlFromRepository(githubRepo)).thenReturn(zipFile);

			Application app = appRepo.installFromGitHub(githubRepo);
			assertEquals("zipped-app@0.1.2", app.getId());
		}

		apps = appRepo.getAll();
		assertNotNull(apps);
		assertEquals(1, apps.size());
		assertEquals("zipped-app@0.1.2", apps.get(0).getId());
	}

	@Test
	public void testGetById() throws IOException {
		ApplicationRepository repo = new ApplicationRepository();
		repo.installFromYaml(
				Path.of("test-data/return-true.yaml"),
				false);
		repo.installFromYaml(
				Path.of("test-data/return-false.yaml"),
				false);

		Application app;

		// Using ID without version for an installed app works.
		app = repo.getById("return-true-step-public");
		assertEquals(repo.getAll().get(0), app);
		assertEquals("return-true-step-public@1.0.1", app.getId());

		// Same for other installed app.
		app = repo.getById("return-false-step-public");
		assertEquals(repo.getAll().get(1), app);
		assertEquals("return-false-step-public@1.0.1", app.getId());

		// Adding the correct version suffix also works.
		app = repo.getById("return-true-step-public@1.0.1");
		assertEquals(repo.getAll().get(0), app);
		assertEquals("return-true-step-public@1.0.1", app.getId());

		// Wrong version -> returns null.
		app = repo.getById("return-true-step-public@99.99.99");
		assertNull(app);

		// Non-installed app -> returns null.
		app = repo.getById("no-steps"); // ID for test-data/no-steps.yaml
		assertNull(app);
	}
}
