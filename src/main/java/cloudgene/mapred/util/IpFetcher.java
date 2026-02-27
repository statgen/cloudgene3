package cloudgene.mapred.util;

import com.amazonaws.util.EC2MetadataUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the static method {@link #fetchServerIp()}, which returns this
 * server's IP.
 */
public final class IpFetcher {

	private IpFetcher() {}

	private static String serverIp = null;

	/**
	 * Loops through the system's reported IP addresses and returns the first IPv4
	 * external address. If anything goes wrong, returns null.
	 *
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
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Does the actual IP fetching work behind fetchServerIp(), see comments there.
	 */
	private static String actuallyFetchTheIp() {
		Logger.getLogger("com.amazonaws").setLevel(Level.SEVERE); // Avoid warning dumps if we're not in AWS
		String ec2Ip = EC2MetadataUtils.getPrivateIpAddress();
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
	 *
	 * Results are cached for the lifetime of the application.
	 */
	public static String fetchServerIp() {
		if (serverIp == null) {
			serverIp = actuallyFetchTheIp();
		}
		return serverIp;
	}
}
