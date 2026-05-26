package cloudgene.mapred.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Provides the static method {@link #fetchServerIp()}, which returns this
 * server's IP.
 */
public final class IpFetcher {

	private static final Logger log = LoggerFactory.getLogger(IpFetcher.class);

	private IpFetcher() {}

	private static String serverIp = null;

	/**
	 * Loops through the system's reported IP addresses and returns the first IPv4
	 * external address. If anything goes wrong, returns null.
	 * <p>
	 * Note that the returned IP is only guaranteed to be visible in the local
	 * network link. E.g., a router in the way could map this IP to a different one.
	 */
	private static String fetchSystemIpV4() {
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface iface = networkInterfaces.nextElement();

				if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint()) {
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

			log.warn("No system IP found (is the computer offline?)");
			return null;
		} catch (Exception e) {
			log.error("Error while trying to fetch system IPv4", e);
			return null;
		}
	}

	/**
	 * Wrapper for {@link EC2MetadataUtils#getPrivateIpAddress()}. Attempts to use
	 * the EC2 service (AWS) to retrieve this device's IPv4, as visible from the
	 * local VPC subnet. If anything goes wrong, returns {@code null} instead of
	 * throwing.
	 */
	private static String fetchEC2IpV4() {
		try {
			return EC2MetadataUtils.getPrivateIpAddress();
		} catch (SdkClientException e) {
			return null;
		}
	}

	/**
	 * Does the actual IP fetching work behind fetchServerIp(), see comments there.
	 */
	private static String actuallyFetchTheIp() {
		String ec2Ip = fetchEC2IpV4();
		if (ec2Ip != null) {
			return ec2Ip;
		}

		String localIp = fetchSystemIpV4();
		if (localIp != null) {
			return localIp;
		}

		throw new IllegalStateException("Failed to obtain a valid server IPv4 address from available metadata.");
	}

	/**
	 * Fetches this server's IPv4, as visible from its local network. In an AWS
	 * environment, it uses the EC2 metadata service to get the "private IP" that is
	 * valid in the local VPC. Otherwise, attempts to get the address from the
	 * system. Raises an IllegalStateException if no valid IPv4 can be obtained.
	 * <p>
	 * Results are cached for the lifetime of the application.
	 */
	public static String fetchServerIp() {
		if (serverIp == null) {
			serverIp = actuallyFetchTheIp();
		}
		return serverIp;
	}
}
