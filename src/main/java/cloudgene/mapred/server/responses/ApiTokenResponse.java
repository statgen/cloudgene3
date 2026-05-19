package cloudgene.mapred.server.responses;

import cloudgene.mapred.core.ApiToken;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.time.Instant;

@JsonClassDescription
public record ApiTokenResponse(
		boolean success,
		String message,
		String token,
		Instant expiresOn) {

	public static ApiTokenResponse ok(ApiToken token) {
		return new ApiTokenResponse(true, "Creation successful", token.accessToken(), token.expiresOn());
	}

	public static ApiTokenResponse fail(String message) {
		return new ApiTokenResponse(false, message, null, null);
	}
}
