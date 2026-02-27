package cloudgene.mapred.server.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.plugins.nextflow.NextflowCollector;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.server.auth.AuthenticationService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;

@Controller
public class NextflowController {

	protected static final Logger log = LoggerFactory.getLogger(NextflowController.class);

	@Inject
	protected Application application;

	@Inject
	protected AuthenticationService authenticationService;

	@Post("/api/v2/collect/{job}")
	@Secured(SecurityRule.IS_ANONYMOUS)
	public String post(String job, @Body Map<String, Object> event) {

		try {
			NextflowCollector info = NextflowCollector.getInstance();
			if (event.containsKey("trace")) {
				info.addEvent(job, event);
			}
		} catch (Exception e) {
			log.error("Parsing Nextflow weblog for job {} failed.", job, e);
		}

		return "";
	}
}
