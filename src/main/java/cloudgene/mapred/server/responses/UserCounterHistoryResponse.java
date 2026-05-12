package cloudgene.mapred.server.responses;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.CounterDao;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import io.micronaut.core.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonClassDescription
public record UserCounterHistoryResponse(@NonNull String username, @NonNull Map<String, List<HistoryEntry>> history) {
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
			@NonNull Map<String, List<CounterDao.HistoryEntry>> counterHistory) {

		String username = user.getUsername();
		if (username == null || username.isBlank()) {
			username = "";
		}

		Map<String, List<HistoryEntry>> processed = new HashMap<>();
		for (String counter : counterHistory.keySet()) {
			List<CounterDao.HistoryEntry> dbHist = counterHistory.get(counter);
			List<HistoryEntry> jsonHist = dbHist.stream().map(HistoryEntry::build).toList();
			processed.put(counter, jsonHist);
		}

		return new UserCounterHistoryResponse(username, processed);
	}
}
