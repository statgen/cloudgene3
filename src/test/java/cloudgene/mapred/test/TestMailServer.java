package cloudgene.mapred.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

/**
 * Singleton providing a mock SMTP server
 */
public class TestMailServer {

	public final static int PORT = 9985;

	private static TestMailServer instance;

	private SimpleSmtpServer smtp;

	private TestMailServer() {}

	public static TestMailServer getInstance() {
		if (instance == null) {
			instance = new TestMailServer();
		}
		return instance;
	}

	public void start() {
		if (smtp == null) {
			smtp = SimpleSmtpServer.start(PORT);
		}
	}

	public synchronized int getReceivedEmailSize() {
		return smtp.getReceivedEmailSize();
	}

	public synchronized Iterator<SmtpMessage> getReceivedEmail() {
		return (Iterator<SmtpMessage>) smtp.getReceivedEmail();
	}

	public List<SmtpMessage> getReceivedEmailAsList() {
		Iterator<SmtpMessage> iterator = getReceivedEmail();
		List<SmtpMessage> list = new ArrayList<>();

		while (iterator.hasNext()) {
			list.add(iterator.next());
		}

		return list;
	}
}