package cloudgene.mapred.server.auth;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.micronaut.core.annotation.NonNull;
import org.apache.commons.lang3.RandomStringUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import cloudgene.mapred.core.ApiToken;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.server.responses.ValidatedApiTokenResponse;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator;
import io.micronaut.security.token.jwt.validator.ReactiveJsonWebTokenValidator;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

@Singleton
public class AuthenticationService {

	private static final String MESSAGE_VALID_API_TOKEN = "API Token was created by %s and is valid.";
	private static final String MESSAGE_INVALID_API_TOKEN = "Invalid API Token.";

	private static final String ATTRIBUTE_TOKEN_TYPE = "token_type";
	private static final String ATTRIBUTE_API_HASH = "api_hash";

	@Inject
	protected Application application;

	@Inject
	protected JwtTokenGenerator generator;

	@Inject
	protected ReactiveJsonWebTokenValidator<?, ?> validator;

	public User getUserByAuthentication(Authentication authentication) {
		return getUserByAuthentication(authentication, AuthenticationType.ACCESS_TOKEN);
	}

	public User getUserByAuthentication(
			Authentication authentication,
			AuthenticationType authenticationType) {

		if (authentication == null) {
			throw new AuthenticationException();
		}

		UserDao userDao = new UserDao(application.getDatabase());
		User user = userDao.findByUsername(authentication.getName());

		Map<String, Object> attributes = authentication.getAttributes();

		if (attributes.containsKey(ATTRIBUTE_TOKEN_TYPE)) {
			String tokenType = attributes.get(ATTRIBUTE_TOKEN_TYPE).toString();

			if (tokenType.equalsIgnoreCase(AuthenticationType.API_TOKEN.toString())) {
				if (authenticationType == AuthenticationType.API_TOKEN
						|| authenticationType == AuthenticationType.ALL_TOKENS) {
					if (user.getApiToken().equals(attributes.get(ATTRIBUTE_API_HASH))) {
						user.setAccessedByApi(true);
						return user;
					}
				}
			} else if (tokenType.equalsIgnoreCase(AuthenticationType.ACCESS_TOKEN.toString())) {
				if (authenticationType == AuthenticationType.ACCESS_TOKEN
						|| authenticationType == AuthenticationType.ALL_TOKENS) {
					return user;
				}
			}
		} else {
			if (authenticationType == AuthenticationType.ACCESS_TOKEN
					|| authenticationType == AuthenticationType.ALL_TOKENS) {
				return user;
			}
		}

		throw new AuthorizationException(authentication);
	}

	/**
	 * Creates a new {@link ApiToken} (JWT + metadata) for the given user that will
	 * expire in a set number of days.
	 * <p>
	 * The token hash (random salt) and expiration timestamp are saved in the
	 * database. Only one hash per user is stored.
	 *
	 * @param user         The produced token allows the bearer to act on this
	 *                     user's behalf (non-null).
	 * @param lifetimeDays Number of days since creation until the token expires.
	 *                     Range: 0..90 (inclusive).
	 * @return The newly created token (non-null, throws on failure).
	 */
	public @NonNull ApiToken createApiToken(@NonNull User user, int lifetimeDays) throws IOException {
		// NOTE(Marc): lifetimeDays = 0 will immediately expire. The test suite depends
		//             on this to test expired tokens.
		if (lifetimeDays < 0 || lifetimeDays > 90) {
			throw new IllegalArgumentException(
					"lifetimeDays should be in range 0..90 (inclusive); found: " + lifetimeDays);
		}

		int lifetimeSeconds = 24 * 60 * 60 * lifetimeDays;

		String hash = RandomStringUtils.secure().nextAlphabetic(30);

		Map<String, Object> attributes = new HashMap<>();

		attributes.put(ATTRIBUTE_TOKEN_TYPE, AuthenticationType.API_TOKEN.toString());
		attributes.put(ATTRIBUTE_API_HASH, hash);

		// Additional attributes needed by imputationbot
		attributes.put("username", user.getUsername());
		attributes.put("name", user.getFullName());
		attributes.put("mail", user.getMail());
		attributes.put("api", true);

		Authentication authentication = Authentication.build(user.getUsername(), attributes);
		Optional<String> jwt = generator.generateToken(authentication, lifetimeSeconds);

		if (jwt.isEmpty()) {
			throw new IOException("Failed to generate JWT token.");
		}

		Date expiresOn = new Date(System.currentTimeMillis() + (lifetimeSeconds * 1_000L));

		ApiToken apiToken = new ApiToken(jwt.get(), hash, expiresOn);

		// store random hash (not access token) in database to validate token
		user.setApiToken(hash);
		user.setApiTokenExpiresOn(expiresOn);

		UserDao userDao = new UserDao(application.getDatabase());
		boolean successful = userDao.update(user);

		if (!successful) {
			throw new IOException("Failed to update database.");
		}

		return apiToken;
	}

	public Mono<ValidatedApiTokenResponse> validateApiToken(String token) {
		Publisher<Authentication> authentication = validator.validateToken(token, null);

		return Mono.<ValidatedApiTokenResponse>create(emitter -> {
			authentication.subscribe(new Subscriber<>() {

				private Subscription subscription;

				@Override
				public void onComplete() {
					// handle empty publisher. e.g. when token is invalid
					emitter.success(ValidatedApiTokenResponse.error(MESSAGE_INVALID_API_TOKEN));
				}

				@Override
				public void onError(Throwable throwable) {
					emitter.error(throwable);
				}

				@Override
				public void onNext(Authentication authentication) {
					try {
						User user = getUserByAuthentication(authentication, AuthenticationType.API_TOKEN);
						if (user == null) {
							emitter.success(ValidatedApiTokenResponse.error(MESSAGE_INVALID_API_TOKEN));
						} else {
							ValidatedApiTokenResponse response = ValidatedApiTokenResponse
									.valid(MESSAGE_VALID_API_TOKEN, user);
							response.setExpire((Date) authentication.getAttributes().get("exp"));
							emitter.success(response);
						}
					} catch (Exception e) {
						emitter.success(ValidatedApiTokenResponse.error(MESSAGE_INVALID_API_TOKEN));
					}
					subscription.request(1);
				}

				@Override
				public void onSubscribe(Subscription subscription) {
					this.subscription = subscription;
					subscription.request(1);
				}
			});
		}).single();
	}
}
