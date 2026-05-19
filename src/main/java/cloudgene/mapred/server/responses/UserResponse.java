package cloudgene.mapred.server.responses;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cloudgene.mapred.core.User;
import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription
public record UserResponse(
		int id,
		String username,
		String fullName,
		Instant lastLogin,
		String lockedUntil,
		boolean active,
		int loginAttempts,
		String role,
		String mail,
		boolean admin,
		boolean hasApiToken,
		Instant apiTokenExpiresOn,
		boolean apiTokenValid) {

	public static UserResponse build(User user) {
		int id = user.getId();
		String username = user.getUsername();
		String fullName = user.getFullName();
		Instant lastLogin = user.getLastLogin() != null ? user.getLastLogin().toInstant() : null;
		String lockedUntil = lockedUntilToString(user.getLockedUntil());
		boolean active = user.isActive();
		int loginAttempts = user.getLoginAttempts();
		String role = String.join(User.ROLE_SEPARATOR, user.getRoles()).toLowerCase();
		String mail = user.getMail();
		boolean admin = user.isAdmin();

		boolean hasApiToken = user.getApiToken() != null && !user.getApiToken().isEmpty();
		boolean apiTokenValid = false;
		Instant apiTokenExpiresOn = user.getApiTokenExpiresOn() != null ? user.getApiTokenExpiresOn().toInstant() : null;

		if (hasApiToken && apiTokenExpiresOn != null) {
			if (apiTokenExpiresOn.toEpochMilli() > System.currentTimeMillis()) {
				apiTokenValid = true;
			}
		}

		return new UserResponse(id, username, fullName, lastLogin, lockedUntil, active, loginAttempts, role, mail,
				admin, hasApiToken, apiTokenExpiresOn, apiTokenValid);
	}

	public static List<UserResponse> build(List<User> users) {
		List<UserResponse> response = new ArrayList<>();

		for (User user : users) {
			response.add(UserResponse.build(user));
		}

		return response;
	}

	public static String lockedUntilToString(Date date) {
		if (date != null) {
			if (date.after(new Date())) {
				return date.toString();
			} else {
				return "";
			}
		} else {
			return "";
		}
	}
}
