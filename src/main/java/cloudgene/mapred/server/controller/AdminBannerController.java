package cloudgene.mapred.server.controller;

import cloudgene.mapred.core.Banner;
import cloudgene.mapred.core.User;
import cloudgene.mapred.server.services.BannerService;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;

@Controller("/api/v2/admin/banner")
@Secured(User.ROLE_ADMIN)
public class AdminBannerController {

	@Introspected
	@Serdeable.Deserializable
	public record CreateInput(@NonNull String type, @NonNull String message) {}

	@Introspected
	@Serdeable.Deserializable
	public record UpdateInput(@Nullable String type, @Nullable String message) {}

	@Introspected
	@Serdeable.Deserializable
	public record SwapInput(@NonNull Integer id1, @NonNull Integer id2) {}

	@Inject
	protected BannerService bannerService;

	@Post("/")
	public Banner create(@Body CreateInput payload) {
		return bannerService.create(payload.type, payload.message);
	}

	@Put("/{id}")
	public HttpResponse<?> update(int id, @Body UpdateInput payload) {
		boolean updated = bannerService.update(id, payload.type, payload.message);
		return updated ? HttpResponse.noContent() : HttpResponse.notFound();
	}

	@Delete("/{id}")
	public HttpResponse<?> delete(int id) {
		boolean deleted = bannerService.delete(id);
		return deleted ? HttpResponse.noContent() : HttpResponse.notFound();
	}

	@Post("/swap")
	public HttpResponse<?> swap(@Body SwapInput payload) {
		boolean swapped = bannerService.swap(payload.id1, payload.id2);
		return swapped ? HttpResponse.noContent() : HttpResponse.badRequest();
	}
}
