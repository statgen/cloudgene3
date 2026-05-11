package cloudgene.mapred.server.responses;

import cloudgene.mapred.jobs.JobValue;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import io.micronaut.core.annotation.NonNull;

import java.util.List;

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
}
