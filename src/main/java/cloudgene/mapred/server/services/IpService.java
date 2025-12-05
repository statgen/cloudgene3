package cloudgene.mapred.server.services;

import com.amazonaws.util.EC2MetadataUtils;
import io.micronaut.runtime.server.EmbeddedServer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Singleton
public class IpService {

    @Inject
    private EmbeddedServer embeddedServer;

    private String serverIp = null;

    /**
     * Loops through the system's reported IP addresses and returns the first IPv4 external address. If anything goes
     * wrong, returns `null`.
     *
     * Note that the returned IP is not guaranteed to be visible outside the local network link. E.g., a router in the
     * way could map this IP to a different one.
     */
    private String fetchLocalIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();

                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Does the actual IP fetching work behind `getServerIp()`, see comments there.
     */
    private String fetchServerIp() {
        String ec2Ip = EC2MetadataUtils.getPrivateIpAddress();
        if (ec2Ip != null) return ec2Ip;

        String localIp = fetchLocalIp();
        if (localIp != null) return localIp;

        return embeddedServer.getHost();
    }

    /**
     * Attempts to return this server's IPv4, as visible from its local network.
     * Due to fallback on `EmbeddedServer.getHost()`, might return aliases such as `localhost`.
     * Results are cached for the lifetime of the application.
     *
     * @return This server's IP.
     */
    public String getServerIp() {
        if (serverIp == null) {
            serverIp = fetchServerIp();
        }
        return serverIp;
    }
}
