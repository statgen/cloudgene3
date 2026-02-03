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
	public Banner add(@NotNull String type, @NotNull String message) {
		// Raises IllegalArgumentException if null or malformed.
		Banner.Type bannerType = Banner.Type.of(type);

		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("message must be a non-blank string.");
		}

		BannerDao dao = new BannerDao(application.getDatabase());

		Banner banner = dao.insert(bannerType, message);
		return banner;
	}

	public boolean delete(Banner banner) {
		if (banner == null) {
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
		if (first == null || second == null) {
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
