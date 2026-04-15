package cloudgene.mapred.plugins.nextflow;

import cloudgene.mapred.jobs.Message;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import io.micronaut.core.annotation.NonNull;
import org.codehaus.groovy.control.CompilationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class NextflowProcessRenderer {

	private static final Logger log = LoggerFactory.getLogger(NextflowProcessRenderer.class);

	private static final Map<String, Template> CACHE = new HashMap<>();

	private static final String TEMPLATES_LIST = "/templates/list.html";
	private static final String TEMPLATES_LABEL = "/templates/label.html";
	private static final String TEMPLATES_STATUS = "/templates/status.html";
	private static final String TEMPLATES_PROGRESSBAR = "/templates/progressbar.html";

	private static final String SUBMITTED = "SUBMITTED";
	private static final String RUNNING = "RUNNING";
	private static final String COMPLETED = "COMPLETED";
	private static final String FAILED = "FAILED";
	private static final String KILLED = "KILLED";

	private static final String VIEW_LABEL = "label";
	private static final String VIEW_STATUS = "status";
	private static final String VIEW_PROGRESSBAR = "progressbar";
	private static final String TRACE_STATUS = "status";

	public static void render(
			@NonNull NextflowProcessConfig config,
			@NonNull NextflowProcess process,
			@NonNull Message message) {

		String label = (config.getLabel() != null ? config.getLabel() : process.getName());

		switch (config.getView()) {
			case VIEW_PROGRESSBAR:
				render(label, TEMPLATES_PROGRESSBAR, process, message);
				break;
			case VIEW_LABEL:
				render(label, TEMPLATES_LABEL, process, message);
				break;
			case VIEW_STATUS:
				render(label, TEMPLATES_STATUS, process, message);
				break;
			default:
				render(label, TEMPLATES_LIST, process, message);
		}
	}

	private static void render(
			@NonNull String label,
			@NonNull String template,
			@NonNull NextflowProcess process,
			@NonNull Message message) {

		int running = 0;
		int completed = 0;
		int failed = 0;

		for (NextflowTask task : process.getTasks()) {
			String status = (String) task.getTrace().get(TRACE_STATUS);

			if (status.equals(RUNNING) || status.equals(SUBMITTED)) {
				running++;
			}

			if (status.equals(COMPLETED)) {
				completed++;
			}

			if (status.equals(FAILED) || status.equals(KILLED)) {
				failed++;
			}
		}

		int total = running + completed + failed;
		String id = label.trim().toLowerCase().replaceAll("[\\s_-]+", "-");

		Map<String, Object> bindings = new HashMap<>();
		bindings.put("id", id);
		bindings.put("label", label);
		bindings.put("total", total);
		bindings.put("running", running);
		bindings.put("completed", completed);
		bindings.put("failed", failed);
		bindings.put("tasks", process.getTasks());

		try {
			String text = renderTemplate(template, bindings);
			message.setMessage(text);
		} catch (Exception e) {
			message.setMessage("Template could not be rendered: " + e);
			log.error("Template could not be rendered ", e);
		}

		if (running > 0) {
			message.setType(Message.RUNNING);
		} else if (completed > 0) {
			message.setType(Message.OK);
		} else {
			message.setType(Message.ERROR);
		}
	}

	private static @NonNull String renderTemplate(@NonNull String path, @NonNull Map<String, Object> bindings)
			throws CompilationFailedException, ClassNotFoundException, IOException, URISyntaxException {

		Template template = getTemplate(path);
		String rendered = template.make(bindings).toString();
		return rendered.replace("\n", "");
	}

	private static synchronized @NonNull Template getTemplate(@NonNull String path)
			throws IOException, CompilationFailedException, ClassNotFoundException, URISyntaxException {

		Template template = CACHE.get(path);

		if (template != null) {
			return template;
		}

		URL resourceUrl = NextflowProcessRenderer.class.getResource(path);
		if (resourceUrl == null) {
			throw new FileNotFoundException("Resource not found in the classpath: " + path);
		}
		Path resourcePath = Paths.get(resourceUrl.toURI());
		String content = Files.readString(resourcePath);

		SimpleTemplateEngine engine = new SimpleTemplateEngine();
		template = engine.createTemplate(content);
		CACHE.put(path, template);

		return template;
	}
}
