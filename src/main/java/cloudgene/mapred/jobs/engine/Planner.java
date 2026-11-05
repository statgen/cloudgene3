package cloudgene.mapred.jobs.engine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.util.MapValueParser;
import cloudgene.mapred.util.config.Settings;
import cloudgene.mapred.wdl.*;
import groovy.text.SimpleTemplateEngine;
import org.codehaus.groovy.control.CompilationFailedException;

/**
 * Provides {@link #evaluateWDL(WdlApp, CloudgeneContext, Settings)} to resolve
 * templated values in {@link WdlApp} manifest files.
 */
public final class Planner {

	private Planner() {}

	/**
	 * Returns a copy of {@code app}, with all its {@code ${...}} templated
	 * parameters resolved in the manifest ({@code cloudgene.yaml} or equivalent).
	 *
	 * @param app      Original (non-resolved) version of the application.
	 * @param context  Submission-specific details (inputs, outputs, job info, user
	 *                 info).
	 * @param settings Cloudgene configuration.
	 * @return Resolved copy of {@code app}, with all templated parameters in the
	 *         manifest substituted by their values.
	 */
	public static WdlApp evaluateWDL(WdlApp app, CloudgeneContext context, Settings settings)
			throws CompilationFailedException, ClassNotFoundException, IOException {
		// variable -> value bindings used to resolve parameter substitutions in the
		// app's manifest.
		Map<String, Object> bindings = new HashMap<>();

		// Add application inputs to bindings.
		for (WdlParameterInput param : app.getWorkflow().getInputs()) {
			if (param.getTypeAsEnum() == WdlParameterInputType.APP_LIST) {
				Map<String, Object> linkedApp = (Map<String, Object>) context.getData(param.getId());
				if (linkedApp != null) {
					bindings.put(param.getId(), MapValueParser.parseMap(linkedApp));
				}
			} else {
				String value = context.getInput(param.getId());
				bindings.put(param.getId(), MapValueParser.guessType(value));
			}
		}

		// Add application outputs to bindings.
		for (WdlParameterOutput param : app.getWorkflow().getOutputs()) {
			String value = context.getOutput(param.getId());
			bindings.put(param.getId(), MapValueParser.guessType(value));
		}

		// Add Cloudgene context to bindings (see Environment class).
		Map<String, String> cloudgeneBindings = settings
				.buildEnvironment() // Env vars: CLOUDGENE_SERVICE_*, CLOUDGENE_SMTP_*, CLOUDGENE_WORKSPACE_*
				.addApplication(app) // Application data: CLOUDGENE_APP_*
				.addContext(context) // User and job context: CLOUDGENE_JOB_*, CLOUDGENE_USER_*
				.toMap();
		bindings.putAll(cloudgeneBindings);

		// Load the manifest (cloudgene.yaml or equivalent) and substitute ${} params
		// using the bindings.
		File manifest = new File(app.getManifestFile());
		SimpleTemplateEngine engine = new SimpleTemplateEngine();
		String content = engine.createTemplate(manifest).make(bindings).toString();

		// Reload the app using the resolved manifest.
		WdlApp app2 = WdlReader.loadAppFromString(manifest.getAbsolutePath(), content);
		app2.getWorkflow().setInputs(app.getWorkflow().getInputs());
		app2.getWorkflow().setOutputs(app.getWorkflow().getOutputs());

		context.log("Planner: WDL evaluated.");
		return app2;
	}
}
