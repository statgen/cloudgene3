package cloudgene.mapred.util;

import java.util.Properties;
import java.util.Map;

import cloudgene.mapred.util.config.Settings;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MailUtil {

	private MailUtil() {}

	private static final Logger log = LoggerFactory.getLogger(MailUtil.class);

	public static void notifyAdmin(Settings settings, String subject, String text) throws MessagingException {
		String adminMail = settings.getAdminMail();

		if (adminMail != null && !adminMail.isEmpty()) {
			send(settings, adminMail, subject, text);
		}
	}

	public static void send(Settings settings, String recipients, String subject, String text)
			throws MessagingException {
		Map<String, String> mail = settings.getMail();

		send(
				mail.get("smtp"),
				mail.get("port"),
				mail.get("user"),
				mail.get("password"),
				mail.get("name"),
				recipients,
				subject,
				text);
	}

	public static void send(final String smtp, final String port, final String username, final String password,
			final String name, String recipients, String subject, String text) throws MessagingException {

		Properties props = new Properties();
		props.put("mail.smtp.host", smtp);
		props.put("mail.smtp.port", port);

		Session session = null;

		if (username != null && !username.isEmpty()) {
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			session = Session.getInstance(props, new jakarta.mail.Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});
		} else {
			session = Session.getInstance(props);
		}

		try {
			InternetAddress[] addresses = InternetAddress.parse(recipients);

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(name));
			message.setRecipients(Message.RecipientType.TO, addresses);
			message.setSubject(subject);
			message.setText(text);

			Transport.send(message);

			log.debug("E-Mail sent to {}.", recipients);

		} catch (MessagingException e) {
			throw new MessagingException("Failed to send mail", e);
		}
	}
}
