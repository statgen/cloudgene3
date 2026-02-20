package cloudgene.mapred.plugins.docker;

import cloudgene.mapred.jobs.CloudgeneStepFactory;
import cloudgene.mapred.plugins.IPlugin;
import cloudgene.mapred.util.config.Settings;
import cloudgene.mapred.wdl.WdlApp;

import java.util.Map;

public class DockerPlugin implements IPlugin {

	public static final String ID = "docker";

	private Settings settings;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return "Docker";
	}

	@Override
	public boolean isInstalled() {
		DockerBinary docker = DockerBinary.build(settings);
		return docker.isInstalled();
	}

	@Override
	public String getDetails() {
		DockerBinary docker = DockerBinary.build(settings);
		return docker.getVersion();
	}

	@Override
	public void configure(Settings settings) {
		this.settings = settings;
		CloudgeneStepFactory factory = CloudgeneStepFactory.getInstance();
		factory.register("docker", DockerStep.class);
	}

	@Override
	public String getStatus() {
		if (isInstalled()) {
			return "Docker support enabled.";
		} else {
			return "Docker Binary not found. Docker support disabled.";
		}
	}

	@Override
	public Map<String, String> getConfig(WdlApp app) {
		return null;
	}

	@Override
	public void updateConfig(WdlApp app, Map<String, String> config) {

	}

}
