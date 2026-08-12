package cloudgene.mapred.server.responses;

import cloudgene.mapred.database.dao.CounterHistoryDao;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import io.micronaut.core.annotation.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StatisticsResponse {

	private StatisticsResponse() {}

	@JsonClassDescription
	public record Entry(Instant timestamp, @JsonAnyGetter Map<String, Long> counters) {
		public static Entry of(CounterHistoryDao.Entry entry) {
			return new Entry(entry.timestamp(), entry.counters());
		}
	}

	private static final String[] IMPORTANT_COUNTERS = new String[] { "runningJobs", "waitingJobs", "completeJobs", "users" };

	public static List<Entry> build(@NonNull List<CounterHistoryDao.Entry> stats) {
		List<Entry> entries = new ArrayList<>();

		if (stats.size() <= 2) {
			for (CounterHistoryDao.Entry entry : stats) {
				entries.add(Entry.of(entry));
			}
			return entries;
		}

		entries.add(Entry.of(stats.getFirst()));

		// minimize points
		for (int i = 1; i < stats.size() - 1; i++) {
			CounterHistoryDao.Entry prev = stats.get(i - 1);
			CounterHistoryDao.Entry current = stats.get(i);
			CounterHistoryDao.Entry next = stats.get(i + 1);

			if (!equals(prev, current) && !equals(current, next)) {
				entries.add(Entry.of(current));
			}
		}

		entries.add(Entry.of(stats.getLast()));

		return entries;
	}

	private static boolean equals(CounterHistoryDao.Entry a, CounterHistoryDao.Entry b) {
		for (String key : IMPORTANT_COUNTERS) {
			Long ca = a.counters().get(key);
			Long cb = b.counters().get(key);

			if (ca == null || cb == null) {
				return false;
			}

			if (!ca.equals(cb)) {
				return false;
			}
		}

		return true;
	}
}
