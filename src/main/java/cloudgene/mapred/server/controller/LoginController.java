package cloudgene.mapred.server.controller;

import cloudgene.mapred.server.Application;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.event.SecurityEvent;
import jakarta.validation.Valid;

import cloudgene.mapred.server.auth.DatabaseAuthenticationProvider;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.security.event.LoginFailedEvent;
import io.micronaut.security.event.LoginSuccessfulEvent;
import io.micronaut.security.token.bearer.AccessRefreshTokenLoginHandler;
import jakarta.inject.Inject;

import java.util.Locale;

@Controller
public class LoginController {

	@Inject
	private Application application;

	@Inject
	protected AccessRefreshTokenLoginHandler loginHandler;

	@Inject
	protected ApplicationEventPublisher<SecurityEvent> eventPublisher;

	@Inject
	protected DatabaseAuthenticationProvider authenticator;

	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON })
	@Post("/login")
	public MutableHttpResponse<?> login(
			@Valid @Body UsernamePasswordCredentials usernamePasswordCredentials,
			HttpRequest<?> request) {

		AuthenticationResponse authResponse = authenticator.authenticate(request, usernamePasswordCredentials);
		String url = application.getSettings().getServerUrl();
		Locale locale = request.getLocale().orElse(Locale.US);

		if (authResponse.isAuthenticated() && authResponse.getAuthentication().isPresent()) {
			Authentication auth = authResponse.getAuthentication().get();
			eventPublisher.publishEvent(new LoginSuccessfulEvent(auth, url, locale));
			return loginHandler.loginSuccess(auth, request);
		} else {
			eventPublisher.publishEvent(new LoginFailedEvent(authResponse, usernamePasswordCredentials, url, locale));
			return loginHandler.loginFailed(authResponse, request);
		}
	}
}
