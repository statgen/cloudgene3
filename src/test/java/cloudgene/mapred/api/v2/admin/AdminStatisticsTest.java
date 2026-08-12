package cloudgene.mapred.api.v2.admin;

import cloudgene.mapred.test.CloudgeneClientRestAssured;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest()
public class AdminStatisticsTest {

	@Inject
	CloudgeneClientRestAssured client;

	@Test
	public void testGetStatistics() {
		// Must be logged in to GET.
		RestAssured
				.when()
				.get("/api/v2/admin/server/statistics")
				.then()
				.statusCode(401); // Requires authentication.

		// Non-admin GET denied.
		Header publicToken = client.loginAsPublicUser();
		RestAssured
				.given()
				.header(publicToken)
				.when()
				.get("/api/v2/admin/server/statistics")
				.then()
				.statusCode(403); // Requires higher permissions.

		// Admin user allowed.
		Header adminToken = client.login("admin", "admin1978");

		// Add a warning
		List<Map<String, Object>> payload = RestAssured
				.given()
				.header(adminToken)
				.when()
				.get("/api/v2/admin/server/statistics")
				.then()
				.statusCode(200)
				.extract()
				.as(new TypeRef<>() {});

		assertFalse(payload.isEmpty());
		Map<String, Object> entry = payload.getFirst();
		assertTrue(entry.size() >= 2);
		assertTrue(entry.containsKey("timestamp"));
		for (String key : entry.keySet()) {
			Object value = entry.get(key);
			if (Objects.equals(key, "timestamp")) {
				assertInstanceOf(String.class, value);
				// Can be parsed as an ISO 8601 time string.
				assertDoesNotThrow(() -> Instant.parse((String) value));
			} else {
				// In back-end these are Long, but RestAssured uses Integer when parsing.
				assertInstanceOf(Integer.class, value);
			}
		}
	}
}
