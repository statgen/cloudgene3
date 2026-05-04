package cloudgene.mapred.api.v2.users;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.dumbster.smtp.SmtpMessage;

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
public class ResetPasswordTest {

	@Inject
	TestApplication application;

	@BeforeAll
	protected void setUp() throws Exception {
		TestMailServer.getInstance().start();

		// insert two dummy users
		Database database = application.getDatabase();
		UserDao userDao = new UserDao(database);

		User testUser1 = new User();
		testUser1.setUsername("test_reset_1");
		testUser1.setFullName("Test Reset 1");
		testUser1.setMail("test_reset_1@reset.password.test");
		testUser1.setRoles(new String[] { "User" });
		testUser1.setActive(true);
		testUser1.setActivationCode("");
		testUser1.setActivationCodeCreated(null);
		testUser1.setPassword(HashUtil.hashPassword("olD-Password+1024?"));
		userDao.insert(testUser1);

		User testUser2 = new User();
		testUser2.setUsername("test_reset_2");
		testUser2.setFullName("Test Reset 2");
		testUser2.setMail("test_reset_2@reset.password.test");
		testUser2.setRoles(new String[] { "User" });
		testUser2.setActive(false);
		testUser2.setActivationCode(HashUtil.hashPassword("fdsfdsfsdfsdfsd"));
		testUser2.setActivationCodeCreated(Instant.now());
		testUser2.setPassword(HashUtil.hashPassword("olD-Password+2048?"));
		userDao.insert(testUser2);
	}

	@Test
	public void testWithWrongName() {
		TestMailServer mailServer = TestMailServer.getInstance();
		int mailsBefore = mailServer.getReceivedEmailSize();

		Map<String, String> form = Map.of("username", "unknown-user-wrong");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/reset")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("We couldn't find an account with that username or email."));

		assertEquals(mailsBefore, mailServer.getReceivedEmailSize());
	}

	@Test
	public void testWithInActiveUser() {
		TestMailServer mailServer = TestMailServer.getInstance();
		int mailsBefore = mailServer.getReceivedEmailSize();

		Map<String, String> form = Map.of("username", "test_reset_2");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/reset")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("Account is not activated."));

		assertEquals(mailsBefore, mailServer.getReceivedEmailSize());
	}

	@Test
	public void testWithWrongEMail() {
		TestMailServer mailServer = TestMailServer.getInstance();
		int mailsBefore = mailServer.getReceivedEmailSize();

		Map<String, String> form = Map.of("username", "wrong@e-mail.com");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/reset")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("We couldn't find an account with that username or email."));

		assertEquals(mailsBefore, mailServer.getReceivedEmailSize());
	}

	@Test
	public void testWithSpecial() {
		TestMailServer mailServer = TestMailServer.getInstance();
		int mailsBefore = mailServer.getReceivedEmailSize();

		Map<String, String> form = Map.of("username", "%");

		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/reset")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", equalTo("We couldn't find an account with that username or email."));

		assertEquals(mailsBefore, mailServer.getReceivedEmailSize());
	}

	@Test
	public void testResetPassword() {
		TestMailServer mailServer = TestMailServer.getInstance();
		int mailsBefore = mailServer.getReceivedEmailSize();

		Map<String, String> form = Map.of("username", "test_reset_1");

		// rest password and check if mail was sent
		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/reset")
				.then()
				.statusCode(200)
				.body("success", equalTo(true))
				.body("message", containsString("We sent you an email"));

		assertEquals(mailsBefore + 1, mailServer.getReceivedEmailSize());

		// try it a second time (nervous user) -> fails (too soon)
		RestAssured
				.given()
				.formParams(form)
				.when()
				.post("/api/v2/users/reset")
				.then()
				.statusCode(200)
				.body("success", equalTo(false))
				.body("message", containsString("You must wait"));

		// No new mail sent
		assertEquals(mailsBefore + 1, mailServer.getReceivedEmailSize());

		// get activation key from database and check if key was reused in mail2
		Database database = application.getDatabase();
		UserDao userDao = new UserDao(database);
		User user = userDao.findByUsername("test_reset_1");
		assertNotNull(user);

		// check if correct key is in mail
		SmtpMessage message = mailServer.getReceivedEmailAsList().get(mailsBefore);
		String activationCode = extractActivationCode(message.getBody());
		assertTrue(HashUtil.checkPassword(activationCode, user.getActivationCode()));
	}

	private static final Pattern URL_PATTERN = Pattern.compile("https?://[^/]+/#!recovery/test_reset_1/([a-zA-Z0-9]+)");

	private static String extractActivationCode(String mailBody) {
		Matcher matcher = URL_PATTERN.matcher(mailBody);
		assertTrue(matcher.find());
		return matcher.group(1);
	}
}
