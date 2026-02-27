package cloudgene.mapred.database;

import cloudgene.mapred.TestApplication;
import cloudgene.mapred.core.Banner;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.util.config.Settings;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(rebuildContext = true)
public class BannerDaoTest {

	private BannerDao dao;

	@BeforeEach
	public void setup() throws Exception {
		Settings settings = TestApplication.loadSettings("BannerDaoTest");
		TestApplication application = new TestApplication(settings);
		Database db = application.getDatabase();
		dao = new BannerDao(db);
	}

	@Test
	public void testInsert() {
		Banner observed;
		Banner expected;

		observed = dao.insert(Banner.Type.WARNING, "Some warning!");
		expected = new Banner(Banner.Type.WARNING, "Some warning!", 1, 1);

		assertEquals(expected, observed);

		observed = dao.insert(Banner.Type.DANGER, "real DANger?");
		expected = new Banner(Banner.Type.DANGER, "real DANger?", 2, 2);

		assertEquals(expected, observed);
	}

	@Test
	public void testFindById() {
		Banner og = dao.insert(Banner.Type.DANGER, "smells like burnt toast");
		Banner expected = new Banner(Banner.Type.DANGER, "smells like burnt toast", 1, 1);

		assertEquals(expected, og);

		Banner found = dao.findById(og.getId());

		assertEquals(og, found);
	}

	@Test
	public void testUpdate() {
		Banner banner;

		// First insert a test element
		banner = dao.insert(Banner.Type.WARNING, "- Update Test -");

		assertNotNull(banner);
		assertEquals(Banner.Type.WARNING, banner.getType());
		assertEquals("- Update Test -", banner.getMessage());
		assertEquals(1, banner.getId());
		assertEquals(1, banner.getPosition());

		// Then update it
		banner.setType(Banner.Type.DANGER);
		banner.setMessage("-- test UPDATED --");

		boolean result = dao.update(banner);
		assertTrue(result);

		banner = dao.findById(banner.getId());

		assertNotNull(banner);
		assertEquals(Banner.Type.DANGER, banner.getType());
		assertEquals("-- test UPDATED --", banner.getMessage());
		assertEquals(1, banner.getId());
		assertEquals(1, banner.getPosition());
	}

	@Test
	public void testFindAll() {
		dao.insert(Banner.Type.WARNING, "eins");
		dao.insert(Banner.Type.DANGER, "ZWEI");

		List<Banner> observed = dao.findAll();

		List<Banner> expected = List.of(
				new Banner(Banner.Type.WARNING, "eins", 1, 1),
				new Banner(Banner.Type.DANGER, "ZWEI", 2, 2));

		assertEquals(expected, observed);
	}

	@Test
	public void testDelete() {
		List<Banner> observed;
		List<Banner> expected;

		Banner first = dao.insert(Banner.Type.DANGER, "1 = u");
		Banner second = dao.insert(Banner.Type.WARNING, "2 = dos");
		Banner third = dao.insert(Banner.Type.WARNING, "3 = tres");

		observed = dao.findAll();
		expected = List.of(
				new Banner(Banner.Type.DANGER, "1 = u", 1, 1),
				new Banner(Banner.Type.WARNING, "2 = dos", 2, 2),
				new Banner(Banner.Type.WARNING, "3 = tres", 3, 3));

		assertEquals(expected, observed);

		dao.delete(second);

		observed = dao.findAll();
		expected = List.of(
				new Banner(Banner.Type.DANGER, "1 = u", 1, 1),
				new Banner(Banner.Type.WARNING, "3 = tres", 2, 3));

		assertEquals(expected, observed);
	}

	@Test
	public void testSwap() {
		List<Banner> observed;
		List<Banner> expected;

		Banner first = dao.insert(Banner.Type.DANGER, "01 ... Un");
		Banner second = dao.insert(Banner.Type.WARNING, "02 ... Deux");
		Banner third = dao.insert(Banner.Type.DANGER, "03 ... Trois");

		observed = dao.findAll();
		expected = List.of(
				new Banner(Banner.Type.DANGER, "01 ... Un", 1, 1),
				new Banner(Banner.Type.WARNING, "02 ... Deux", 2, 2),
				new Banner(Banner.Type.DANGER, "03 ... Trois", 3, 3));

		assertEquals(expected, observed);

		dao.swap(first, second);

		observed = dao.findAll();
		expected = List.of(
				new Banner(Banner.Type.WARNING, "02 ... Deux", 1, 2),
				new Banner(Banner.Type.DANGER, "01 ... Un", 2, 1),
				new Banner(Banner.Type.DANGER, "03 ... Trois", 3, 3));

		assertEquals(expected, observed);
	}
}
