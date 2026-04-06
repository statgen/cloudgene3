package cloudgene.mapred.jobs;

import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.util.FormUtil;
import cloudgene.mapred.wdl.WdlApp;
import cloudgene.mapred.wdl.WdlParameterInput;
import cloudgene.mapred.wdl.WdlParameterInputType;
import genepi.io.FileUtil;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Inject;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobParameterParser {

	private static final Logger log = LoggerFactory.getLogger(JobParameterParser.class);

	private static final String PARAM_JOB_NAME = "job-name";

	@Inject
	protected Application application;

	public static Map<String, String> parse(
			@NonNull List<FormUtil.Parameter> form,
			@NonNull WdlApp app,
			@NonNull IWorkspace workspace) throws Exception {

		Map<String, String> props = new HashMap<>();
		Map<String, String> params = new HashMap<>();

		// uploaded files
		for (FormUtil.Parameter formParam : form) {
			String name = formParam.getName();
			Object value = formParam.getValue();

			// remove upload identification!
			String key = StringEscapeUtils.escapeHtml4(name);
			if (key.startsWith("input-")) {
				key = key.replace("input-", "");
			}

			log.debug("Process parameter {}...", key);

			if (key.equals(PARAM_JOB_NAME) || key.endsWith("-pattern")) {
				String cleanedValue = StringEscapeUtils.escapeHtml4(value.toString());
				props.put(key, cleanedValue);
				log.debug("Parameter {} ignored.", key);
				continue;
			}

			WdlParameterInput input = getInputParamByName(app, key);
			if (input == null) {
				log.error("Parameter {} not found in wdl application.", key);
				throw new Exception("Parameter '" + key + "' not found.");
			}

			if (value instanceof File inputFile) {
				log.debug("Parameter {} is a file.", key);

				try {
					// copy to workspace in input directory
					long start = System.currentTimeMillis();
					log.debug("Upload file {} to workspace...", inputFile.getAbsolutePath());
					String target = workspace.uploadInput(key, inputFile);
					log.debug("File {} uploaded in {} ms", inputFile.getAbsolutePath(),
							System.currentTimeMillis() - start);

					if (input.isFolder()) {
						props.put(key, workspace.getParent(target));
					} else {
						// file
						props.put(key, target);
					}
				} finally {
					FileUtil.deleteFile(inputFile.getAbsolutePath());
				}

				log.debug("Parameter {} processed.", key);
			} else {
				log.debug("Parameter {} is a value parameter.", key);

				String cleanedValue = StringEscapeUtils.escapeHtml4(value.toString());

				if (input.getWriteFile() != null && !input.getWriteFile().trim().isEmpty()) {
					File file = Files.createTempFile("upload_", input.getWriteFile()).toFile();
					file.deleteOnExit();

					try {
						FileUtil.writeStringBufferToFile(file.getAbsolutePath(), new StringBuffer(cleanedValue));
						String target = workspace.uploadInput(key, file);
						cleanedValue = target;
						log.debug("Parameter {} value written to file '{}\"", key, target);
					} finally {
						file.delete();
					}
				}

				if (!props.containsKey(key)) {
					// don't override uploaded files
					props.put(key, cleanedValue);
				}
			}
		}

		for (WdlParameterInput input : app.getWorkflow().getInputs()) {
			if (!params.containsKey(input.getId())) {
				if (props.containsKey(input.getId())) {
					if (input.isFolder() && input.getPattern() != null && !input.getPattern().isEmpty()) {
						String pattern = props.get(input.getId() + "-pattern");
						if (pattern == null) {
							pattern = input.getPattern();
						}
						String value = props.get(input.getId());
						if (!value.endsWith("/")) {
							value = value + "/";
						}
						params.put(input.getId(), value + pattern);
					} else {
						if (input.getTypeAsEnum() == WdlParameterInputType.CHECKBOX) {
							params.put(input.getId(), input.getValues().get("true"));
						} else {
							params.put(input.getId(), props.get(input.getId()));
						}
					}
				} else {
					// ignore invisible input parameters
					if (input.getTypeAsEnum() == WdlParameterInputType.CHECKBOX && input.isVisible()) {
						params.put(input.getId(), input.getValues().get("false"));
					}
				}
			}
		}

		params.put(PARAM_JOB_NAME, props.get(PARAM_JOB_NAME));

		return params;
	}

	private static WdlParameterInput getInputParamByName(@NonNull WdlApp app, String name) {
		for (WdlParameterInput input : app.getWorkflow().getInputs()) {
			if (input.getId().equals(name)) {
				return input;
			}
		}

		return null;
	}
}
