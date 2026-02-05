package cloudgene.mapred.server.controller;

import cloudgene.mapred.core.Banner;
import cloudgene.mapred.server.services.BannerService;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;

import java.util.List;

@Controller("/api/v2/banner")
@Secured(SecurityRule.IS_ANONYMOUS)
public class BannerController {

	@Inject
	protected BannerService bannerService;

	@Get("/")
	public List<Banner> list() {
		return bannerService.getAll();
	}
}
