package cloudgene.mapred.jobs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.queue.PriorityRunnable;
import cloudgene.mapred.jobs.workspace.IWorkspace;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.Settings;
import genepi.io.FileUtil;

abstract public class AbstractJob extends PriorityRunnable {

	public static final String JOB_LOG = "job.txt";

	public static final String JOB_OUT = "std.out";

	private static final Logger log = LoggerFactory.getLogger(AbstractJob.class);

	private final DateFormat formatter = new SimpleDateFormat("yy/MM/dd HH:mm:ss");

	// states

	public static final int STATE_WAITING = 1;

	public static final int STATE_RUNNING = 2;

	public static final int STATE_EXPORTING = 3;

	public static final int STATE_SUCCESS = 4;

	public static final int STATE_FAILED = 5;

	public static final int STATE_CANCELED = 6;

	public static final int STATE_RETIRED = 7;

	public static final int STATE_SUCESS_AND_NOTIFICATION_SEND = 8;

	public static final int STATE_FAILED_AND_NOTIFICATION_SEND = 9;

	public static final int STATE_DEAD = -1;

	public static final int STATE_DELETED = 10;

	// properties

	private String id;

	private int state = STATE_WAITING;

	private long startTime = 0;

	private long endTime = 0;

	private long submittedOn = 0;

	private String name;

	private User user;

	private String userAgent = "";

	private long deletedOn = -1;

	private String application;

	private String applicationId;

	private String error = "";

	private int positionInQueue = -1;

	protected List<CloudgeneParameterInput> inputParams = new ArrayList<>();

	protected List<CloudgeneParameterOutput> outputParams = new ArrayList<>();

	protected Map<String, CloudgeneParameterOutput> outputParamsIndex = new HashMap<String, CloudgeneParameterOutput>();

	protected CloudgeneParameterOutput logOutput = null;

	protected List<Step> steps = new ArrayList<>();

	protected BufferedOutputStream stdOutStream;

	private BufferedOutputStream logStream;

	protected CloudgeneContext context;

	private Settings settings;

	private String localWorkspace;

	private boolean canceled = false;

	protected IWorkspace workspace;

	private String workspaceSize = null;

	private String publicJobId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		String salt = RandomStringUtils.secure().next(500);
		this.publicJobId = HashUtil.getSha256(id + salt);
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public void setSubmittedOn(long submittedOn) {
		this.submittedOn = submittedOn;
	}

