package cloudgene.mapred.jobs.engine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.util.MapValueParser;
import cloudgene.mapred.util.config.Settings;
import cloudgene.mapred.wdl.*;
import groovy.text.SimpleTemplateEngine;

public class Planner {

	public WdlApp evaluateWDL(WdlApp app, CloudgeneContext context, Settings settings) throws Exception {

		Map<String, Object> context2 = new HashMap<String, Object>();

		// add input values to context
		for (WdlParameterInput param : app.getWorkflow().getInputs()) {
			if (param.getTypeAsEnum() == WdlParameterInputType.APP_LIST) {
				Map<String, Object> linkedApp = (Map<String, Object>) context.getData(param.getId());
				if (linkedApp != null) {
					context2.put(param.getId(), MapValueParser.parseMap(linkedApp));
				}
			} else {
				String value = context.getInput(param.getId());
				context2.put(param.getId(), MapValueParser.guessType(value));
			}
		}

		// add output values to context
		for (WdlParameterOutput param : app.getWorkflow().getOutputs()) {
			String value = context.getOutput(param.getId());
			context2.put(param.getId(), MapValueParser.guessType(value));
		}

		context2.putAll(settings.buildEnvironment().addApplication(app).addContext(context).toMap());

		File manifest = new File(app.getManifestFile());

		SimpleTemplateEngine engine = new SimpleTemplateEngine();
		String content = engine.createTemplate(manifest).make(context2).toString();

		WdlApp app2 = WdlReader.loadAppFromString(manifest.getAbsolutePath(), content);

		app2.getWorkflow().setInputs(app.getWorkflow().getInputs());
		app2.getWorkflow().setOutputs(app.getWorkflow().getOutputs());

		context.log("Planner: WDL evaluated.");

		return app2;
	}

}
