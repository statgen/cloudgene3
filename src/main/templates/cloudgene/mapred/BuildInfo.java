package cloudgene.mapred;

public final class BuildInfo {
	private BuildInfo() {}

	public static final String APP_NAME = "${project.name}";
	public static final String APP_ID = "${project.artifactId}";
	public static final String VERSION = "${project.version}";
	public static final String ORG_NAME = "${project.organization.name}";
	public static final String URL = "${project.url}";
	public static final String BUILD_TIME = "${maven.build.timestamp}";
	public static final String BUILT_BY = "statgen";
}
