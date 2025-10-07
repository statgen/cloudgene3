package cloudgene.mapred.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class UserTest {

	@Test
	public void testUsernameRules() {

		assertNotNull(User.checkUsername("us"));
		assertNotNull(User.checkUsername("username_is_taking_too_long"));
		assertNotNull(User.checkUsername("user#name"));
		assertNotNull(User.checkUsername("user-name"));
		assertNotNull(User.checkUsername("user!name"));
		assertNotNull(User.checkUsername("user\"na'me"));
		assertNotNull(User.checkUsername("_user name"));
		assertNotNull(User.checkUsername("user.name"));
		assertNotNull(User.checkUsername(",user,name"));
		assertNotNull(User.checkUsername("12username12"));
		assertNotNull(User.checkUsername("uSeRnAmE"));

		assertNull(User.checkUsername("user_name"));
		assertNull(User.checkUsername("username"));
		assertNull(User.checkUsername("username27"));
		assertNull(User.checkUsername("user12name"));

	}

	@Test
	public void testPasswordRules() {

		assertNotNull(User.checkPassword(null, null));
		assertNotNull(User.checkPassword("", ""));
		assertNotNull(User.checkPassword("password1", "password"));
		assertNotNull(User.checkPassword("pass", "pass"));
		assertNotNull(User.checkPassword("password", "password"));
		assertNotNull(User.checkPassword("PassworD", "PassworD"));
		assertNotNull(User.checkPassword("PassworDpassword", "PassworDpassword"));

		assertNull(User.checkPassword("PassworDpasswor!d2", "PassworDpasswor!d2"));
		assertNull(User.checkPassword("PassworDpasswor]d2", "PassworDpasswor]d2"));
		assertNull(User.checkPassword("qwertyASDFGH\"12345", "qwertyASDFGH\"12345"));
		assertNull(User.checkPassword("0987qwerASDF$%@^", "0987qwerASDF$%@^"));

	}

	@Test
	public void testMail() {
		assertNotNull(User.checkMail("user.name@host"));
		assertNotNull(User.checkMail("user.name@host."));
		assertNotNull(User.checkMail("user#.name@host.com"));
		assertNotNull(User.checkMail("user#.namehost.com"));
		assertNotNull(User.checkMail("user#.nameh@.com"));

		assertNull(User.checkMail("user.name@host.com"));
		assertNull(User.checkMail("username+subuser@host.com"));
	}

}
