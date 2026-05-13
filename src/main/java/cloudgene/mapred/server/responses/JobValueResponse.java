package cloudgene.mapred.server.responses;

import cloudgene.mapred.jobs.JobValue;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import io.micronaut.core.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonClassDescription
public record JobValueResponse(String name, String value, int count) {
	@NonNull
	public static JobValueResponse build(JobValue jobValue) {
		return new JobValueResponse(
				jobValue.name(),
				jobValue.value(),
				jobValue.count());
	}

	@NonNull
	public static List<JobValueResponse> build(@NonNull List<JobValue> data) {
		return data.stream().map(JobValueResponse::build).toList();
	}

	@NonNull
	public static Map<String, List<JobValueResponse>> build(@NonNull Map<String, List<JobValue>> data) {
		Map<String, List<JobValueResponse>> response = new LinkedHashMap<>();

		for (Map.Entry<String, List<JobValue>> entry : data.entrySet()) {
			String application = entry.getKey();
			List<JobValue> values = entry.getValue();

			response.put(application, build(values));
		}

		return response;
	}
}
