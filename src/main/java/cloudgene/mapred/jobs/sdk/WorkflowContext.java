package cloudgene.mapred.jobs.sdk;

import java.util.Map;
import java.util.Set;

import cloudgene.mapred.jobs.workspace.IWorkspace;

public abstract class WorkflowContext {

	public static final int OK = 0;

	public static final int ERROR = 1;

	public static final int WARNING = 2;

	public static final int RUNNING = 3;

	public abstract String getInput(String param);

	public abstract String getJobId();

	public abstract String getOutput(String param);

	/**
	 * If an input with the given name exists, returns its value.
	 * Otherwise, if an output with the given name exists, returns its value.
	 * If no input or output parameter with the given name exists, returns {@code null}.
	 *
	 * @param param Name of the input or output parameter to retrieve.
	 * @return The value of the requested parameter, or {@code null}.
	 */
	public abstract String get(String param);

	public abstract void println(String line);

	public abstract void log(String line);

	public abstract String getWorkingDirectory();

	public abstract boolean sendMail(String subject, String body) throws Exception;

	public abstract boolean sendMail(String to, String subject, String body) throws Exception;

	/**
	 * @return The names of all available inputs.
	 */
	public abstract Set<String> getInputs();

	public abstract void incCounter(String name, long value);

	public abstract void submitCounter(String name);

	public abstract Map<String, Long> getCounters();

	public abstract Object getData(String key);

	public abstract String getJobName();

	public abstract String getLocalTemp();

	public abstract String getConfig(String param);

	public abstract void setConfig(Map<String, Object> config);

	public abstract void message(String message, int type);

	private IWorkspace workspace;

	public void setWorkspace(IWorkspace workspace) {
		this.workspace = workspace;
	}

	public IWorkspace getWorkspace() {
		return workspace;
	}

	public void ok(String message) {
		message(message, OK);
	}

	public void error(String message) {
		message(message, ERROR);
	}

	public void warning(String message) {
		message(message, WARNING);
	}

	public abstract void beginTask(String name);

	public abstract void updateTask(String name, int type);

	public abstract void endTask(String message, int type);
}
