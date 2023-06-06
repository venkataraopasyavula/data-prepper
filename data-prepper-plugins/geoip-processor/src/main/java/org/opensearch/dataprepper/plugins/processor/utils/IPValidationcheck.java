package org.opensearch.dataprepper.plugins.processor.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPValidationcheck {

    public static boolean ipValidationcheck(String input) throws UnknownHostException {
        String ipAddress = input;
        InetAddress address = InetAddress.getByName(input);
        if (address instanceof Inet6Address) {
            return PrivateIPVersionCheck(ipAddress);
        } else if (address instanceof Inet4Address) {
            return PrivateIPVersionCheck(ipAddress);
        }
        return false;
    }
    public static boolean PrivateIPVersionCheck(String ipAddress) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(ipAddress);
        if (address instanceof Inet6Address) {
            boolean PrivateIPv6Check = false;
            String[] ipv6Parts = ipAddress.trim().split(":");
            if (ipv6Parts.length > 0) {
                String firstPart = ipv6Parts[0];
                String prefix = firstPart.substring(0, 2);
                if (firstPart.equalsIgnoreCase("fe80")
                        || firstPart.equalsIgnoreCase("100")
                        || ((prefix.equalsIgnoreCase("fc") && firstPart.length() >= 4))
                        || ((prefix.equalsIgnoreCase("fd") && firstPart.length() >= 4))) {
                    PrivateIPv6Check = true;
                }
            }
            return PrivateIPv6Check;
        } else if (address instanceof Inet4Address) {
            String[] ipAddressData = ipAddress.split("\\.");
            int[] ipv4Parts = new int[ipAddressData.length];
            for (int i = 0; i < ipAddressData.length; i++) {
                ipv4Parts[i] = Integer.parseInt(ipAddressData[i].trim());
            }
            switch (ipv4Parts[0]) {
                case 10:
                case 127:
                    return true;
                case 172:
                    return (ipv4Parts[1] >= 16) && (ipv4Parts[1] < 32);
                case 192:
                    return (ipv4Parts[1] == 168);
                case 169:
                    return (ipv4Parts[1] == 254);
            }
        }
        return false;
    }
}
