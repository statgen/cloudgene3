package cloudgene.mapred.util;

import genepi.io.FileUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.CompletedPart;
import io.micronaut.http.server.multipart.MultipartBody;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Singleton
public class FormUtil {

	private static final Logger log = LoggerFactory.getLogger(FormUtil.class);

	@Inject
	protected cloudgene.mapred.server.Application application;

	public Publisher<HttpResponse<Object>> processMultipartBody(MultipartBody body, File folder,
			Function<List<Parameter>, HttpResponse<Object>> callback) {

		return Mono.<HttpResponse<Object>>create(emitter -> {

			body.subscribe(new Subscriber<CompletedPart>() {

				List<Parameter> form = new ArrayList<>();

				private Subscription s;

				@Override
				public void onSubscribe(Subscription s) {
					this.s = s;
					s.request(1);
				}

				@Override
				public void onNext(CompletedPart completedPart) {
					log.debug("Parse parameter " + completedPart.getName() + "...");
					Parameter formParameter = processCompletedPart(completedPart, folder);
					if (formParameter != null) {
						form.add(formParameter);
					}
					log.debug("Parsed parameter " + completedPart.getName() + ".");
					s.request(1);
				}

				@Override
				public void onError(Throwable t) {
					emitter.error(t);
				}

				@Override
				public void onComplete() {
					HttpResponse<Object> result = callback.apply(form);
					emitter.success(result);
				}
			});
		});

	}

	public Parameter processCompletedPart(CompletedPart completedPart, File folder) {

		String partName = completedPart.getName();

		if (completedPart instanceof CompletedFileUpload) {

			String originalFileName = ((CompletedFileUpload) completedPart).getFilename();
			File file = new File(folder, originalFileName);
			String absolutePath = file.getAbsolutePath();

			try {
				long start = System.currentTimeMillis();
				log.debug("Write data to " + absolutePath + "...");

				InputStream stream = completedPart.getInputStream();
				FileUtils.copyInputStreamToFile(stream, file);
				stream.close();

				long end = System.currentTimeMillis();
				log.debug("Data written to " + absolutePath + " in " + (end - start) + " ms.");

				return new Parameter(partName, file);
			} catch (IOException e) {
				log.error("Write data to " + absolutePath + " failed", e);
			}

		} else {

			try {
				log.debug("Write data to string...");
				InputStream stream = completedPart.getInputStream();
				String value = FileUtil.readFileAsString(stream);
				stream.close();
				log.debug("Data written to string.");
				return new Parameter(partName, value);
			} catch (IOException e) {
				log.error("Write data to string failed", e);
			}
		}

		return null;
	}

	public static class Parameter {

		private String name;

		private Object value;

		public Parameter(String name, Object value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}
	}
}
