package cloudgene.mapred.server.responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.jobs.AbstractJob;
import cloudgene.mapred.jobs.WorkflowEngine;
import cloudgene.mapred.server.Application;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import jakarta.inject.Inject;

@JsonClassDescription
public class CounterResponse {

	@Inject
	protected Application application;

	private Map<String, Long> complete = new HashMap<>();
	private Map<String, Long> queue = new HashMap<>();
	private int users = 0;

	public Map<String, Long> getComplete() {
		return complete;
	}

	public void setComplete(Map<String, Long> complete) {
		this.complete = complete;
	}

	public int getUsers() {
		return users;
	}

	public void setUsers(int users) {
		this.users = users;
	}

	public void setQueue(Map<String, Long> queue) {
		this.queue = queue;
	}

	public Map<String, Long> getQueue() {
		return queue;
	}

	public static CounterResponse build(WorkflowEngine workflowEngine, List<String> counters) {
		CounterResponse response = new CounterResponse();

		response.complete = workflowEngine.getCounters(AbstractJob.STATE_SUCCESS, counters);
		response.queue.put("size", (long) workflowEngine.getSize());

		return response;
	}
}
