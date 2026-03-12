package cloudgene.mapred.api.v2.users;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.dumbster.smtp.SmtpMessage;

import cloudgene.mapred.TestApplication;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao;
import cloudgene.mapred.database.util.Database;
import cloudgene.mapred.util.TestMailServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ActivateUserTest {

	@Inject
	TestApplication application;

	@BeforeAll
	protected void setUp() {
		TestMailServer.getInstance().start();
	}

	@Test
	public void testUserActivation() {
		TestMailServer mailServer = TestMailServer.getInstance();
		int mailsBefore = mailServer.getReceivedEmailSize();

		// form data
		Map<String, String> form = new HashMap<>();
		form.put("username", "unique_name_5");
		form.put("full-name", "Full Name");
		form.put("mail", "new.user@test.com");
		form.put("new-password", "LongPassword@1714");
		form.put("confirm-new-password", "LongPassword@1714");

		// register user
		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/register")
				.then()
				.statusCode(200)
				.body("success", equalTo(true))
				.body("message", equalTo("User successfully created."));

		int mailsAfter = mailServer.getReceivedEmailSize();
		assertEquals(mailsBefore + 1, mailsAfter); // Exactly one email received

		// get activation key from database
		Database database = application.getDatabase();
		UserDao userDao = new UserDao(database);
		User user = userDao.findByUsername("unique_name_5");
		assertNotNull(user);

		// check if correct key is in mail
		SmtpMessage message = mailServer.getReceivedEmailAsList().get(mailsBefore);
		assertTrue(message.getBody().contains(user.getActivationCode()));

		// login should not be possible
		form = new HashMap<>();
		form.put("username", "unique_name_5");
		form.put("password", "LongPassword@1714");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/login")
				.then()
				.statusCode(401)
				.body("message", equalTo("Login Failed! User account is not activated."));

		// activate user with wrong activation code
		RestAssured
				.when()
				.get("/users/activate/" + user.getUsername() + "/RANDOMACTIVATIONCODE")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("Wrong activation code."));

		// activate user with wrong username
		RestAssured
				.when()
				.get("/users/activate/randomusername/" + user.getActivationCode())
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("Wrong username."));

		// login should not be possible after wrong activation attempts
		form = new HashMap<>();
		form.put("username", "unique_name_5");
		form.put("password", "LongPassword@1714");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/login")
				.then()
				.statusCode(401)
				.body("message", equalTo("Login Failed! User account is not activated."));

		// activate user with correct data
		RestAssured
				.when()
				.get("/users/activate/" + user.getUsername() + "/" + user.getActivationCode())
				.then()
				.statusCode(200)
				.body("success", equalTo(true))
				.body("message", equalTo("User successfully activated."));

		// login should work
		form = new HashMap<>();
		form.put("username", "unique_name_5");
		form.put("password", "LongPassword@1714");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/login")
				.then()
				.statusCode(200)
				.body("username", equalTo("unique_name_5"))
				.body("access_token", notNullValue());
	}
}
