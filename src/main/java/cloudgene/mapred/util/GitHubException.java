package cloudgene.mapred.util;

public class GitHubException extends Exception {

	private static final long serialVersionUID = 1L;

	public GitHubException(String message) {
		super(message);
	}

	public GitHubException(String message, Throwable cause) {
		super(message, cause);
	}
}
