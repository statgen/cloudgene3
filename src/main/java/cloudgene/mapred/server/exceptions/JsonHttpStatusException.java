package cloudgene.mapred.server.exceptions;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;

import java.io.Serial;

public class JsonHttpStatusException extends HttpStatusException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final MessageWrapper object;

	public JsonHttpStatusException(HttpStatus status, String message) {
		super(status, new MessageWrapper(message, false));
		object = new MessageWrapper(message, false);
	}

	public MessageWrapper getObject() {
		return object;
	}
}
