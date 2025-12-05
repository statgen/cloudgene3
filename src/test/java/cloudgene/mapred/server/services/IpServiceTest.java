package cloudgene.mapred.server.services;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class IpServiceTest {

    @Test
    public void testGetServerIpReturnsIpV4() {
        IpService ipService = new IpService();

        Pattern regex = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

        String serverIp = ipService.getServerIp();
        Matcher matcher = regex.matcher(serverIp);

        assertTrue(matcher.find());

    }
}
