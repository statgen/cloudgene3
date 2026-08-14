package cloudgene.mapred.api.v2.admin;

import cloudgene.mapred.server.responses.CounterStatisticsResponse;
import cloudgene.mapred.server.responses.JobValueResponse;
import cloudgene.mapred.test.CloudgeneClientRestAssured;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest()
public class AdminCounterValuesTest {

	@Inject
	CloudgeneClientRestAssured client;

	@BeforeEach
	public void setup() {
		Header accessToken = client.loginAsPublicUser();

		// NOTE(Marc): The original idea here was to use a BashCommandStep to add some
		// counters and values. This doesn't work because CommandOutputParser is only
		// invoked in Nextflow workflows. However, we still need to ensure *something*
		// has run so we get the default counters and values.

		// submit job
		String id = RestAssured
				.given()
				.header(accessToken)
				.multiPart("input", "input-file")
				.when()
				.post("/api/v2/jobs/submit/values-counters")
				.then()
				.statusCode(200)
				.extract().jsonPath().getString("id");

		assertTrue(id.startsWith("job-"));
	}

	@Test
	public void testGetCounters() {
		// Must be logged in to GET.
		RestAssured
				.when()
				.get("/api/v2/admin/server/counters")
				.then()
				.statusCode(401); // Requires authentication.

		// Non-admin GET denied.
		Header publicToken = client.loginAsPublicUser();
		RestAssured
				.given()
				.header(publicToken)
				.when()
				.get("/api/v2/admin/server/counters")
				.then()
				.statusCode(403); // Requires higher permissions.

		// Admin user allowed.
		Header adminToken = client.login("admin", "admin1978");

		// Get counters
		CounterStatisticsResponse payload = RestAssured
				.given()
				.header(adminToken)
				.when()
				.get("/api/v2/admin/server/counters")
				.then()
				.statusCode(200)
				.extract()
				.as(CounterStatisticsResponse.class); // Throws if the payload shape is not right.

		assertNotNull(payload);
		assertNotNull(payload.counters());
		assertTrue(payload.counters().containsKey("values-counters"));
		assertTrue(payload.counters().get("values-counters").containsKey("runs"));
	}

	@Test
	public void testGetValues() {
		// Must be logged in to GET.
		RestAssured
				.when()
				.get("/api/v2/admin/server/values")
				.then()
				.statusCode(401); // Requires authentication.

		// Non-admin GET denied.
		Header publicToken = client.loginAsPublicUser();
		RestAssured
				.given()
				.header(publicToken)
				.when()
				.get("/api/v2/admin/server/values")
				.then()
				.statusCode(403); // Requires higher permissions.

		// Admin user allowed.
		Header adminToken = client.login("admin", "admin1978");

		// Get values
		Map<String, List<JobValueResponse>> payload = RestAssured
				.given()
				.header(adminToken)
				.when()
				.get("/api/v2/admin/server/values")
				.then()
				.statusCode(200)
				.extract()
				.as(new TypeRef<>() {}); // Throws if the payload shape is not right.

		assertNotNull(payload);
		assertTrue(payload.containsKey("values-counters"));
		assertTrue(payload.get("values-counters").stream().anyMatch(x -> x.name().equals("app-version")));
	}
}
