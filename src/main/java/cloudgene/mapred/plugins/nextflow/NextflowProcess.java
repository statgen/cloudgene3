package cloudgene.mapred.plugins.nextflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.jobs.CloudgeneContext;
import cloudgene.mapred.jobs.Step;

public class NextflowProcess {

	private final String name;

	private final Step step;

	private final CloudgeneContext context;

	private final List<NextflowTask> tasks = new ArrayList<>();

	public NextflowProcess(CloudgeneContext context, Map<String, Object> trace, Step step) throws IOException {
		this.context = context;
		this.name = (String) trace.get("process");
		this.step = step;
		addTrace(trace);
	}

	public String getName() {
		return name;
	}

	public List<NextflowTask> getTasks() {
		return tasks;
	}

	public void addTrace(Map<String, Object> trace) throws IOException {
		int taskId = (Integer) trace.get("task_id");
		for (NextflowTask task : tasks) {
			if (task.getId() == taskId) {
				task.update(trace);
				return;
			}
		}
		NextflowTask task = new NextflowTask(context, trace, step);
		tasks.add(task);
	}
}
