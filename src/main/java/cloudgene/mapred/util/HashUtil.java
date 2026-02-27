package cloudgene.mapred.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.bcrypt.BCrypt;

import cloudgene.mapred.core.User;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtil {

	private HashUtil() {}

    // NOTE(Marc): Used in UserService.resetPassword() to create a one-time activation code (used for a randomized activation URL).
    // NOTE(Marc): Used in UserService.registerUser() to create a one-time activation code (used for a randomized activation URL).
    // TODO(Marc): Should be substituted with a fixed-length cryptographically secure random hash.
    /**
     * Calculates a time-based hash.
     * @param user Ignored.
     */
	public static String getActivationHash(User user) {
		return HashUtil.getSha256(System.currentTimeMillis() + "_" + Math.round(2000));
	}

//    // TODO(Marc): Unused!
//    // TODO(Marc): Should be substituted with a fixed-length cryptographically secure random hash.
//    /**
//     * Calculates a time-based hash.
//     * @param user Ignored.
//     */
//	public static String getCsrfToken(User user) {
//		return HashUtil.getSha256(System.currentTimeMillis() + "_" + Math.round(2000));
//	}

    /**
     * Calculates the SHA-256 digest and returns the value as a hex string.
     * (wrapps {@link org.apache.commons.codec.digest.DigestUtils}{@code .sha256Hex()}).
     */
	public static String getSha256(String name) {
		return DigestUtils.sha256Hex(name);
	}

	/** Return BCrypt hash from input */
	public static String hashPassword(String input) {
		String hashed = DigestUtils.md5Hex(input);
		return BCrypt.hashpw(hashed, BCrypt.gensalt());
	}

	/** Check if a provided candidate password is the same as an existing hash */
	public static boolean checkPassword(String candidate, String hash) {
		String hashedCandidate = DigestUtils.md5Hex(candidate);
		return BCrypt.checkpw(hashedCandidate, hash);
	}
}
