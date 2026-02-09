package cloudgene.mapred.cli;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.esotericsoftware.yamlbeans.YamlReader;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.util.S3Util;

public class CloneApplications extends BaseTool {

	public CloneApplications(String[] args) {
		super(args);
	}

	@Override
	public void createParameters() {}

	@Override
	public int run() {
		if (args.length != 1) {
			System.out.println("Usage: cloudgene clone <filename|url> ");
			System.out.println();
			System.exit(1);
		}

		String repo = args[0];
		File tmpFile = new File("repo.yaml");

		if (repo.startsWith("http://") || repo.startsWith("https://")) {
			try {
				URL url = new URI(repo).toURL();
				FileUtils.copyURLToFile(url, tmpFile);
				repo = tmpFile.getPath();
			} catch (Exception e) {
				System.out.println("Error during download repository from " + repo);
				e.printStackTrace();
				return 1;
			}
		} else if (repo.startsWith("s3://")) {
			try {
				S3Util.copyToFile(repo, tmpFile);
				repo = tmpFile.getPath();
			} catch (Exception e) {
				System.out.println("Error during download repository from " + repo);
				e.printStackTrace();
				return 1;
			}
		}

		try {
			YamlReader reader = new YamlReader(new FileReader(repo));

			while (true) {
				Map<?, ?> entry = reader.read(Map.class);
				if (entry == null) {
					break;
				}

				String url = entry.get("url").toString();
				System.out.println("Installing application " + url + "...");

				try {
					List<Application> applications = repository.install(url);

					if (!applications.isEmpty()) {
						settings.save();
						for (Application application : applications) {
							printlnInGreen("[OK] Application '" + application.getWdlApp().getName() + "' installed.");
						}
						System.out.println();
					} else {
						printlnInRed("[ERROR] No valid Application found in repo '" + url + "'\n");
						return 1;
					}
				} catch (Exception e) {
					printlnInRed("[ERROR] Application not installed:" + e + "\n");
				}
			}

			reader.close();
		} catch (Exception e) {
			printlnInRed("[ERROR] Error reading file '" + repo + "':" + e + "\n");
		}

		return 0;
	}
}
