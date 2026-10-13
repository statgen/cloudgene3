package cloudgene.mapred.server.responses;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Response type for {@code GET /api/v2/server}
 *
 * @param name                        Name of this server.
 * @param emailRequired               Are users required to provide an email
 *                                    address?
 * @param serverUsesEmail             Is the server configured to send email?
 * @param userEmailDescription        Display string explaining how it works if
 *                                    the user provides an email address.
 * @param userWithoutEmailDescription Display string explaining how it works if
 *                                    the user does NOT provide an email
 *                                    address.
 * @param oauth                       Possibly unused?
 * @param user                        Basic metadata about the user. Only filled
 *                                    if the user is logged in.
 * @param apps                        Primary list of available Cloudgene apps.
 * @param deprecatedApps              List of DEPRECATED Cloudgene apps.
 * @param experimentalApps            List of EXPERIMENTAL Cloudgene apps.
 * @param loggedIn                    True if the user is logged in.
 * @param maintenance                 True if the server is in maintenance mode.
 * @param maintenanceMessage          Display message to be shown in a banner if
 *                                    the server is in maintenance mode.
 */
@JsonClassDescription
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerResponse(
		String name,

		boolean emailRequired,
		boolean serverUsesEmail,

		@Deprecated String userEmailDescription,
		@Deprecated String userWithoutEmailDescription,

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
	public record User(String username, String fullName, String mail, boolean admin) {
	}

	@JsonClassDescription
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record App(String id, String name, String version) {
	}
}
