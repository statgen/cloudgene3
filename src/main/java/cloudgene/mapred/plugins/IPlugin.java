package cloudgene.mapred.plugins;

import cloudgene.mapred.util.config.Settings;
import cloudgene.mapred.wdl.WdlApp;

import java.io.IOException;
import java.util.Map;

public interface IPlugin {

	public String getId();
	
	public String getName();
	
	public boolean isInstalled();
	
	public String getDetails();
	
	public void configure(Settings settings);
	
	public String getStatus();

	public Map<String, String> getConfig(WdlApp app) throws IOException;

	public void updateConfig(WdlApp app, Map<String, String> config) throws IOException;

}