	public long getSubmittedOn() {
		return submittedOn;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public void setDeletedOn(long deletedOn) {
		this.deletedOn = deletedOn;
	}

	public long getDeletedOn() {
		return deletedOn;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public List<CloudgeneParameterInput> getInputParams() {
		return inputParams;
	}

	public void setInputParams(List<CloudgeneParameterInput> inputParams) {
		this.inputParams = inputParams;
	}

	public List<CloudgeneParameterOutput> getOutputParams() {
		return outputParams;
	}

	public void setOutputParams(List<CloudgeneParameterOutput> outputParams) {
		this.outputParams = outputParams;
		for (CloudgeneParameterOutput param : outputParams) {
			outputParamsIndex.put(param.getName(), param);
		}
	}

	public CloudgeneParameterOutput getLogOutput() {
		return logOutput;
	}

	public void setPositionInQueue(int positionInQueue) {
		this.positionInQueue = positionInQueue;
	}

	public int getPositionInQueue() {
		return positionInQueue;
	}

	public void setWorkspaceSize(String workspaceSize) {
		this.workspaceSize = workspaceSize;
	}

	public String getWorkspaceSize() {
		return workspaceSize;
	}

	public boolean afterSubmission() {
		try {

			initStdOutFiles();

			setup();
			return true;

		} catch (Exception e1) {

			log.error("Job " + getId() + ": initialization failed.", e1);
			writeLog("Initialization failed: " + e1.getLocalizedMessage());
			setState(STATE_FAILED);
			return false;

		}
	}

	@Override
	public void run() {

		if (canceled) {
			return;
		}

		log.info("[Job {}] Setup job...", getId());
		setState(AbstractJob.STATE_RUNNING);
		setStartTime(System.currentTimeMillis());

		log.info("[Job {}] Running job...", getId());
		setStartTime(System.currentTimeMillis());

		try {
			writeLog("Details:");
			writeLog("  Name: " + getName());
			writeLog("  Job-Id: " + getId());
			writeLog("  Submitted On: " + new Date(getSubmittedOn()).toString());
			writeLog("  Submitted By: " + getUser().getUsername());
			writeLog("  User-Agent: " + getUserAgent());
			writeLog("  Inputs:");
			for (CloudgeneParameterInput parameter : inputParams) {
				writeLog("    " + parameter.getDescription() + ": " + context.get(parameter.getName()));
			}

			// TODO: check if all input parameters are set

			writeLog("  Outputs:");
			for (CloudgeneParameterOutput parameter : outputParams) {
				writeLog("    " + parameter.getDescription() + ": " + context.get(parameter.getName()));
			}

			writeLog("Executing Job....");

			boolean successful = execute();

			if (successful) {

				log.info("[Job {}] Execution successful.", getId());

				writeLog("Job Execution successful.");
				writeLog("Exporting Data...");

				setState(AbstractJob.STATE_EXPORTING);

				try {

					boolean successfulAfter = after();

					if (successfulAfter) {

						setState(AbstractJob.STATE_SUCCESS);
						log.info("[Job {}]  data export successful.", getId());
						writeLog("Data Export successful.");

					} else {

						setState(AbstractJob.STATE_FAILED);
						log.error("[Job {}]  data export failed.", getId());
						writeLog("Data Export failed.");

					}

				} catch (Error | Exception e) {

					Writer writer = new StringWriter();
					PrintWriter printWriter = new PrintWriter(writer);
					e.printStackTrace(printWriter);
					String s = writer.toString();

					setState(AbstractJob.STATE_FAILED);
					log.error("[Job {}]  data export failed.", getId(), e);
					writeLog("Data Export failed: " + e.getLocalizedMessage() + "\n" + s);

				}

			} else {

				setState(AbstractJob.STATE_FAILED);
				log.error("[Job {}] Execution failed. {}", getId(), getError());
				writeLog("Job Execution failed: " + getError());

			}

			writeLog("Cleaning up...");
			if (getState() == AbstractJob.STATE_FAILED || getState() == AbstractJob.STATE_CANCELED) {
				onFailure();
			} else {
				cleanUp();
			}
			log.info("[Job {}]cleanup successful.", getId());
			writeLog("Cleanup successful.");

			if (canceled) {
				setState(AbstractJob.STATE_CANCELED);
			}

		} catch (Exception | Error e) {

			setState(AbstractJob.STATE_FAILED);
			log.error("[Job {}]: initialization failed.", getId(), e);

			Writer writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			e.printStackTrace(printWriter);
			String s = writer.toString();

			writeLog("Initialization failed: " + e.getLocalizedMessage() + "\n" + s);

			writeLog("Cleaning up...");
			onFailure();
			log.info("[Job {}]: cleanup successful.", getId());
			writeLog("Cleanup successful.");

		}

		closeStdOutFiles();
		setEndTime(System.currentTimeMillis());
	}

	public void cancel() {

		writeLog("Canceled by user.");
		log.info("[Job {}]: canceld by user.", getId());

		canceled = true;
		setEndTime(System.currentTimeMillis());
		setState(AbstractJob.STATE_CANCELED);

	}

	private void initStdOutFiles() throws FileNotFoundException {
		stdOutStream = new BufferedOutputStream(new FileOutputStream(FileUtil.path(localWorkspace, JOB_OUT)));
		logStream = new BufferedOutputStream(new FileOutputStream(FileUtil.path(localWorkspace, JOB_LOG)));
	}

	private void closeStdOutFiles() {
		try {
			stdOutStream.close();
			logStream.close();

			// stage files to workspace
			workspace.uploadLog(new File(FileUtil.path(localWorkspace, JOB_OUT)));
			workspace.uploadLog(new File(FileUtil.path(localWorkspace, JOB_LOG)));

			FileUtil.deleteFile(FileUtil.path(localWorkspace, JOB_OUT));
			FileUtil.deleteFile(FileUtil.path(localWorkspace, JOB_LOG));

		} catch (IOException e) {
			log.error("[Job {}]: Staging log files failed.", getId(), e);
		}
	}

	public void writeOutput(String line) {
		try {
			if (stdOutStream != null && line != null) {
				stdOutStream.write(line.getBytes("UTF-8"));
				stdOutStream.flush();

			}
		} catch (IOException e) {
			log.error("[Job {}]: Write output failed.", getId(), e);
		}
	}

	public void writeOutputln(String line) {
		writeOutput(line + "\n");
	}

	public void writeLog(String line) {
		try {
			if (logStream == null) {
				initStdOutFiles();
			}

			logStream.write((formatter.format(new Date()) + " ").getBytes());
			logStream.write(line.getBytes("UTF-8"));
			logStream.write("\n".getBytes("UTF-8"));
			logStream.flush();

		} catch (IOException e) {
			log.error("[Job {}]: Write output failed.", getId(), e);
		}
	}

	public List<Step> getSteps() {
		return steps;
	}

	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}

	public CloudgeneContext getContext() {
		return context;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof AbstractJob)) {
			return false;
		}

		return ((AbstractJob) obj).getId().equals(id);
	}

	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	public String getLocalWorkspace() {
		return localWorkspace;
	}

	public void setLocalWorkspace(String localWorkspace) {
		this.localWorkspace = localWorkspace;
	}

	public void setWorkspace(IWorkspace workspace) {
		this.workspace = workspace;
	}

	public IWorkspace getWorkspace() {
		return workspace;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getApplication() {
		return application;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public boolean isRunning() {
		return state == STATE_EXPORTING || state == STATE_RUNNING || state == STATE_WAITING;
	}

	public Download findDownloadByHash(String hash) {
		for (CloudgeneParameterOutput param : getOutputParams()) {
			if (param.getFiles() == null) {
				continue;
			}
			for (Download download : param.getFiles()) {
				if (download.getHash().equals(hash)) {
					return download;
				}
			}
		}
		return null;
	}

	abstract public boolean execute();

	abstract public boolean setup() throws Exception;

	abstract public boolean after();

	abstract public boolean onFailure();

	abstract public boolean cleanUp();

	public void kill() {
	}

	public String getPublicJobId() {
		return publicJobId;
	}

	public String getLog(String name) {
		String logFilename = FileUtil.path(settings.getLocalWorkspace(), getId(), name);
		return FileUtil.readFileAsString(logFilename);
	}
}
