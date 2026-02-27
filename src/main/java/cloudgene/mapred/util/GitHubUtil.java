package cloudgene.mapred.util;

import static io.micronaut.http.HttpHeaders.ACCEPT;
import static io.micronaut.http.HttpHeaders.USER_AGENT;

import java.net.URI;
import java.util.Map;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.netty.DefaultHttpClient;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

public class GitHubUtil {

	public static class Repository {
		private String user;

		private String repo;

		private String tag;

		private String yaml = null;

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public String getRepo() {
			return repo;
		}

		public void setRepo(String repo) {
			this.repo = repo;
		}

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public String getYaml() {
			return yaml;
		}

		public void setYaml(String yaml) {
			this.yaml = yaml;
		}
	}

	/**
	 * Parses a GitHub repo string like {@code username/repo[/path/to/cloudgene.yaml][@git-ref]}
	 * into its component parts.
	 *
	 * @param shorthand String identifying a GitHub repo (and possibly a git tag, and the
	 *                  path to the YAML config).
	 * @return The parsed repo details.
	 */
	public static Repository parseShorthand(@NotNull String shorthand) throws IllegalArgumentException {
		Repository repo = new Repository();

		// username/repo[/subdir][@ref]
		String[] tiles2 = shorthand.split("@");
		String[] tiles = tiles2[0].split("/", 3);

		if (tiles.length < 2) {
			throw new IllegalArgumentException(
					"Expected input format 'user/repo[/path][@ref]', but did not find a forward slash in shorthand: "
							+ shorthand);
		}

		repo.setUser(tiles[0]);
		repo.setRepo(tiles[1]);

		if (tiles.length > 2) {
			repo.setYaml(tiles[2]);
		}

		if (tiles2.length == 2) {
			repo.setTag(tiles2[1]);
		} else if (tiles2.length > 2) {
			throw new IllegalArgumentException(
					"Expected input format 'user/repo[/path][@ref]', but found too many @ signs in shorthand: "
							+ shorthand);
		}

		return repo;
	}

	/**
	 * Returns a GitHub URL pointing to the repository's {@code zipball} (a ZIP file
	 * containing the repository's contents).
	 *
	 * @param repo The repository to download.
	 * @return The URL to the repo's {@code zipball}.
	 */
	@NotNull
	public static String buildUrlFromRepository(@NotNull Repository repo) throws GitHubException {
		String tag = repo.getTag();

		if (tag != null && tag.equalsIgnoreCase("latest")) {
			tag = getLatestReleaseFromRepository(repo);
		}

		String url = "https://api.github.com/repos/" + repo.getUser() + "/" + repo.getRepo() + "/zipball";

		if (tag != null) {
			url += "/" + tag;
		}

		return url;
	}

	/**
	 * Attempts to retrieve the {@code tag} that corresponds to {@code latest} in
	 * the repo, by calling the GitHub API.
	 *
	 * @param repo GitHub repository queried for its latest version.
	 * @return The latest version {@code tag}.
	 */
	public static String getLatestReleaseFromRepository(@NotNull Repository repo) throws GitHubException {
		String urlString = "https://api.github.com/repos/" + repo.getUser() + "/" + repo.getRepo() + "/releases/latest";

		HttpClient httpClient = new DefaultHttpClient();

		try {
			URI uri = new URI(urlString);

			HttpRequest<?> req = HttpRequest.GET(uri)
					.header(USER_AGENT, "Cloudgene")
					.header(ACCEPT, "application/vnd.github.v3+json, application/json");

			String tag = Mono.from(httpClient.retrieve(req, Map.class))
					.block()
					.get("tag_name")
					.toString();

			if (tag == null || tag.isBlank()) {
				throw new GitHubException("Received empty tag from repository");
			}

			return tag;
		} catch (Exception e) {
			throw new GitHubException("Failed to retrieve latest tag from repository", e);
		} finally {
			httpClient.close();
		}
	}
}
