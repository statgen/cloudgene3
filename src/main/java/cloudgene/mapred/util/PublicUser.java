package cloudgene.mapred.util;

import cloudgene.mapred.core.User;
import cloudgene.mapred.database.UserDao;
import cloudgene.mapred.database.util.Database;

public final class PublicUser {

	private PublicUser() {
	}

	/**
	 * Fetches the "public" user from the {@code database}.
	 * If no such user exists, it is created.
	 * <p>
	 * The public user receives all orphaned jobs from deleted users.
	 */
	public static User getUser(Database database) {
		// TODO(Marc): The logic here hints at problematic stuff. You can log in as the
		//             public user by using 'public-password' as the password (I tried,
		//             it works). Probably, this user should exist by default, not be a
		//             valid username, and not be login-able.

		UserDao dao = new UserDao(database);
		User user = dao.findByUsername("public");

		if (user == null) {
			user = new User();
			user.setUsername("public");
			String password = HashUtil.getSha256("public-password");
			user.setPassword(password);
			user.setRoles(new String[] { "public" });
			dao.insert(user);
		}

		return user;
	}
}
