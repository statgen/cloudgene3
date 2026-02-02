package cloudgene.mapred.server.responses;

import java.util.ArrayList;
import java.util.List;

import cloudgene.mapred.jobs.Step;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonClassDescription
public class StepResponse {

	private int id;
	private String name;
	private boolean empty = true;

	@JsonProperty("logMessages")
	private List<MessageResponse> messages;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEmpty() {
		return empty;
	}

	public void setEmpty(boolean empty) {
		this.empty = empty;
	}

	public static StepResponse build(Step step) {
		StepResponse response = new StepResponse();

		response.setId(step.getId());
		response.setName(step.getName());
		response.setEmpty(step.getLogMessages() == null || step.getLogMessages().isEmpty());

		List<MessageResponse> responses = MessageResponse.build(step.getLogMessages());
		response.setMessages(responses);

		return response;
	}

	public static List<StepResponse> build(List<Step> steps) {
		List<StepResponse> response = new ArrayList<>();

		for (Step step : steps) {
			response.add(StepResponse.build(step));
		}

		return response;
	}

	public List<MessageResponse> getMessages() {
		return messages;
	}

	public void setMessages(List<MessageResponse> messages) {
		this.messages = messages;
	}
}
