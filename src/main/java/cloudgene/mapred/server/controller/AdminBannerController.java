package cloudgene.mapred.server.controller;

import cloudgene.mapred.core.Banner;
import cloudgene.mapred.core.User;
import cloudgene.mapred.server.services.BannerService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Controller("/api/v2/admin/banner")
@Secured(User.ROLE_ADMIN)
public class AdminBannerController {

	@Inject
	protected BannerService bannerService;

	@Get("/")
	public List<Banner> list() {
		return bannerService.getAll();
	}

	@Post("/")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Banner add(
			@NotNull String type,
			@NotNull String message) {

		Banner banner = bannerService.add(type, message);
		return banner;
	}

	@Delete("/{id}")
	public HttpResponse<?> delete(int id) {
		boolean deleted = bannerService.delete(id);

		if (deleted) {
			return HttpResponse.noContent();
		} else {
			return HttpResponse.notFound();
		}
	}

	@Post("/swap")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public HttpResponse<?> swap(int id1, int id2) {
		boolean swapped = bannerService.swap(id1, id2);

		if (swapped) {
			return HttpResponse.noContent();
		} else {
			return HttpResponse.badRequest();
		}
	}
}
