package cloudgene.mapred.jobs.engine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import cloudgene.mapred.jobs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.jobs.sdk.WorkflowStep;
import cloudgene.mapred.plugins.PluginManager;
import cloudgene.mapred.steps.ErrorStep;
import cloudgene.mapred.steps.JavaInternalStep;
import cloudgene.mapred.util.TimeUtil;
import cloudgene.mapred.wdl.WdlStep;

public class ExecutableStep {

	private static final Logger log = LoggerFactory.getLogger(ExecutableStep.class);

	private final CloudgeneContext context;

	private final CloudgeneJob job;

	private WdlStep step;

	private CloudgeneStep instance;

	private boolean killed = false;

	private long time;

	public ExecutableStep(WdlStep step, CloudgeneContext context) throws Exception {
		this.step = step;
		this.context = context;
		this.job = context.getJob();
		context.incStepCounter();
		instance();
	}

	public WdlStep getStep() {
		return step;
	}

	public void setStep(WdlStep step) {
		this.step = step;
	}

	private void instance() {
		// find step implementation
		CloudgeneStepFactory factory = CloudgeneStepFactory.getInstance();
		Class<?> myClass = factory.getClassname(step);

		// create instance
		try {
			Object object = myClass.getDeclaredConstructor().newInstance();

			if (object instanceof CloudgeneStep) {
				instance = (CloudgeneStep) object;
			} else if (object instanceof WorkflowStep) {
				instance = new JavaInternalStep((WorkflowStep) object);
			} else {
				instance = new ErrorStep("Error during initialization: class " + step.getClassname()
						+ " ( " + object.getClass().getSuperclass().getCanonicalName() + ") "
						+ " has to extend CloudgeneStep or WorkflowStep. ");

			}

			// check requirements
			PluginManager pluginManager = PluginManager.getInstance();
			for (String plugin : instance.getRequirements()) {
				if (!pluginManager.isEnabled(plugin)) {
					instance = new ErrorStep(
							"Requirements not fulfilled. This steps needs plugin '" + plugin + "'");
				}
			}

		} catch (Exception e) {
			Writer writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			e.printStackTrace(printWriter);
			String s = writer.toString();
			instance = new ErrorStep("Error during initialization: " + s);

		}

		instance.setJob(job);
	}

	public ExecutionResult run() {
		job.writeLog("------------------------------------------------------");
		job.writeLog(step.getName());
		job.writeLog("------------------------------------------------------");

		long start = System.currentTimeMillis();

		try {
			instance.setup(context);
			boolean successful = instance.run(step, context);

			if (!successful) {
				job.writeLog("  " + step.getName() + " [ERROR]");
				return killed ? ExecutionResult.CANCELED : ExecutionResult.FAILED;

			} else {
				long end = System.currentTimeMillis();
				long time = end - start;

				job.writeLog("  " + step.getName() + " [" + TimeUtil.format(time) + "]");
				setTime(time);
			}
		} catch (Exception e) {
			log.error("Running extern job failed!", e);
			return killed ? ExecutionResult.CANCELED : ExecutionResult.FAILED;
		}

		return ExecutionResult.SUCCESS;
	}

	public void kill() {
		killed = true;
		if (instance != null) {
			log.info("Get kill signal for job {}", job.getId());
			instance.kill();
		}
	}

	public void updateProgress() {
		if (instance != null) {
			instance.updateProgress();
		}
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getExecutionTime() {
		return time;
	}
}
