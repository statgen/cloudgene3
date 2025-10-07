package cloudgene.mapred.core;

import java.util.Date;
import java.util.regex.Pattern;

public class User {

	private static final Pattern DIGIT = Pattern.compile("[0-9]");
	private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
	private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
	private static final Pattern SPECIAL = Pattern.compile("[\"#$%&'()*+,./:;<=>?@\\[\\]\\\\^_`{|}~!-]");
	private static final Pattern EMAIL = Pattern.compile("^[_A-Za-z0-9+-]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
	private static final Pattern USERNAME = Pattern.compile("^[a-z][a-z0-9_]+[a-z0-9]$");

	private String username;

	private String password;

	private int id;

	private String fullName = "";

	private String mail;

	private String[] roles = new String[0];

	private boolean active = true;

	private String activationKey = null;

	private String apiToken = "";

	private Date lastLogin;

	private Date lockedUntil;

	private int loginAttempts;

	private Date apiTokenExpiresOn = null;
	
	private boolean accessedByApi = false;

	public static final String ROLE_SEPARATOR = ",";

	public static final String ROLE_ADMIN = "admin";

	public static final String ROLE_USER = "user";

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
		for (int i = 0; i < roles.length; i++) {
			if (roles[i].equalsIgnoreCase(role)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasRole(String[] roles) {
		if (this.roles == null || roles == null) {
			return false;
		}
		for (int i = 0; i < roles.length; i++) {
			if (hasRole(roles[i])) {
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
		return activationKey;
	}

	public void setActivationCode(String activationKey) {
		this.activationKey = activationKey;
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

	public static String checkUsername(String username) {

		if (username == null || username.isEmpty()) {
			return "The username is required.";
		}

		if (username.length() < 4 || username.length() > 12) {
			return "The username must contain between 4 and 12 characters.";
		}

		if (!USERNAME.matcher(username).find()) {
			return "Your username is not valid. It can only contain lowercase letters a-z, digits 0-9, and " +
					"underscores _. It must start with a lowercase letter, and cannot end in an underscore.";
		}

		return null;

	}
	
	public void setAccessedByApi(boolean accessedByApi) {
		this.accessedByApi = accessedByApi;
	}
	
	public boolean isAccessedByApi() {
		return accessedByApi;
	}

	public static String checkPassword(String password, String confirmPassword) {

		if (password == null || password.isEmpty() || !password.equals(confirmPassword)) {
			return "Please check your passwords.";
		}

		if (password.length() < 14) {
			return "Password must contain at least 14 characters!";
		}

		if (!DIGIT.matcher(password).find()) {
			return "Password must contain at least one number (0-9)!";
		}

		if (!LOWERCASE.matcher(password).find()) {
			return "Password must contain at least one lowercase letter (a-z)!";
		}

		if (!UPPERCASE.matcher(password).find()) {
			return "Password must contain at least one uppercase letter (A-Z)!";
		}

		if (!SPECIAL.matcher(password).find()) {
			return "Password must contain at least one special character: !\"#$%&'()*+,-./:;<=>?@[]\\^_`{|}~";
		}

		return null;

	}

	public static String checkMail(String mail) {

		if (mail == null || mail.isEmpty()) {
			return "E-Mail is required.";
		}

		if (!EMAIL.matcher(mail).find()) {
			return "Please enter a valid mail address.";
		}

		return null;
	}

	public static String checkName(String name) {

		if (name == null || name.isEmpty()) {
			return "The full name is required.";
		}

		return null;
	}

	@Override
	public boolean equals(Object obj) {
		return ((User) obj).getUsername().equals(username);
	}

}
