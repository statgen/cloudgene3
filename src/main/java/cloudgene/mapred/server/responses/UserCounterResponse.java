package cloudgene.mapred.server.responses;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.CounterDao;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import io.micronaut.core.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

@JsonClassDescription
public record UserCounterResponse(String username, Map<String, Stats> counters) {
	@JsonClassDescription
	public record Stats(long total, double mean) {}

	@NonNull
	public static UserCounterResponse build(@NonNull User user, @NonNull Map<String, CounterDao.Stats> counters) {
		String username = user.getUsername();
		if (username == null || username.isBlank()) {
			username = "";
		}

		Map<String, Stats> processed = new HashMap<>();
		for (String key : counters.keySet()) {
			CounterDao.Stats dbStat = counters.get(key);
			Stats jsonStats = new Stats(dbStat.total(), dbStat.mean());
			processed.put(key, jsonStats);
		}

		return new UserCounterResponse(username, processed);
	}
}
