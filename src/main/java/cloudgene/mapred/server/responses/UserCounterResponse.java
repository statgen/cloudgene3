package cloudgene.mapred.server.responses;

import cloudgene.mapred.core.User;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.HashMap;
import java.util.Map;

@JsonClassDescription
public class UserCounterResponse {

	private String username = "";
	private Map<String, Long> counters = new HashMap<>();

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Map<String, Long> getCounters() {
		return counters;
	}

	public void setCounters(Map<String, Long> counters) {
		this.counters = counters;
	}

	public static UserCounterResponse build(User user, Map<String, Long> counters) {
		UserCounterResponse response = new UserCounterResponse();

		response.setUsername(user.getUsername());
		response.setCounters(counters);

		return response;
	}
}
