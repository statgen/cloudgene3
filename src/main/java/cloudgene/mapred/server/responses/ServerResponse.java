package cloudgene.mapred.server.responses;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonClassDescription
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerResponse(
		String name,
		String background,
		String foreground,
		boolean emailRequired,

		// TODO(Marc): maybe we should get rid of these two.
		String userEmailDescription,
		String userWithoutEmailDescription,

		List<String> oauth, // TODO(Marc): Is this used?

		User user,

		List<App> apps,
		// TODO(Marc): Are these two used?
		List<App> deprecatedApps,
		List<App> experimentalApps,

		boolean loggedIn,

		boolean maintenance,
		String maintenanceMessage) {

	@JsonClassDescription
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record User(String username, String fullName, String mail, boolean admin) {}

	@JsonClassDescription
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record App(String id, String name, String version) {}
}
