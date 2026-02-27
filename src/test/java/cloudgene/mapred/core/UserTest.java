package cloudgene.mapred.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class UserTest {

	private static Stream<Arguments> provideForCheckUsername() {
		return Stream.of(
				// Null and empty not allowed.
				arguments(null, "The username is required."),
				arguments("", "The username is required."),

				// Must be 4 - 16 characters.
				arguments("us", "The username must contain between 4 and 16 characters."),
				arguments("username_is_taking_too_long", "The username must contain between 4 and 16 characters."),

				// Must be lowercase identifier.
				arguments("user#name", "Your username is not valid. It can only contain lowercase"),
				arguments("user-name", "Your username is not valid. It can only contain lowercase"),
				arguments("user!name", "Your username is not valid. It can only contain lowercase"),
				arguments("user\"na'me", "Your username is not valid. It can only contain lowercase"),
				arguments("_user name", "Your username is not valid. It can only contain lowercase"),
				arguments("user.name", "Your username is not valid. It can only contain lowercase"),
				arguments(",user,name", "Your username is not valid. It can only contain lowercase"),
				arguments("12username12", "Your username is not valid. It can only contain lowercase"),
				arguments("uSeRnAmE", "Your username is not valid. It can only contain lowercase"),

				// On success, null is returned.
				arguments("user_name", null),
				arguments("username", null),
				arguments("username27", null),
				arguments("user12name", null));
	}

	@ParameterizedTest
	@MethodSource("provideForCheckUsername")
	public void testCheckUsername(String username, String error) {
		// User.checkUsername() returns null on success, and an error string on failure.
		String observed = User.checkUsername(username);

		if (error == null) {
			assertNull(observed);
		} else {
			assertNotNull(observed);
			assertTrue(observed.startsWith(error));
		}
	}

	private static Stream<Arguments> provideForCheckPassword() {
		return Stream.of(
				// Fail if first argument is null or empty.
				arguments(null, null, "Please provide a password."),
				arguments(null, "", "Please provide a password."),
				arguments("", null, "Please provide a password."),
				arguments("", "", "Please provide a password."),
				arguments("", "IAmAVerySecurePwd!@#$5678", "Please provide a password."),

				// Fail if the two passwords don't match.
				arguments("pwd", null, "Please ensure the passwords match."),
				arguments("pwd", "", "Please ensure the passwords match."),
				arguments("pwd", " pwd ", "Please ensure the passwords match."),
				arguments(
						"IAmAVerySecurePwd!@#$5678",
						"IAmAVerySecurePwd!@#$567890",
						"Please ensure the passwords match."),

				// Fail if length requirement is not met.
				arguments("pwd", "pwd", "Password must contain at least 14 characters."),
				// Length is counted as # of Unicode characters (as opposed to e.g. in-memory
				// bytes).
				arguments("这是一个很长的密码", "这是一个很长的密码", "Password must contain at least 14 characters."),

				// Fail if no numbers found.
				arguments(
						"IAmAVerySecurePwd!@#$",
						"IAmAVerySecurePwd!@#$",
						"Password must contain at least one number: 0-9"),

				// Fail if no lowercase letters found.
				arguments(
						"ALL_CAPS-RULES^1337!?!",
						"ALL_CAPS-RULES^1337!?!",
						"Password must contain at least one lowercase letter: a-z"),

				// Fail if no UPPERCASE letters found.
				arguments(
						"relaxing=in-lowercase...42",
						"relaxing=in-lowercase...42",
						"Password must contain at least one UPPERCASE letter: A-Z"),

				// Fail if no special characters found.
				arguments(
						"lowercase UPPERCASE 321",
						"lowercase UPPERCASE 321",
						"Password must contain at least one special character: !\"#$%&'()*+,-./:;<=>?@[]\\^_`{|}~"),

				// All the following are valid passwords.
				arguments("This Is Fine 1!", "This Is Fine 1!", null),
				arguments("PassworDpasswor!d2", "PassworDpasswor!d2", null),
				arguments("PassworDpasswor]d2", "PassworDpasswor]d2", null),
				arguments("qwertyASDFGH\"12345", "qwertyASDFGH\"12345", null),
				arguments("0987qwerASDF$%@^", "0987qwerASDF$%@^", null),
				arguments("大家好！这是我的 password: aA1!", "大家好！这是我的 password: aA1!", null));
	}

	@ParameterizedTest
	@MethodSource("provideForCheckPassword")
	public void testCheckPassword(String password, String confirmPassword, String error) {
		String observed = User.checkPassword(password, confirmPassword);
		assertEquals(error, observed);
	}

	private static Stream<Arguments> provideForCheckMail() {
		return Stream.of(
				// Mail cannot be null or blank.
				arguments(null, "E-Mail is required."),
				arguments("", "E-Mail is required."),
				arguments(" \t\n ", "E-Mail is required."),

				// Mail must be a full e-mail address
				arguments("user", "Please enter a valid mail address."),
				arguments("user.", "Please enter a valid mail address."),
				arguments("user.name", "Please enter a valid mail address."),
				arguments("user.name@", "Please enter a valid mail address."),
				arguments("user.name@host", "Please enter a valid mail address."),
				arguments("user.name@host.", "Please enter a valid mail address."),
				arguments("user.name@.com", "Please enter a valid mail address."),
				arguments("@host.com", "Please enter a valid mail address."),

				// Legacy failure cases
				arguments("user#.name@host.com", "Please enter a valid mail address."),
				arguments("user#.namehost.com", "Please enter a valid mail address."),
				arguments("user#.nameh@.com", "Please enter a valid mail address."),

				// Good emails
				arguments("user.name@host.com", null),
				arguments("foo@bar.org", null),
				arguments("very.long.username@sub.domains.and.so.on.gmail", null),
				arguments("username+subuser@host.com", null));
	}

	@ParameterizedTest
	@MethodSource("provideForCheckMail")
	public void testCheckMail(String mail, String error) {
		String observed = User.checkMail(mail);
		assertEquals(error, observed);
	}

	private static Stream<Arguments> provideForCheckFullName() {
		return Stream.of(
				arguments(null, "The full name is required."),
				arguments("", "The full name is required."),
				arguments(" ", "The full name is required."),
				arguments(" \t\n ", "The full name is required."),

				arguments("a", null),
				arguments("AA", null),
				arguments("Foo Bar", null),
				arguments("@#%^%!#%^236347375", null));
	}

	@ParameterizedTest
	@MethodSource("provideForCheckFullName")
	public void testCheckFullName(String fullName, String error) {
		String observed = User.checkFullName(fullName);
		assertEquals(error, observed);
	}

	private static Stream<Arguments> provideForHasRole() {
		return Stream.of(
				arguments(null, "user", false),
				arguments(new String[] {}, "user", false),
				arguments(new String[] { "foo" }, "user", false),

				arguments(new String[] { "user" }, "user", true),
				arguments(new String[] { "foo", "user", "bar" }, "user", true));
	}

	@ParameterizedTest
	@MethodSource("provideForHasRole")
	public void testHasRole(String[] userRoles, String queryRole, boolean expected) {
		User user = new User();
		user.setRoles(userRoles);

		boolean observed = user.hasRole(queryRole);
		assertEquals(expected, observed);
	}
}
