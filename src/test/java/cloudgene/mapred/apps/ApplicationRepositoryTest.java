package cloudgene.mapred.apps;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationRepositoryTest {
	// TODO: Test reload()
	// TODO: Test getByIdAndUser()
	// TODO: Test getAllByUser()
	// TODO: Test remove()
	// TODO: Test updateConfig()
	// TODO: Test install()
	// TODO: Test installFrom*()

	@Test
	public void testInstallFromYaml() throws IOException {
		ApplicationRepository repo = new ApplicationRepository();
		repo.installFromYaml("test-data/return-true.yaml", false);

		List<Application> apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(1, apps.size());
		assertEquals("return-true-step-public@1.0.1", apps.get(0).getId());

		repo.installFromYaml("test-data/return-false.yaml", false);

		apps = repo.getAll();
		assertNotNull(apps);
		assertEquals(2, apps.size());
		assertEquals("return-true-step-public@1.0.1", apps.get(0).getId());
		assertEquals("return-false-step-public@1.0.1", apps.get(1).getId());
	}

	@Test
	public void testGetById() throws IOException {
		ApplicationRepository repo = new ApplicationRepository();
		repo.installFromYaml("test-data/return-true.yaml", false);
		repo.installFromYaml("test-data/return-false.yaml", false);

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
