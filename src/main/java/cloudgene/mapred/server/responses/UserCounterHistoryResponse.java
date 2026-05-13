package cloudgene.mapred.server.responses;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.CounterDao;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import io.micronaut.core.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonClassDescription
public record UserCounterHistoryResponse(
		@NonNull String username,
		@NonNull Map<String, Map<String, List<HistoryEntry>>> history) {

	@JsonClassDescription
	public record HistoryEntry(long timestamp, long value) {
		@NonNull
		public static HistoryEntry build(@NonNull CounterDao.HistoryEntry dbEntry) {
			return new HistoryEntry(dbEntry.time().toEpochMilli(), dbEntry.value());
		}
	}

	@NonNull
	public static UserCounterHistoryResponse build(
			@NonNull User user,
			@NonNull Map<String, Map<String, List<CounterDao.HistoryEntry>>> counterHistory) {

		String username = user.getUsername();
		if (username == null || username.isBlank()) {
			username = "";
		}

		Map<String, Map<String, List<HistoryEntry>>> processed = new HashMap<>();
		for (String application : counterHistory.keySet()) {
			Map<String, List<CounterDao.HistoryEntry>> appHist = counterHistory.get(application);
			Map<String, List<HistoryEntry>> inner = new HashMap<>();

			for (String counter : appHist.keySet()) {
				List<CounterDao.HistoryEntry> dbHist = appHist.get(counter);
				List<HistoryEntry> jsonHist = dbHist.stream().map(HistoryEntry::build).toList();

				inner.put(counter, jsonHist);
			}

			processed.put(application, inner);
		}

		return new UserCounterHistoryResponse(username, processed);
	}
}
