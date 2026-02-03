package cloudgene.mapred.api.v2.admin;

import cloudgene.mapred.util.CloudgeneClientRestAssured;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;

@MicronautTest()
public class BannerTest {

	@Inject
	CloudgeneClientRestAssured client;

	@Test
	public void testBannerCreation() {
		// Must be logged in.
		RestAssured
				.when()
				.get("/api/v2/admin/banner")
				.then()
				.statusCode(401); // Requires authentication.

		// Non-admin user denied.
		Header publicToken = client.loginAsPublicUser();
		RestAssured
				.given()
				.header(publicToken)
				.when()
				.get("/api/v2/admin/banner")
				.then()
				.statusCode(403); // Requires higher permissions.

		// Admin user allowed, but we haven't added anything yet.
		Header adminToken = client.login("admin", "admin1978");
		RestAssured
				.given()
				.header(adminToken)
				.when()
				.get("/api/v2/admin/banner")
				.then()
				.statusCode(200)
				.body(equalTo("[]")); // Top-level element is an empty list.

		// Add a warning
		RestAssured
				.given()
				.header(adminToken)
				.formParam("type", "warning")
				.formParam("message", "Test, test's \"; 1, 2, 3!")
				.when()
				.post("/api/v2/admin/banner")
				.then()
				.statusCode(200)
				.body("type", equalTo("warning"))
				.body("message", equalTo("Test, test's \"; 1, 2, 3!"))
				.body("id", equalTo(1));

		// Now there is one element
		RestAssured
				.given()
				.header(adminToken)
				.when()
				.get("/api/v2/admin/banner")
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
				.formParam("type", "danger")
				.formParam("message", "\" or \"\"=\"")
				.when()
				.post("/api/v2/admin/banner")
				.then()
				.statusCode(200)
				.body("type", equalTo("danger"))
				.body("message", equalTo("\" or \"\"=\""))
				.body("id", equalTo(2));

		// Now there are two elements, in order of addition.
		RestAssured
				.given()
				.header(adminToken)
				.when()
				.get("/api/v2/admin/banner")
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
				.given()
				.header(adminToken)
				.when()
				.get("/api/v2/admin/banner")
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
				.formParam("type", "warning")
				.formParam("message", "!@#$%^&*")
				.when()
				.post("/api/v2/admin/banner")
				.then()
				.statusCode(200)
				.body("type", equalTo("warning"))
				.body("message", equalTo("!@#$%^&*"))
				.body("id", equalTo(3));

		// Two elements again.
		RestAssured
				.given()
				.header(adminToken)
				.when()
				.get("/api/v2/admin/banner")
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
				.formParam("id1", 2)
				.formParam("id2", 3)
				.when()
				.post("/api/v2/admin/banner/swap")
				.then()
				.statusCode(204);

		// The elements have swapped.
		RestAssured
				.given()
				.header(adminToken)
				.when()
				.get("/api/v2/admin/banner")
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
	}
}
