package cloudgene.mapred.server.responses;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.CounterDao;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import io.micronaut.core.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonClassDescription
public record UserCounterResponse(String username, Map<String, Map<String, Stats>> counters) {

	@JsonClassDescription
	public record Stats(long total, double mean) {}

	@NonNull
	public static UserCounterResponse build(
			@NonNull User user,
			@NonNull Map<String, Map<String, CounterDao.Stats>> counters) {

		String username = user.getUsername();
		if (username == null || username.isBlank()) {
			username = "";
		}

		Map<String, Map<String, Stats>> processed = new LinkedHashMap<>();
		for (String application : counters.keySet()) {
			Map<String, CounterDao.Stats> appCtrs = counters.get(application);
			Map<String, Stats> inner = new LinkedHashMap<>();

			for (String counter : appCtrs.keySet()) {
				CounterDao.Stats dbStat = appCtrs.get(counter);
				Stats jsonStats = new Stats(dbStat.total(), dbStat.mean());
				inner.put(counter, jsonStats);
			}

			processed.put(application, inner);
		}

		return new UserCounterResponse(username, processed);
	}
}
