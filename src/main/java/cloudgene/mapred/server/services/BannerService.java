package cloudgene.mapred.server.services;

import cloudgene.mapred.core.Banner;
import cloudgene.mapred.database.BannerDao;
import cloudgene.mapred.server.Application;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Singleton
public class BannerService {

	@Inject
	protected Application application;

	public List<Banner> getAll() {
		BannerDao dao = new BannerDao(application.getDatabase());
		List<Banner> banners = dao.findAll();
		return banners;
	}

	@Nullable
	public Banner create(@NotNull String type, @NotNull String message) {
		// Raises IllegalArgumentException if null or malformed.
		Banner.Type bannerType = Banner.Type.of(type);

		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("message must be a non-blank string.");
		}

		BannerDao dao = new BannerDao(application.getDatabase());

		Banner banner = dao.insert(bannerType, message);
		return banner;
	}

	public boolean update(
			@Nullable Banner banner,
			@Nullable Banner.Type type,
			@Nullable String message) {

		if (banner == null || banner.getId() < 0 || banner.getPosition() < 0) {
			return false;
		}

		if (type != null) {
			banner.setType(type);
		}

		if (message != null && !message.isBlank()) {
			message = message.trim();
			banner.setMessage(message);
		}

		BannerDao dao = new BannerDao(application.getDatabase());
		return dao.update(banner);
	}

	public boolean update(
			int id,
			@Nullable String type,
			@Nullable String message) {

		Banner.Type bannerType;
		if (type != null && !type.isBlank()) {
			bannerType = Banner.Type.of(type);
		} else {
			bannerType = null;
		}

		BannerDao dao = new BannerDao(application.getDatabase());
		Banner banner = dao.findById(id);

		return update(banner, bannerType, message);
	}

	public boolean delete(Banner banner) {
		if (banner == null || banner.getId() < 0 || banner.getPosition() < 0) {
			return false;
		}

		BannerDao dao = new BannerDao(application.getDatabase());

		boolean success = dao.delete(banner);
		return success;
	}

	public boolean delete(int id) {
		BannerDao dao = new BannerDao(application.getDatabase());
		Banner banner = dao.findById(id);
		return delete(banner);
	}

	public boolean swap(Banner first, Banner second) {
		if (first == null || first.getId() < 0 || first.getPosition() < 0) {
			return false;
		}

		if (second == null || second.getId() < 0 || second.getPosition() < 0) {
			return false;
		}

		BannerDao dao = new BannerDao(application.getDatabase());
		return dao.swap(first, second);
	}

	public boolean swap(int id1, int id2) {
		BannerDao dao = new BannerDao(application.getDatabase());

		Banner first = dao.findById(id1);
		Banner second = dao.findById(id2);

		return swap(first, second);
	}
}
