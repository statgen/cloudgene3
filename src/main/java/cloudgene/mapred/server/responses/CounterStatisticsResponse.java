package cloudgene.mapred.server.responses;

import cloudgene.mapred.database.dao.CounterDao;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import io.micronaut.core.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonClassDescription
public record CounterStatisticsResponse(Map<String, Map<String, Stats>> counters) {

	@JsonClassDescription
	public record Stats(long count, long total, double mean) {}

	@NonNull
	public static CounterStatisticsResponse build(
			@NonNull Map<String, Map<String, CounterDao.Stats>> counters) {

		Map<String, Map<String, Stats>> processed = new LinkedHashMap<>();
		for (String application : counters.keySet()) {
			Map<String, CounterDao.Stats> appCtrs = counters.get(application);
			Map<String, Stats> inner = new LinkedHashMap<>();

			for (String counter : appCtrs.keySet()) {
				CounterDao.Stats dbStat = appCtrs.get(counter);
				Stats jsonStats = new Stats(dbStat.count(), dbStat.total(), dbStat.mean());
				inner.put(counter, jsonStats);
			}

			processed.put(application, inner);
		}

		return new CounterStatisticsResponse(processed);
	}
}
