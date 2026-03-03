package cloudgene.mapred.server.auth;

import java.util.Arrays;
import java.util.Date;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.util.HashUtil;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.provider.AuthenticationProvider;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DatabaseAuthenticationProvider implements AuthenticationProvider<HttpRequest<?>, String, String> {

	private static final Logger log = LoggerFactory.getLogger(DatabaseAuthenticationProvider.class);

	private static final String MESSAGE_LOGIN_FAILED = "Login Failed! Wrong Username or Password.";
	private static final String MESSAGE_ACCOUNT_IS_INACTIVE = "Login Failed! User account is not activated.";
	private static final String MESSAGE_ACCOUNT_LOCKED = "The user account is locked for %d minutes. Too many failed logins.";

	public static final int MAX_LOGIN_ATTEMPTS = 5;
	public static final int LOCKING_TIME_MIN = 30;

	@Inject
	protected Application application;

	@Override
	public @NonNull AuthenticationResponse authenticate(
			@Nullable HttpRequest<?> httpRequest,
			@NonNull AuthenticationRequest<String, String> authenticationRequest) {

		String loginUsername = authenticationRequest.getIdentity();
		String loginPassword = authenticationRequest.getSecret();

		UserDao dao = new UserDao(application.getDatabase());
		User user = dao.findByUsername(loginUsername);

		if (user == null) {
			return AuthenticationResponse.failure(MESSAGE_LOGIN_FAILED);
		}

		if (!user.isActive()) {
			log.info(
					"Authorization failure: User account is not activated for account {} (ID {} - email {})",
					user.getUsername(), user.getId(), user.getMail());

			return AuthenticationResponse.failure(MESSAGE_ACCOUNT_IS_INACTIVE);
		}

		if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
			if (user.getLockedUntil() == null || user.getLockedUntil().after(new Date())) {
				log.info(
						"Authorization failure: login retries are currently locked for account {} (ID {} - email {})",
						user.getUsername(), user.getId(), user.getMail());

				return AuthenticationResponse.failure(
						String.format(MESSAGE_ACCOUNT_LOCKED, LOCKING_TIME_MIN));

			} else {
				// penalty time is over. set to zero
				log.info(
						"Authorization: Account login lock has expired; releasing for account {} (ID {} - email {})",
						user.getUsername(), user.getId(), user.getMail());

				user.setLoginAttempts(0);
			}
		}

		if (HashUtil.checkPassword(loginPassword, user.getPassword())) {
			user.setLoginAttempts(0);
			user.setLastLogin(new Date());
			dao.update(user);

			String message = String.format("Authorization success: user login %s (ID %s - email %s)",
					user.getUsername(), user.getId(), user.getMail());
			if (user.isAdmin()) {
				// Note: Admin user logins are called out explicitly, to aid log analysis in the
				// event of a breach
				message += " (ADMIN)";
			}

			log.info(message);
			return AuthenticationResponse.success(user.getUsername(), Arrays.asList(user.getRoles()));
		} else {
			// count failed logins
			int attempts = user.getLoginAttempts();
			attempts++;
			user.setLoginAttempts(attempts);

			// too many, lock user
			if (attempts >= MAX_LOGIN_ATTEMPTS) {
				log.warn(
						"Authorization failure: User account {} (ID {} - email {}) locked due to too many failed logins",
						user.getUsername(), user.getId(), user.getMail());

				user.setLockedUntil(new Date(System.currentTimeMillis() + (LOCKING_TIME_MIN * 60 * 1000)));
			}

			dao.update(user);

			log.warn("Authorization failure: Invalid password for username: {}", loginUsername);
			return AuthenticationResponse.failure(MESSAGE_LOGIN_FAILED);
		}
	}
}
