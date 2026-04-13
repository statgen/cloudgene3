package cloudgene.mapred.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShowPluginsTest {

	@Test
	public void testShowPlugins() {
		String[] args = {};
		ShowPlugins cmd = new ShowPlugins(args);
		int result = cmd.start();
		assertEquals(0, result);
	}
}
