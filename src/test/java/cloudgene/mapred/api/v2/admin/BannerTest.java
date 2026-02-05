package cloudgene.mapred.api.v2.admin;

import cloudgene.mapred.util.CloudgeneClientRestAssured;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

@MicronautTest()
public class BannerTest {

	@Inject
	CloudgeneClientRestAssured client;

	@Test
	public void testBannerCreation() {
		// Banner listing endpoint is public (no authentication required).
		RestAssured
				.when()
				.get("/api/v2/banner")
				.then()
				.statusCode(200)
				.body(equalTo("[]")); // We haven't added any elements yet.

		// Must be logged in to POST.
		RestAssured
				.given()
				.contentType(ContentType.JSON)
				.body(Map.of(
						"type", "warning",
						"message", "Test, test's \"; 1, 2, 3!"))
				.when()
				.post("/api/v2/admin/banner")
				.then()
				.statusCode(401); // Requires authentication.

		// Non-admin POST denied.
		Header publicToken = client.loginAsPublicUser();
		RestAssured
				.given()
				.header(publicToken)
				.contentType(ContentType.JSON)
				.body(Map.of(
						"type", "warning",
						"message", "Test, test's \"; 1, 2, 3!"))
				.when()
				.post("/api/v2/admin/banner")
				.then()
				.statusCode(403); // Requires higher permissions.

		// Admin user allowed.
		Header adminToken = client.login("admin", "admin1978");

		// Add a warning
		RestAssured
				.given()
				.header(adminToken)
				.contentType(ContentType.JSON)
				.body(Map.of(
						"type", "warning",
						"message", "Test, test's \"; 1, 2, 3!"))
				.when()
				.post("/api/v2/admin/banner")
				.then()
				.statusCode(200)
				.body("type", equalTo("warning"))
				.body("message", equalTo("Test, test's \"; 1, 2, 3!"))
				.body("id", equalTo(1));

		// Now there is one element (publicly visible)
		RestAssured
				.when()
				.get("/api/v2/banner")
				.then()
				.statusCode(200)
				.body("", hasSize(1))
				.body("[0].type", equalTo("warning"))
				.body("[0].message", equalTo("Test, test's \"; 1, 2, 3!"))
				.body("[0].id", equalTo(1));

		// Add a danger sign
		RestAssured
				.given()
				.header(adminToken)
				.contentType(ContentType.JSON)
				.body(Map.of(
						"type", "danger",
						"message", "\" or \"\"=\""))
				.when()
				.post("/api/v2/admin/banner")
				.then()
				.statusCode(200)
				.body("type", equalTo("danger"))
				.body("message", equalTo("\" or \"\"=\""))
				.body("id", equalTo(2));

		// Now there are two elements, in order of addition.
		RestAssured
				.when()
				.get("/api/v2/banner")
				.then()
				.statusCode(200)
				.body("", hasSize(2))
				.body("[0].type", equalTo("warning"))
				.body("[0].message", equalTo("Test, test's \"; 1, 2, 3!"))
				.body("[0].id", equalTo(1))
				.and()
				.body("[1].type", equalTo("danger"))
				.body("[1].message", equalTo("\" or \"\"=\""))
				.body("[1].id", equalTo(2));

		// We delete the first element and receive 204 NO CONTENT
		RestAssured
				.given()
				.header(adminToken)
				.when()
				.delete("/api/v2/admin/banner/1")
				.then()
				.statusCode(204);

		// Only second element remains.
		RestAssured
				.when()
				.get("/api/v2/banner")
				.then()
				.statusCode(200)
				.body("", hasSize(1))
				.body("[0].type", equalTo("danger"))
				.body("[0].message", equalTo("\" or \"\"=\""))
				.body("[0].id", equalTo(2));

		// Second deletion attempt returns 404 NOT FOUND
		RestAssured
				.given()
				.header(adminToken)
				.when()
				.delete("/api/v2/admin/banner/1")
				.then()
				.statusCode(404);

		// Add a new warning
		RestAssured
				.given()
				.header(adminToken)
				.contentType(ContentType.JSON)
				.body(Map.of(
						"type", "warning",
						"message", "!@#$%^&*"))
				.when()
				.post("/api/v2/admin/banner")
				.then()
				.statusCode(200)
				.body("type", equalTo("warning"))
				.body("message", equalTo("!@#$%^&*"))
				.body("id", equalTo(3));

		// Two elements again.
		RestAssured
				.when()
				.get("/api/v2/banner")
				.then()
				.statusCode(200)
				.body("", hasSize(2))
				.body("[0].type", equalTo("danger"))
				.body("[0].message", equalTo("\" or \"\"=\""))
				.body("[0].id", equalTo(2))
				.and()
				.body("[1].type", equalTo("warning"))
				.body("[1].message", equalTo("!@#$%^&*"))
				.body("[1].id", equalTo(3));

		// Swap elements.
		RestAssured
				.given()
				.header(adminToken)
				.contentType(ContentType.JSON)
				.body(Map.of(
						"id1", 2,
						"id2", 3))
				.when()
				.post("/api/v2/admin/banner/swap")
				.then()
				.statusCode(204);

		// The elements have swapped.
		RestAssured
				.when()
				.get("/api/v2/banner")
				.then()
				.statusCode(200)
				.body("", hasSize(2))
				.body("[0].type", equalTo("warning"))
				.body("[0].message", equalTo("!@#$%^&*"))
				.body("[0].id", equalTo(3))
				.and()
				.body("[1].type", equalTo("danger"))
				.body("[1].message", equalTo("\" or \"\"=\""))
				.body("[1].id", equalTo(2));

		// Let's update one element.
		RestAssured
				.given()
				.header(adminToken)
				.contentType(ContentType.JSON)
				.body(Map.of(
						"message", "fresh"))
				.when()
				.put("/api/v2/admin/banner/2")
				.then()
				.statusCode(204);

		// The element is updated.
		RestAssured
				.when()
				.get("/api/v2/banner")
				.then()
				.statusCode(200)
				.body("", hasSize(2))
				.body("[0].type", equalTo("warning"))
				.body("[0].message", equalTo("!@#$%^&*"))
				.body("[0].id", equalTo(3))
				.and()
				.body("[1].type", equalTo("danger"))
				.body("[1].message", equalTo("fresh")) // updated value here
				.body("[1].id", equalTo(2));
	}
}
