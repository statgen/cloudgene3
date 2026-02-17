package cloudgene.mapred.jobs;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import cloudgene.mapred.apps.Application;
import cloudgene.mapred.apps.ApplicationRepository;
import cloudgene.mapred.core.User;
import cloudgene.mapred.jobs.sdk.WorkflowContext;
import cloudgene.mapred.util.MailUtil;
import cloudgene.mapred.util.Settings;
import cloudgene.mapred.wdl.WdlParameterInputType;
import genepi.io.FileUtil;

public class CloudgeneContext extends WorkflowContext {

	private final String localTemp;

	private final String localOutput;

	private final String workingDirectory;

	private final Settings settings;

	private final User user;

	private final Map<String, CloudgeneParameterInput> inputParameters;

	private final Map<String, CloudgeneParameterOutput> outputParameters;

	private final Map<String, Long> counters = new HashMap<>();

	private final Map<String, String> values = new HashMap<>();

	private final Map<String, Boolean> submitCounters = new HashMap<>();

	private final Map<String, Boolean> submitValues = new HashMap<>();

	private final CloudgeneJob job;

	private final Map<String, Object> data = new HashMap<>();

	private Map<String, Object> config;

	private int stepCounter = 0;

	public CloudgeneContext(CloudgeneJob job) {

		this.workingDirectory = job.getWorkingDirectory();
		this.job = job;

		this.user = job.getUser();

		setData("cloudgene.user.mail", user.getMail());
		setData("cloudgene.user.name", user.getFullName());

		localOutput = new File(job.getLocalWorkspace()).getAbsolutePath();
		localTemp = new File(FileUtil.path(job.getLocalWorkspace(), "temp")).getAbsolutePath();

		inputParameters = new HashMap<>();
		for (CloudgeneParameterInput param : job.getInputParams()) {
			inputParameters.put(param.getName(), param);
		}

		outputParameters = new HashMap<>();
		for (CloudgeneParameterOutput param : job.getOutputParams()) {
			outputParameters.put(param.getName(), param);
		}

		settings = job.getSettings();
	}

	public boolean resolveAppLinks() throws IOException {

		Settings settings = getSettings();
		ApplicationRepository repository = settings.getApplicationRepository();

		// resolve application links
		for (CloudgeneParameterInput input : inputParameters.values()) {
			if (input.getType() != WdlParameterInputType.APP_LIST) {
				continue;
			}
			String value = input.getValue();
			String linkedAppId = value;
			if (value.startsWith("apps@")) {
				linkedAppId = value.replaceAll("apps@", "");
			}

			if (value.isEmpty()) {
				continue;
			}
			Application linkedApp = repository.getByIdAndUser(linkedAppId, getUser());
			if (linkedApp == null) {
				throw new IOException("Application " + linkedAppId + " is not installed or wrong permissions.");
			}
			// update environment variables
			Environment environment = settings.buildEnvironment()
					.addApplication(linkedApp.getWdlApp())
					.addContext(this);

			Map<String, Object> properties = linkedApp.getWdlApp().getProperties();

			for (String property : properties.keySet()) {
				Object propertyValue = properties.get(property);
				if (propertyValue instanceof String) {
					propertyValue = environment.resolve(propertyValue.toString());
				}
				properties.put(property, propertyValue);
			}

			setData(input.getName(), properties);
		}

		return true;
	}

	@Override
	public String getInput(String param) {
		CloudgeneParameterInput input = inputParameters.get(param);
		if (input != null) {
			return input.getValue();
		} else {
			return null;
		}
	}

	@Override
	public String getJobId() {
		return job.getId();
	}

	public String getPublicJobId() {
		return job.getPublicJobId();
	}

	@Override
	public String getOutput(String param) {
		CloudgeneParameterOutput output = outputParameters.get(param);
		if (output != null) {
			return output.getValue();
		} else {
			return null;
		}
	}

	@Override
	public String get(String param) {
		String result = getInput(param);
		if (result == null) {
			return getOutput(param);
		} else {
			return result;
		}
	}

	public Settings getSettings() {
		return settings;
	}

	@Override
	public String getLocalTemp() {
		return localTemp;
	}

	public String getLocalOutput() {
		return localOutput;
	}

	@Override
	public void println(String line) {
		job.writeOutputln(line);
	}

	public void log(String line, Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		e.printStackTrace(pw);
		String sStackTrace = sw.toString();

		job.writeLog(line + "\n" + sStackTrace);
	}

	@Override
	public void log(String line) {
		job.writeLog(line);
	}

	public CloudgeneJob getJob() {
		return job;
	}

