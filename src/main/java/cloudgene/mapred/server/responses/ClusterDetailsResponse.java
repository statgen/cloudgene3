package cloudgene.mapred.server.responses;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonClassDescription
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ClusterDetailsResponse(
		boolean maintenance,
		boolean blocked,
		String version,
		String hash,
		boolean dirty,
		int threads,
		int maxJobsUser,
		String builtBy,
		String builtTime,
		long uptimeMs,

		String workspacePath,
		long freeDiskSpace,
		long totalDiskSpace,
		long usedDiskSpace,

		List<Plugin> plugins,

		int dbMaxActive,
		int dbActive,
		int dbMaxIdle,
		int dbIdle,
		int dbMaxOpenPrepStatements) {

	@JsonClassDescription
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	public record Plugin(
			String name,
			boolean enabled,
			String details,
			String error) {
		public static Plugin ok(String name, String details) {
			return new Plugin(name, true, details, null);
		}

		public static Plugin err(String name, String error) {
			return new Plugin(name, false, null, error);
		}
	}
}
