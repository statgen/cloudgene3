package cloudgene.mapred.api.v2.users;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import cloudgene.mapred.TestApplication;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.TestMailServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UpdatePasswordTest {

	@Inject
	TestApplication application;

	@BeforeAll
	protected void setUp() {
		TestMailServer.getInstance().start();

		// insert two dummy users
		Database database = application.getDatabase();
		UserDao userDao = new UserDao(database);

		User testUser1 = new User();
		testUser1.setUsername("test-1");
		testUser1.setFullName("Test 1");
		testUser1.setMail("test-1@update.password.test");
		testUser1.setRoles(new String[] { "User" });
		testUser1.setActive(true);
		testUser1.setActivationCode(HashUtil.hashPassword("ACTIVATION-CODE-FROM-MAIL"));
		testUser1.setActivationCodeCreated(Instant.now());
		testUser1.setPassword(HashUtil.hashPassword("Old-P4ssword!"));
		userDao.insert(testUser1);

		User testUser2 = new User();
		testUser2.setUsername("test-2");
		testUser2.setFullName("Test 2");
		testUser2.setMail("test-2@update.password.test");
		testUser2.setRoles(new String[] { "User" });
		testUser2.setActive(false);
		testUser2.setActivationCode(HashUtil.hashPassword("ACTIVATION-CODE-FROM-MAIL"));
		testUser2.setActivationCodeCreated(Instant.now());
		testUser2.setPassword(HashUtil.hashPassword("Old-P4ssword!"));
		userDao.insert(testUser2);

		User testUser3 = new User();
		testUser3.setUsername("test-3");
		testUser3.setFullName("Test 3");
		testUser3.setMail("test-3@update.password.test");
		testUser3.setRoles(new String[] { "User" });
		testUser3.setActive(true);
		testUser3.setActivationCode(HashUtil.hashPassword("ACTIVATION-CODE-FROM-MAIL-3"));
		testUser3.setActivationCodeCreated(Instant.now());
		testUser3.setPassword(HashUtil.hashPassword("Old-P4ssword!"));
		userDao.insert(testUser3);

		User testUser4 = new User();
		testUser4.setUsername("test-4");
		testUser4.setFullName("Test 4");
		testUser4.setMail("test-4@update.password.test");
		testUser4.setRoles(new String[] { "User" });
		testUser4.setActive(true);
		testUser4.setActivationCode(HashUtil.hashPassword("ACTIVATION-CODE-FROM-MAIL-4"));
		testUser4.setActivationCodeCreated(Instant.now().minus(1, ChronoUnit.DAYS));
		testUser4.setPassword(HashUtil.hashPassword("Old-P4ssword!"));
		userDao.insert(testUser4);
	}

	@Test
	public void testWithCorrectActivationCode() {
		// try to update invalid password
		Map<String, String> form = new HashMap<>();
		form.put("token", "ACTIVATION-CODE-FROM-MAIL-3");
		form.put("username", "test-3");
		form.put("new-password", "new-password-91");
		form.put("confirm-new-password", "new-password-91");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/update-password")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("Password must contain at least one UPPERCASE letter: A-Z"));

		// try to update password
		form = Map.of(
				"token", "ACTIVATION-CODE-FROM-MAIL-3",
				"username", "test-3",
				"new-password", "New-Password-91",
				"confirm-new-password", "New-Password-91");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/update-password")
				.then()
				.statusCode(200)
				.body("success", equalTo(true))
				.body("message", equalTo("Password successfully updated."));

		// try login with old password
		form = Map.of(
				"username", "test-3",
				"password", "old-password");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/login")
				.then()
				.statusCode(401)
				.body("message", equalTo("Login Failed! Wrong Username or Password."));

		// try login with new password
		form = Map.of(
				"username", "test-3",
				"password", "New-Password-91");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/login")
				.then()
				.statusCode(200)
				.body("username", equalTo("test-3"))
				.body("access_token", notNullValue());
	}

	@Test
	public void testWithWrongActivationCode() {
		Map<String, String> form = Map.of(
				"token", "WRONG TOKEN",
				"username", "test-1",
				"new-password", "Password27",
				"confirm-new-password", "Password27");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/update-password")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("Your recovery request is invalid or expired."));
	}

	@Test
	public void testWithEmptyUsername() {
		Map<String, String> form = Map.of(
				"token", "ACTIVATION-CODE-FROM-MAIL",
				"new-password", "Password27",
				"confirm-new-password", "Password27");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/update-password")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("No username set."));
	}

	@Test
	public void testWithWrongUsername() {
		Map<String, String> form = Map.of(
				"token", "ACTIVATION-CODE-FROM-MAIL",
				"username", "wrong-username",
				"new-password", "Password27",
				"confirm-new-password", "Password27");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/update-password")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("We couldn't find an account with that username or email."));
	}

	@Test
	public void testWithInActiveUser() {
		Map<String, String> form = Map.of(
				"token", "ACTIVATION-CODE-FROM-MAIL",
				"username", "test-2",
				"new-password", "Password27",
				"confirm-new-password", "Password27");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/update-password")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("Account is not activated."));
	}

	@Test
	public void testWithExpiredToken() {
		Map<String, String> form = Map.of(
				"token", "ACTIVATION-CODE-FROM-MAIL-4",
				"username", "test-4",
				"new-password", "Nu^P4sS+64?",
				"confirm-new-password", "Nu^P4sS+64?");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/update-password")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("Your recovery request is invalid or expired."));
	}
}
