package cloudgene.mapred.core;

import cloudgene.mapred.util.HashUtil;
import io.micronaut.core.annotation.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

public class User {

	public static final String ROLE_SEPARATOR = ",";
	public static final String ROLE_ADMIN = "admin";
	public static final String ROLE_USER = "user";

	private static final Pattern DIGIT = Pattern.compile("[0-9]");
	private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
	private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
	private static final Pattern SPECIAL = Pattern.compile("[\"#$%&'()*+,./:;<=>?@\\[\\]\\\\^_`{|}~!-]");
	private static final Pattern EMAIL = Pattern.compile("^[_A-Za-z0-9+-]+(\\.[_A-Za-z0-9+-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
	private static final Pattern USERNAME = Pattern.compile("^[a-z][a-z0-9_]+[a-z0-9]$");
	// \p{?} matches Unicode characters belonging to category ?. L -> letter, Pd -> dashes.
	private static final Pattern FULL_NAME = Pattern.compile("^\\p{L}[\\p{L}\\p{Pd}\\s]*\\p{L}$");

	private int id;
	private String username;
	private String password;
	private String fullName = "";
	private String mail;
	private String[] roles = new String[0];
	private boolean active = true;
	private String activationCode = null;
	private Instant activationCodeCreated = null;
	private Date lastLogin;
	private Date lockedUntil;
	private int loginAttempts;
	private String apiToken = "";
	private Date apiTokenExpiresOn = null;
	private boolean accessedByApi = false;

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setPassword(String pwd) {
		this.password = pwd;
	}

	public String getPassword() {
		return password;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getMail() {
		return mail;
	}

	public void setRoles(String[] roles) {
		this.roles = roles;
	}

	public String[] getRoles() {
		return roles;
	}

	public boolean hasRole(String role) {
		if (roles == null) {
			return false;
		}

		for (String s : roles) {
			if (s.equalsIgnoreCase(role)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasRole(@Nullable Collection<String> roles) {
		if (this.roles == null || roles == null) {
			return false;
		}

		for (String role : roles) {
			if (hasRole(role)) {
				return true;
			}
		}

		return false;
	}

	public void replaceRole(String oldRole, String newRole) {
		for (int i = 0; i < roles.length; i++) {
			if (roles[i].equalsIgnoreCase(oldRole)) {
				roles[i] = newRole;
				return;
			}
		}
	}

	public boolean isAdmin() {
		return hasRole(ROLE_ADMIN);
	}

	public void makeAdmin() {
		setRoles(new String[] { ROLE_ADMIN });
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getActivationCode() {
		return activationCode;
	}

	/**
	 * <b>DON'T USE DIRECTLY</b> (unless you're populating this from a DB or other
	 * serialization).
	 * <p>
	 * Raw setter for {@link #activationCode}. Use {@link #createActivationCode()}
	 * or {@link #clearActivationCode()} instead.
	 */
	public void setActivationCode(String activationCode) {
		this.activationCode = activationCode;
	}

	public Instant getActivationCodeCreated() {
		return activationCodeCreated;
	}

	/**
	 * <b>DON'T USE DIRECTLY</b> (unless you're populating this from a DB or other
	 * serialization).
	 * <p>
	 * Raw setter for {@link #activationCodeCreated}. Use
	 * {@link #createActivationCode()} or {@link #clearActivationCode()} instead.
	 */
	public void setActivationCodeCreated(Instant activationCodeCreated) {
		this.activationCodeCreated = activationCodeCreated;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public String getApiToken() {
		return apiToken;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLockedUntil(Date lockedUntil) {
		this.lockedUntil = lockedUntil;
	}

	public Date getLockedUntil() {
		return lockedUntil;
	}

	public void setLoginAttempts(int loginAttempts) {
		this.loginAttempts = loginAttempts;
	}

	public int getLoginAttempts() {
		return loginAttempts;
	}

	public void setApiTokenExpiresOn(Date apiTokenExpiresOn) {
		this.apiTokenExpiresOn = apiTokenExpiresOn;
	}

	public Date getApiTokenExpiresOn() {
		return apiTokenExpiresOn;
	}

	public void setAccessedByApi(boolean accessedByApi) {
		this.accessedByApi = accessedByApi;
	}

	public boolean isAccessedByApi() {
		return accessedByApi;
	}

	/**
	 * Generates a secure random activation code, sets {@link #activationCode} to a
	 * hash of the code, sets {@link #activationCodeCreated} to now, and returns the
	 * activation code.
	 * <p>
	 * The returned code is not stored.
	 * <p>
	 * <b>NOTE:</b> This method does not call the DB. Updating is up to the caller.
	 *
	 * @return The secure activation code used for password creation and recovery
	 *         (as part of the URL mailed to the end user).
	 */
	public String createActivationCode() {
		String code = HashUtil.getSecureHash();
		String hash = HashUtil.hashPassword(code);

		this.setActivationCode(hash);
		this.setActivationCodeCreated(Instant.now());

		return code;
	}

	/**
	 * Clears {@link #activationCode} and {@link #activationCodeCreated}.
	 * <p>
	 * <b>NOTE:</b> This method does not call the DB. Updating is up to the caller.
	 */
	public void clearActivationCode() {
		this.setActivationCode(null);
		this.setActivationCodeCreated(null);
	}

	/**
	 * Checks if the provided {@code username} follows the formatting requirements.
	 * <p>
	 * Must be between 4 and 16 characters and follow lowercase identifier rules
	 * (starts with {@code a-z}, continues with {@code a-z_0-9}, and ends with
	 * {@code a-z0-9}).
	 *
	 * @param username String to check for username format compliance.
	 * @return {@code null} if no errors were found; error message otherwise.
	 */
	public static String checkUsername(@Nullable String username) {

		if (username == null || username.isEmpty()) {
			return "The username is required.";
		}

		if (username.length() < 4 || username.length() > 16) {
			return "The username must contain between 4 and 16 characters.";
		}

		if (!USERNAME.matcher(username).find()) {
			return "Your username is not valid. It can only contain lowercase letters a-z, digits 0-9, and " +
					"underscores _. It must start with a lowercase letter, and cannot end in an underscore.";
		}

		return null;
	}

	/**
	 * Checks if the provided {@code password} follows the formatting requirements,
	 * and matches {@code confirmPassword}.
	 * <p>
	 * Needs to be at least 14 characters, contain a number {@code 0-9}, contain a
	 * lowercase letter {@code a-z}, contain an uppercase letter {@code A-Z}, and
	 * contain a special character {@code !"#$%&'()*+,-./:;<=>?@[]\^_`{|}~}
	 *
	 * @param password        Proposed password string.
	 * @param confirmPassword Password verification string.
	 * @return {@code null} if no errors were found; error message otherwise.
	 */
	public static String checkPassword(@Nullable String password, @Nullable String confirmPassword) {
		if (password == null || password.isEmpty()) {
			return "Please provide a password.";
		}

		if (!password.equals(confirmPassword)) {
			return "Please ensure the passwords match.";
		}

		if (password.length() < 14) {
			return "Password must contain at least 14 characters.";
		}

		if (!DIGIT.matcher(password).find()) {
			return "Password must contain at least one number: 0-9";
		}

		if (!LOWERCASE.matcher(password).find()) {
			return "Password must contain at least one lowercase letter: a-z";
		}

		if (!UPPERCASE.matcher(password).find()) {
			return "Password must contain at least one UPPERCASE letter: A-Z";
		}

		if (!SPECIAL.matcher(password).find()) {
			return "Password must contain at least one special character: !\"#$%&'()*+,-./:;<=>?@[]\\^_`{|}~";
		}

		return null;
	}

	/**
	 * Checks if the provided {@code mail} follows the formatting requirements.
	 * <p>
	 * Needs to be a valid email address.
	 *
	 * @param mail String to check for email format compliance.
	 * @return {@code null} if no errors were found; error message otherwise.
	 */
	public static String checkMail(@Nullable String mail) {
		if (mail == null || mail.isBlank()) {
			return "E-Mail is required.";
		}

		if (!EMAIL.matcher(mail).find()) {
			return "Please enter a valid mail address.";
		}

		return null;
	}

	/**
	 * Checks if the provided {@code fullName} follows the formatting requirements.
	 * <p>
	 * Must be a non-blank string containing 2 or more Unicode letter characters,
	 * optionally separated by whitespace and/or dashes.
	 *
	 * @param fullName String to check for email format compliance.
	 * @return {@code null} if no errors were found; error message otherwise.
	 */
	public static String checkFullName(@Nullable String fullName) {
		if (fullName == null || fullName.isBlank()) {
			return "The full name is required.";
		}

		if (!FULL_NAME.matcher(fullName).find()) {
			return "Invalid full name. It must contain at least two letters, optionally separated by spaces and/or dashes.";
		}

		return null;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof User user)) return false;
		return Objects.equals(username, user.username);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(username);
	}
}
