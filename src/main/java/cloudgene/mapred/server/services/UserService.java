package cloudgene.mapred.server.services;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cloudgene.mapred.database.dao.CounterDao;
import cloudgene.mapred.database.dao.JobValueDao;
import cloudgene.mapred.jobs.JobValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.core.Template;
import cloudgene.mapred.core.User;
import cloudgene.mapred.database.dao.UserDao;
import cloudgene.mapred.server.Application;
import cloudgene.mapred.server.exceptions.JsonHttpStatusException;
import cloudgene.mapred.server.responses.MessageResponse;
import cloudgene.mapred.util.HashUtil;
import cloudgene.mapred.util.MailUtil;
import cloudgene.mapred.util.Page;
import io.micronaut.http.HttpStatus;

@Singleton
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	public static final String MESSAGE_USER_NOT_FOUND = "User %s not found.";
	public static final String DEFAULT_ROLE = "User";
	public static final String DEFAULT_ANONYMOUS_ROLE = "Anonymous_User";

	/** Minimum time between consecutive password recovery requests, in seconds. */
	private static final int RECOVERY_REQUEST_COOLDOWN_S = 60;
	/** Time before the password recovery token expires, in seconds. */
	private static final int RECOVERY_REQUEST_EXPIRES_IN_S = 60 * 60;

	private static final String MESSAGE_USER_PROFILE_DELETE = "User profile successfully delete.";
	private static final String MESSAGE_DELETE_ERROR = "Error during deleting your user profile.";
	private static final String MESSAGE_WRONG_PASSWORD = "Wrong password.";
	private static final String MESSAGE_PROFILE_UPDATED = "User profile successfully updated.";
	private static final String MESSAGE_NOT_ALLOWED = "You are not allowed to change this user profile.";
	private static final String MESSAGE_NO_USERNAME_SET = "No username set.";
	private static final String MESSAGE_PASSWORD_UPDATED = "Password successfully updated.";
	private static final String MESSAGE_RECOVERY_REQUEST_COOLDOWN = "You must wait " + RECOVERY_REQUEST_COOLDOWN_S
			+ " seconds between consecutive recovery requests.";
	private static final String MESSAGE_INVALID_RECOVERY_REQUEST = "Your recovery request is invalid or expired.";
	private static final String MESSAGE_ACCOUNT_IS_INACTIVE = "Account is not activated.";
	private static final String MESSAGE_ACCOUNT_NOT_FOUND = "We couldn't find an account with that username or email.";
	private static final String MESSAGE_EMAIL_SENT = "We sent you an email with instructions on how to reset your password.";
	private static final String MESSAGE_EMAIL_NOT_AVAILABLE = "No email address is associated with the provided username. Therefore, password recovery cannot be completed.";
	private static final String MESSAGE_SENDING_EMAIL_FAILED = "Sending recovery email failed. ";
	private static final String MESSAGE_INVALID_USERNAME = "Please enter a valid username or email address.";
	private static final String MESSAGE_USER_CREATED = "User successfully created.";
	private static final String MESSAGE_EMAIL_ALREADY_REGISTERED = "E-Mail is already registered.";
	private static final String MESSAGE_USERNAME_ALREADY_EXISTS = "Username already exists.";
	private static final String MESSAGE_WRONG_USERNAME = "Wrong username.";
	private static final String MESSAGE_WRONG_ACTIVATION_CODE = "Wrong activation code.";
	private static final String MESSAGE_USER_ACTIVATED = "User successfully activated.";

	protected Application application;

	public UserService(Application application) {
		this.application = application;
	}

	public Page<User> getAll(String query, Integer page, int pageSize) {
		int offset = 0;

		if (page != null) {
			offset = page;
			if (offset < 1) {
				offset = 1;
			}
			offset = (offset - 1) * pageSize;
		}

		UserDao dao = new UserDao(application.getDatabase());

		List<User> users;
		int count;

		if (query != null && !query.isEmpty()) {
			users = dao.findByQuery(query);
			page = 1;
			count = users.size();
			pageSize = count;
		} else {
			if (page != null) {
				users = dao.findAll(offset, pageSize);
				count = dao.findAll().size();
			} else {
				users = dao.findAll();
				page = 1;
				count = users.size();
				pageSize = count;
			}
		}

		Page<User> result = new Page<>();

		result.setCount(count);
		result.setPage(page);
		result.setPageSize(pageSize);
		result.setData(users);

		return result;
	}

	public User getByUsername(String username) {
		UserDao dao = new UserDao(application.getDatabase());
		User user = dao.findByUsername(username);

		if (user == null) {
			throw new JsonHttpStatusException(HttpStatus.NOT_FOUND, String.format(MESSAGE_USER_NOT_FOUND, username));
		}

		return user;
	}

	public User deleteUser(User user) {
		UserDao dao = new UserDao(application.getDatabase());
		dao.delete(user);
		return user;
	}

	public User changeRoles(User user, String roles) {
		UserDao dao = new UserDao(application.getDatabase());
		user.setRoles(roles.split(User.ROLE_SEPARATOR));
		dao.update(user);
		return user;
	}

	public MessageResponse updateProfile(
			User user,
			String username,
			String fullName,
			String mail,
			String newPassword,
			String confirmNewPassword) {

		String error = User.checkUsername(username);
		if (error != null) {
			return MessageResponse.error(error);
		}

		// check if user is admin or it is his username
		if (!user.getUsername().equals(username) && !user.isAdmin()) {

			log.error("User: ID {} ('{}') attempted to change profile of a different username '{}'",
					user.getId(), user.getUsername(), username);

			return MessageResponse.error(MESSAGE_NOT_ALLOWED);
		}

		error = User.checkFullName(fullName);
		if (error != null) {
			return MessageResponse.error(error);
		}

		boolean mailProvided = (mail != null && !mail.isEmpty());

		if (application.getSettings().isEmailRequired() || mailProvided) {
			error = User.checkMail(mail);
			if (error != null) {
				return MessageResponse.error(error);
			}
		}

		UserDao dao = new UserDao(application.getDatabase());
		User newUser = dao.findByUsername(username);
		newUser.setFullName(fullName);
		newUser.setMail(mail);

		if (user.getMail() != null && !user.getMail().equals(newUser.getMail())) {
			log.info("User: changed email address for user {} (ID {})", newUser.getUsername(), newUser.getId());
		}

		String roleMessage = "";
		if (!application.getSettings().isEmailRequired()) {
			String newUserMail = newUser.getMail();
			boolean newMailAvailable = (newUserMail != null && !newUserMail.isEmpty());

			if (!newMailAvailable & user.hasRole(DEFAULT_ROLE)) {
				newUser.replaceRole(DEFAULT_ROLE, DEFAULT_ANONYMOUS_ROLE);

				log.info("User: changed role to {} for user {} (ID {})",
						DEFAULT_ANONYMOUS_ROLE, newUser.getUsername(), newUser.getId());

				roleMessage += "<br><br>Your account has been <b>downgraded</b>.<br>To apply these changes, please log out and log back in.";
			} else if (newMailAvailable && user.hasRole(DEFAULT_ANONYMOUS_ROLE)) {
				newUser.replaceRole(DEFAULT_ANONYMOUS_ROLE, DEFAULT_ROLE);

				log.info("User: changed role to {} for user {} (ID {})",
						DEFAULT_ROLE, newUser.getUsername(), newUser.getId());

				roleMessage += "<br><br>Your account has been <b>upgraded</b>.<br>To apply these changes, please log out and log back in.";
			}
		}

		// update password only when it's not empty
		if (newPassword != null && !newPassword.isEmpty()) {
			error = User.checkPassword(newPassword, confirmNewPassword);

			if (error != null) {
				return MessageResponse.error(error);
			}

			newUser.setPassword(HashUtil.hashPassword(newPassword));

			log.info("User: changed password for user {} (ID {} - email {})",
					newUser.getUsername(), newUser.getId(), newUser.getMail());
		}

		dao.update(newUser);

		return MessageResponse.success(MESSAGE_PROFILE_UPDATED + roleMessage);
	}

	public MessageResponse deleteProfile(User user, String username, String password) {
		// check if user is admin or it is his username
		if (!user.getUsername().equals(username) && !user.isAdmin()) {
			throw new JsonHttpStatusException(HttpStatus.FORBIDDEN, MESSAGE_NOT_ALLOWED);
		}

		if (HashUtil.checkPassword(password, user.getPassword())) {

			UserDao dao = new UserDao(application.getDatabase());

			log.info("User: requested deletion of account {} (ID {} - email {})",
					user.getUsername(), user.getId(), user.getMail());

			boolean deleted = dao.delete(user);
			if (deleted) {
				return MessageResponse.success(MESSAGE_USER_PROFILE_DELETE);
			} else {
				throw new JsonHttpStatusException(HttpStatus.BAD_REQUEST, MESSAGE_DELETE_ERROR);
			}

		} else {
			throw new JsonHttpStatusException(HttpStatus.UNAUTHORIZED, MESSAGE_WRONG_PASSWORD);
		}
	}

	public MessageResponse updatePassword(
			@Nullable String username,
			String token,
			@Nullable String newPassword,
			@Nullable String confirmNewPassword) {

		if (username == null || username.isEmpty()) {
			return MessageResponse.error(MESSAGE_NO_USERNAME_SET);
		}

		UserDao dao = new UserDao(application.getDatabase());
		User user = dao.findByUsername(username);

		if (user == null) {
			return MessageResponse.error(MESSAGE_ACCOUNT_NOT_FOUND);
		}

		if (!user.isActive()) {
			return MessageResponse.error(MESSAGE_ACCOUNT_IS_INACTIVE);
		}

		Instant codeCreated = user.getActivationCodeCreated();
		if (codeCreated == null) {
			user.clearActivationCode();
			dao.update(user);
			return MessageResponse.error(MESSAGE_INVALID_RECOVERY_REQUEST);
		}
		Instant codeExpires = codeCreated.plusSeconds(RECOVERY_REQUEST_EXPIRES_IN_S);
		if (Instant.now().isAfter(codeExpires)) {
			user.clearActivationCode();
			dao.update(user);
			return MessageResponse.error(MESSAGE_INVALID_RECOVERY_REQUEST);
		}

		String dbHash = user.getActivationCode();
		if (dbHash == null || dbHash.isEmpty()) {
			user.clearActivationCode();
			dao.update(user);
			return MessageResponse.error(MESSAGE_INVALID_RECOVERY_REQUEST);
		}

		if (!HashUtil.checkPassword(token, dbHash)) {
			return MessageResponse.error(MESSAGE_INVALID_RECOVERY_REQUEST);
		}

		String error = User.checkPassword(newPassword, confirmNewPassword);
		if (error != null) {
			return MessageResponse.error(error);
		}

		user.setPassword(HashUtil.hashPassword(newPassword));
		user.clearActivationCode();
		dao.update(user);

		log.info("User: changed password via account recovery mechanism for user {} (ID {} - email {})",
				user.getUsername(), user.getId(), user.getMail());

		return MessageResponse.success(MESSAGE_PASSWORD_UPDATED);
	}

	public MessageResponse resetPassword(String username) {
		if (username == null || username.isBlank()) {
			return MessageResponse.error(MESSAGE_INVALID_USERNAME);
		}

		UserDao dao = new UserDao(application.getDatabase());
		User user = dao.findByUsername(username);

		if (user == null) {
			user = dao.findByMail(username);
		}

		if (user == null) {
			return MessageResponse.error(MESSAGE_ACCOUNT_NOT_FOUND);
		}

		if (!user.isActive()) {
			return MessageResponse.error(MESSAGE_ACCOUNT_IS_INACTIVE);
		}

		Instant codeCreated = user.getActivationCodeCreated();
		if (codeCreated != null) {
			Instant resubmitCutoff = codeCreated.plusSeconds(RECOVERY_REQUEST_COOLDOWN_S);
			if (resubmitCutoff.isAfter(Instant.now())) {
				return MessageResponse.error(MESSAGE_RECOVERY_REQUEST_COOLDOWN);
			}
		}

		String key = user.createActivationCode();
		dao.update(user);

		String hostname = application.getSettings().getServerUrl();
		hostname += application.getSettings().getBaseUrl();

		String link = hostname + "/#!recovery/" + user.getUsername() + "/" + key;

		// send email with activation code
		String app = application.getSettings().getName();
		String subject = "[" + app + "] Password Recovery";
		String body = application.getTemplate(Template.RECOVERY_MAIL, user.getFullName(), app, link);

		try {
			if (user.getMail() != null && !user.getMail().isEmpty()) {
				log.info("Password reset link requested for user '{}'", username);
				MailUtil.send(application.getSettings(), user.getMail(), subject, body);
				return MessageResponse.success(MESSAGE_EMAIL_SENT);
			} else {
				return MessageResponse.error(MESSAGE_EMAIL_NOT_AVAILABLE);
			}
		} catch (Exception e) {
			return MessageResponse.error(MESSAGE_SENDING_EMAIL_FAILED + e.getMessage());
		}
	}

	public MessageResponse registerUser(
			String username,
			String mail,
			String newPassword,
			String confirmNewPassword,
			String fullName) {

		// ==== USERNAME ==== //

		// check username format
		String error = User.checkUsername(username);
		if (error != null) {
			return MessageResponse.error(error);
		}

		// check if username available
		UserDao dao = new UserDao(application.getDatabase());
		if (dao.findByUsername(username) != null) {
			return MessageResponse.error(MESSAGE_USERNAME_ALREADY_EXISTS);
		}

		// ==== MAIL ==== //

		boolean mailProvided = (mail != null && !mail.isEmpty());

		if (application.getSettings().isEmailRequired() || mailProvided) {
			// Check mail format
			error = User.checkMail(mail);
			if (error != null) {
				return MessageResponse.error(error);
			}

			// Check if mail available
			if (dao.findByMail(mail) != null) {
				return MessageResponse.error(MESSAGE_EMAIL_ALREADY_REGISTERED);
			}
		}

		String[] roles = new String[] { mailProvided ? DEFAULT_ROLE : DEFAULT_ANONYMOUS_ROLE };

		// check password format
		error = User.checkPassword(newPassword, confirmNewPassword);
		if (error != null) {
			return MessageResponse.error(error);
		}

		// check full name format
		error = User.checkFullName(fullName);
		if (error != null) {
			return MessageResponse.error(error);
		}

		User newUser = new User();
		newUser.setUsername(username);
		newUser.setFullName(fullName);
		newUser.setMail(mail);
		newUser.setRoles(roles);
		newUser.setPassword(HashUtil.hashPassword(newPassword));

		try {
			String hostname = application.getSettings().getServerUrl();
			hostname += application.getSettings().getBaseUrl();

			// if email server configured, send mails with activation link. Else
			// activate user immediately.

			if (application.getSettings().getMail() != null && mailProvided) {
				String activationKey = newUser.createActivationCode();
				newUser.setActive(false);

				// send email with activation code
				String appName = application.getSettings().getName();
				String subject = "[" + appName + "] Signup activation";
				String activationLink = hostname + "/#!activate/" + username + "/" + activationKey;
				String body = application.getTemplate(Template.REGISTER_MAIL, fullName, appName, activationLink);

				MailUtil.send(application.getSettings(), mail, subject, body);
			} else {
				newUser.setActive(true);
				newUser.clearActivationCode();
			}

			log.info("Registration: New user {} (ID {} - email {} - roles {})", newUser.getUsername(), newUser.getId(),
					newUser.getMail(), Arrays.toString(newUser.getRoles()));

			dao.insert(newUser);

			return MessageResponse.success(MESSAGE_USER_CREATED);
		} catch (Exception e) {
			return MessageResponse.error(e.getMessage());
		}
	}

	@NonNull
	public MessageResponse activateUser(@NonNull String username, @NonNull String code) {
		UserDao dao = new UserDao(application.getDatabase());

		User user = dao.findByUsername(username);
		if (user == null) {
			log.warn("User: attempted activation for unknown username '{}'", username);
			return MessageResponse.error(MESSAGE_WRONG_USERNAME);
		}

		String activationCode = user.getActivationCode();
		if (activationCode == null || activationCode.isEmpty()) {
			log.warn(
					"User: attempted activation, but no code present in DB (already activated?). "
							+ "Username: '{}' (ID {} - email {})",
					user.getUsername(), user.getId(), user.getMail());
			return MessageResponse.error(MESSAGE_WRONG_ACTIVATION_CODE);
		}

		if (HashUtil.checkPassword(code, activationCode)) {
			user.setActive(true);
			user.clearActivationCode();
			dao.update(user);

			log.info("User: activated user '{}' (ID {} - email {})",
					user.getUsername(), user.getId(), user.getMail());

			return MessageResponse.success(MESSAGE_USER_ACTIVATED);
		} else {
			log.warn("User: code is either incorrect or has already been used for user '{}' (ID {} - email {})",
					user.getUsername(), user.getId(), user.getMail());

			return MessageResponse.error(MESSAGE_WRONG_ACTIVATION_CODE);
		}
	}

	/**
	 * Queries the database for all counters related to this user.
	 * <p>
	 * Since the database only stores counters for successfully completed jobs, this
	 * method does not show data for ongoing or failed jobs.
	 */
	@NonNull
	public Map<String, Long> getUserCounters(@NonNull User user) {
		CounterDao counterDao = new CounterDao(application.getDatabase());
		return counterDao.getByUser(user);
	}

	/**
	 * Queries the database for all job values related to this user.
	 */
	@NonNull
	public List<JobValue> getUserValues(@NonNull User user) {
		JobValueDao valueDao = new JobValueDao(application.getDatabase());
		return valueDao.getByUser(user);
	}
}
