package cloudgene.mapred.wdl;

import java.io.*;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;

public class WdlReader {

	public static WdlApp loadAppFromReader(String filename, Reader reader) throws IOException {
		YamlReader yamlReader = new YamlReader(reader);
		YamlConfig config = yamlReader.getConfig();

		config.setPropertyDefaultType(WdlApp.class, "workflow", WdlWorkflow.class);
		config.setPropertyElementType(WdlWorkflow.class, "steps", WdlStep.class);
		config.setPropertyElementType(WdlWorkflow.class, "setups", WdlStep.class);
		config.setPropertyElementType(WdlWorkflow.class, "inputs", WdlParameterInput.class);
		config.setPropertyElementType(WdlWorkflow.class, "outputs", WdlParameterOutput.class);
		config.readConfig.setIgnoreUnknownProperties(true);

		WdlApp app = yamlReader.read(WdlApp.class);
		yamlReader.close();

		updateApp(filename, app);
		return app;
	}

	public static WdlApp loadAppFromString(String filename, String content) throws IOException {
		StringReader reader = new StringReader(content);
		return loadAppFromReader(filename, reader);
	}

	public static WdlApp loadAppFromFile(String filename) throws IOException {
		FileReader reader = new FileReader(filename);
		return loadAppFromReader(filename, reader);
	}

	private static void updateApp(String filename, WdlApp app) throws IOException {
		String path = new File(new File(filename).getAbsolutePath()).getParentFile().getAbsolutePath();

		app.setPath(path);
		app.setManifestFile(filename);

		if (app.getId() == null || app.getId().isEmpty()) {
			throw new IOException("No field 'id' found in file '" + filename + "'.");
		}

		if (app.getVersion() == null || app.getVersion().isEmpty()) {
			throw new IOException("No field 'version' found in file '" + filename + "'.");
		}

		if (app.getName() == null || app.getName().isEmpty()) {
			throw new IOException("No field 'name' found in file '" + filename + "'.");
		}
	}
}