	@Override
	public String getJobName() {
		return job.getName();
	}

	@Override
	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public User getUser() {
		return user;
	}

	@Override
	public boolean sendMail(String subject, String body) throws Exception {
		return sendMail(user.getMail(), subject, body);
	}

	@Override
	public boolean sendMail(String to, String subject, String body) throws Exception {
		Settings settings = getSettings();

		if (settings.getMail() != null) {
			MailUtil.send(
					settings.getMail().get("smtp"),
					settings.getMail().get("port"),
					settings.getMail().get("user"),
					settings.getMail().get("password"),
					settings.getMail().get("name"),
					to,
					"[" + settings.getName() + "] " + subject,
					body);
		}

		return true;
	}

	@Override
	public Set<String> getInputs() {
		return inputParameters.keySet();
	}

	/**
	 * @return The names of all available outputs.
	 */
	public Set<String> getOutputs() {
		return outputParameters.keySet();
	}

	@Override
	public void incCounter(String name, long value) {
		Long oldCount = counters.get(name);
		if (oldCount == null) {
			oldCount = 0L;
		}
		Long newCount = oldCount + value;

		log(String.format("Increment counter '%s': %,d + %,d = %,d", name, oldCount, value, newCount));
		counters.put(name, newCount);
	}

	@Override
	public void submitCounter(String name) {
		log("Submit counter: '" + name + "'");
		submitCounters.put(name, true);
	}

	public Map<String, Long> getSubmittedCounters() {
		Map<String, Long> result = new HashMap<>();
		for (String counter : submitCounters.keySet()) {
			result.put(counter, counters.get(counter));
		}
		return result;
	}

	@Override
	public Map<String, Long> getCounters() {
		return counters;
	}

	public void setValue(String name, String value) {
		log("Set value " + name + " to " + value);
		values.put(name, value);
	}

	public void submitValue(String name) {
		log("Submit value " + name);
		submitValues.put(name, true);
	}

	public Map<String, String> getSubmittedValues() {
		Map<String, String> result = new HashMap<>();
		for (String name : submitValues.keySet()) {
			result.put(name, values.get(name));
		}
		return result;
	}

	public Map<String, String> getValues() {
		return values;
	}

	public Step getCurrentStep() {
		if (job.getSteps().isEmpty()) {
			return createStep("Steps");
		}
		return job.getSteps().getLast();
	}

	@Override
	public void message(String message, int type) {
		message(getCurrentStep(), message, type);
	}

	public void message(Step step, String message, int type) {
		Message status = new Message(step, type, message);

		List<Message> logs = step.getLogMessages();
		if (logs == null) {
			logs = new ArrayList<>();
			step.setLogMessages(logs);
		}
		logs.add(status);

	}

	public Message createTask(String name) {
		return createTask(getCurrentStep(), name);
	}

	public Message createTask(Step step, String name) {
		Message status = new Message(step, Message.RUNNING, name);

		List<Message> logs = step.getLogMessages();
		if (logs == null) {
			logs = new ArrayList<>();
			step.setLogMessages(logs);
		}
		logs.add(status);
		return status;
	}

	@Override
	public void beginTask(String name) {
		beginTask(getCurrentStep(), name);
	}

	public void beginTask(Step step, String name) {
		Message status = new Message(step, Message.RUNNING, name);

		List<Message> logs = step.getLogMessages();
		if (logs == null) {
			logs = new ArrayList<>();
			step.setLogMessages(logs);
		}
		logs.add(status);
	}

	@Override
	public void updateTask(String message, int type) {
		updateTask(getCurrentStep(), message, type);
	}

	public void updateTask(Step step, String message, int type) {
		Message status = step.getLogMessages().getLast();
		status.setType(type);
		status.setMessage(message);
	}

	@Override
	public void endTask(String message, int type) {
		endTask(getCurrentStep(), message, type);
	}

	public void endTask(Step step, String message, int type) {
		Message status = step.getLogMessages().getLast();
		status.setType(type);
		status.setMessage(message);
	}

	public Step createStep(String name) {
		Step outputStep = new Step();
		outputStep.setJob(job);
		outputStep.setName(name);
		job.getSteps().add(outputStep);
		return outputStep;
	}

	@Override
	public Object getData(String key) {
		return data.get(key);
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(String key, Object object) {
		data.put(key, object);
	}

	@Override
	public String getConfig(String param) {
		if (config != null) {
			Object value = config.get(param);
			return value != null ? value.toString() : null;
		} else {
			return null;
		}
	}

	@Override
	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	public void incStepCounter() {
		stepCounter++;
	}

	public int getStepCounter() {
		return stepCounter;
	}
}
