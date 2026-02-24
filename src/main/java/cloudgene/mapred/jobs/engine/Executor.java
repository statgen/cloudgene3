package cloudgene.mapred.jobs.engine;

import java.util.List;

import cloudgene.mapred.jobs.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.wdl.WdlStep;

public class Executor {

	private static final Logger log = LoggerFactory.getLogger(Executor.class);

	private ExecutableStep executableNode;

	public ExecutionResult execute(List<WdlStep> steps, CloudgeneContext context) throws Exception {
		context.log("Execute " + steps.size() + " steps...");

		for (WdlStep step : steps) {
			executableNode = new ExecutableStep(step, context);
			log.info("[Job {}] Executor: execute step '{}'...", context.getJobId(), step.getName());
			ExecutionResult result = executableNode.run();
			if (result != ExecutionResult.SUCCESS) {
				return result;
			}
		}

		return ExecutionResult.SUCCESS;
	}

	public void kill() {
		if (executableNode != null) {
			executableNode.kill();
		}
	}

	public void updateProgress() {
		if (executableNode != null) {
			executableNode.updateProgress();
		}
	}

	public ExecutableStep getCurrentNode() {
		return executableNode;
	}
}
