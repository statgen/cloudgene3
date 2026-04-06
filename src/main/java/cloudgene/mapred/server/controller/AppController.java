package cloudgene.mapred.server.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.core.User;
import cloudgene.mapred.server.auth.AuthenticationService;
import cloudgene.mapred.server.auth.AuthenticationType;
import cloudgene.mapred.server.responses.ApplicationResponse;
import cloudgene.mapred.server.responses.WdlAppResponse;
import cloudgene.mapred.server.services.ApplicationService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class AppController {

	private static final Logger log = LoggerFactory.getLogger(AppController.class);

	@Inject
	protected cloudgene.mapred.server.Application application;

	@Inject
	protected AuthenticationService authenticationService;

	@Inject
	protected ApplicationService applicationService;

	@Get("/api/v2/server/apps/{appId}")
	@Secured(SecurityRule.IS_ANONYMOUS)
	public WdlAppResponse getApp(@Nullable Authentication authentication, String appId) {

		User user = authenticationService.getUserByAuthentication(authentication, AuthenticationType.ALL_TOKENS);
		Application app = applicationService.getByIdAndUser(user, appId);

		applicationService.checkRequirements(app);
		ApplicationRepository repository = applicationService.getRepository();
		List<Application> apps = repository.getAllByUser(user, ApplicationRepository.APPS_AND_DATASETS);

		WdlAppResponse response = WdlAppResponse.build(app.getWdlApp(), apps);

		response.setS3Workspace(application.getSettings().getExternalWorkspaceType().equalsIgnoreCase("S3")
				&& application.getSettings().getExternalWorkspaceLocation().isEmpty());

		log.info("Returning info for app '{}' for authenticated user.", appId);
		return response;
	}

	@Delete("/api/v2/server/apps/{appId}")
	@Secured(User.ROLE_ADMIN)
	public ApplicationResponse removeApp(String appId) {
		Application app = applicationService.removeApp(appId);

		log.info("Application '{}' removed successfully.", appId);
		return ApplicationResponse.build(app);
	}

	@Put("/api/v2/server/apps/{appId}")
	@Secured(User.ROLE_ADMIN)
	public ApplicationResponse updateApp(String appId, @Nullable Boolean enabled, @Nullable String permission,
			@Nullable Boolean reinstall, @Nullable Map<String, String> config) {

		Application app = applicationService.getById(appId);

		if (enabled != null) {
			applicationService.enableApp(app, enabled);
		}

		applicationService.updatePermissions(app, permission);

		log.info("Application '{}' updated successfully. Permission: '{}'", appId, permission);
		return ApplicationResponse.build(app);
	}

	@Get("/api/v2/server/apps/{appId}/settings")
	@Secured(User.ROLE_ADMIN)
	public ApplicationResponse getAppSettings(String appId) {
		Application app = applicationService.getById(appId);
		ApplicationRepository repository = applicationService.getRepository();

		log.info("Application settings loaded successfully for '{}'", appId);
		return ApplicationResponse.buildWithDetails(app, this.application.getSettings(), repository);

	}

	@Put("/api/v2/server/apps/{appId}/settings")
	@Secured(User.ROLE_ADMIN)
	public ApplicationResponse updateAppSettings(String appId, @Nullable Boolean enabled, @Nullable String permission,
			@Nullable Boolean reinstall, @Nullable Map<String, String> config) throws IOException {

		Application app = applicationService.getById(appId);
		applicationService.updateConfig(app, config);

		log.info("Updated settings for application '{}'. Config keys: {}",
				appId, config != null ? config.keySet() : null);

		ApplicationRepository repository = applicationService.getRepository();
		return ApplicationResponse.buildWithDetails(app, this.application.getSettings(), repository);

	}

	@Post("/api/v2/server/apps")
	@Secured(User.ROLE_ADMIN)
	public ApplicationResponse install(@Nullable String url) {
		Application app = applicationService.installApp(url);

		log.info("Application '{}' installed successfully from url='{}'", app.getId(), url);
		return ApplicationResponse.build(app);
	}

	@Get("/api/v2/server/apps")
	@Secured(User.ROLE_ADMIN)
	public List<ApplicationResponse> list(@Nullable @QueryValue("reload") Boolean reload) {
		if (reload == null) {
			reload = false;
		}

		List<Application> apps = applicationService.listApps(reload);
		ApplicationRepository repository = applicationService.getRepository();

		log.info("Returning {} applications. IDs: {}", apps.size(), apps.stream().map(Application::getId).toList());
		return ApplicationResponse.buildWithDetails(apps, application.getSettings(), repository);
	}
}
