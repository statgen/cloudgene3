package cloudgene.mapred.steps;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.CloudgeneStep;
import cloudgene.mapred.wdl.WdlStep;

public class ErrorStep extends CloudgeneStep {

	private final String errorMessage;

	public ErrorStep(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public boolean run(WdlStep step, CloudgeneContext context) {
		context.createStep(step.getName());
		context.error(errorMessage);
		return false;
	}
}
