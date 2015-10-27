/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.renderer.googlecast.server;

import android.net.NetworkInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * This class derived from cling's NetworkAddressFactory
 * Created by drew on 10/31/15.
 */
public class NetworkUtil {

    static List<InterfaceAddress> getInterfaceAddresses(NetworkInterface networkInterface) {
        return networkInterface.getInterfaceAddresses();
    }

    static List<InetAddress> getInetAddresses(NetworkInterface networkInterface) {
        return Collections.list(networkInterface.getInetAddresses());
    }

    public static InetAddress getBindAddressInSubnetOf(List<NetworkInterface> networkInterfaces,
                                                       List<InetAddress> bindAddresses,
                                                       InetAddress inetAddress) {
        for (NetworkInterface iface : networkInterfaces) {
            for (InterfaceAddress ifaceAddress : getInterfaceAddresses(iface)) {

                if (ifaceAddress == null || !bindAddresses.contains(ifaceAddress.getAddress())) {
                    continue;
                }

                if (isInSubnet(
                        inetAddress.getAddress(),
                        ifaceAddress.getAddress().getAddress(),
                        ifaceAddress.getNetworkPrefixLength())
                        ) {
                    return ifaceAddress.getAddress();
                }
            }

        }
        return null;
    }

    static boolean isInSubnet(byte[] ip, byte[] network, short prefix) {
        if (ip.length != network.length) {
            return false;
        }

        if (prefix / 8 > ip.length) {
            return false;
        }

        int i = 0;
        while (prefix >= 8 && i < ip.length) {
            if (ip[i] != network[i]) {
                return false;
            }
            i++;
            prefix -= 8;
        }
        if(i == ip.length) return true;
        final byte mask = (byte) ~((1 << 8 - prefix) - 1);

        return (ip[i] & mask) == (network[i] & mask);
    }

    public static List<NetworkInterface> discoverNetworkInterfaces() {
        try {
            List<NetworkInterface> networkInterfaces = new ArrayList<>();
            Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaceEnumeration)) {
                //displayInterfaceInformation(iface);

//                Timber.v("Analyzing network interface: " + iface.getDisplayName());
                if (isUsableNetworkInterface(iface)) {
                    Timber.v("Discovered usable network interface: " + iface.getDisplayName());
                    networkInterfaces.add(iface);
                } else {
//                    Timber.v("Ignoring non-usable network interface: " + iface.getDisplayName());
                }
            }
            return networkInterfaces;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not not analyze local network interfaces: " + ex, ex);
        }
    }

    /**
     * Validation of every discovered network interface.
     * <p>
     * Override this method to customize which network interfaces are used.
     * </p>
     * <p>
     * The given implementation ignores interfaces which are
     * </p>
     * <ul>
     * <li>loopback (yes, we do not bind to lo0)</li>
     * <li>down</li>
     * <li>have no bound IP addresses</li>
     * <li>named "vmnet*" (OS X VMWare does not properly stop interfaces when it quits)</li>
     * <li>named "vnic*" (OS X Parallels interfaces should be ignored as well)</li>
     * <li>named "*virtual*" (VirtualBox interfaces, for example</li>
     * <li>named "ppp*"</li>
     * </ul>
     *
     * @param iface The interface to validate.
     * @return True if the given interface matches all validation criteria.
     * @throws Exception If any validation test failed with an un-recoverable error.
     */
    protected static boolean isUsableNetworkInterface(NetworkInterface iface) throws Exception {
        if (!iface.isUp()) {
            Timber.v("Skipping network interface (down): " + iface.getDisplayName());
            return false;
        }

        if (getInetAddresses(iface).size() == 0) {
            Timber.v("Skipping network interface without bound IP addresses: " + iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ENGLISH).startsWith("vmnet") ||
                (iface.getDisplayName() != null &&  iface.getDisplayName().toLowerCase(Locale.ENGLISH).contains("vmnet"))) {
            Timber.v("Skipping network interface (VMWare): " + iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ENGLISH).startsWith("vnic")) {
            Timber.v("Skipping network interface (Parallels): " + iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ENGLISH).contains("virtual")) {
            Timber.v("Skipping network interface (named '*virtual*'): " + iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ENGLISH).startsWith("ppp")) {
            Timber.v("Skipping network interface (PPP): " + iface.getDisplayName());
            return false;
        }

        if (iface.isLoopback()) {
            Timber.v("Skipping network interface (ignoring loopback): " + iface.getDisplayName());
            return false;
        }

        return true;
    }

    public static List<InetAddress> discoverBindAddresses(List<NetworkInterface> networkInterfaces) {
        try {
            List<InetAddress> bindAddresses = new ArrayList<>();
            Iterator<NetworkInterface> it = networkInterfaces.iterator();
            while (it.hasNext()) {
                NetworkInterface networkInterface = it.next();

                Timber.v("Discovering addresses of interface: " + networkInterface.getDisplayName());
                int usableAddresses = 0;
                for (InetAddress inetAddress : getInetAddresses(networkInterface)) {
                    if (inetAddress == null) {
                        Timber.w("Network has a null address: " + networkInterface.getDisplayName());
                        continue;
                    }

                    if (isUsableAddress(networkInterface, inetAddress)) {
                        Timber.v("Discovered usable network interface address: " + inetAddress.getHostAddress());
                        usableAddresses++;
                        bindAddresses.add(inetAddress);
                    } else {
                        Timber.v("Ignoring non-usable network interface address: " + inetAddress.getHostAddress());
                    }
                }

                if (usableAddresses == 0) {
                    Timber.v("Network interface has no usable addresses, removing: " + networkInterface.getDisplayName());
                    it.remove();
                }
            }
            return bindAddresses;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not not analyze local network interfaces: " + ex, ex);
        }
    }

    /**
     * Validation of every discovered local address.
     * <p>
     * Override this method to customize which network addresses are used.
     * </p>
     * <p>
     * The given implementation ignores addresses which are
     * </p>
     * <ul>
     * <li>not IPv4</li>
     * <li>the local loopback (yes, we ignore 127.0.0.1)</li>
     * </ul>
     *
     * @param networkInterface The interface to validate.
     * @param address The address of this interface to validate.
     * @return True if the given address matches all validation criteria.
     */
    static boolean isUsableAddress(NetworkInterface networkInterface, InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            Timber.v("Skipping unsupported non-IPv4 address: " + address);
            return false;
        }

        if (address.isLoopbackAddress()) {
            Timber.v("Skipping loopback address: " + address);
            return false;
        }

        return true;
    }

    public static void logInterfaceInformation(NetworkInterface networkInterface) throws SocketException {
        StringBuilder sb = new StringBuilder(500);
        sb.append("---------------------------------------------------------------------------------\n");
        sb.append(String.format("Interface display name: %s\n", networkInterface.getDisplayName()));
        if (networkInterface.getParent() != null)
            sb.append(String.format("Parent Info: %s\n", networkInterface.getParent()));
        sb.append(String.format("Name: %s\n", networkInterface.getName()));

        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            sb.append(String.format("InetAddress: %s\n", inetAddress));
        }

        List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();

        for (InterfaceAddress interfaceAddress : interfaceAddresses) {
            if (interfaceAddress == null) {
                sb.append("Skipping null InterfaceAddress!\n");
                continue;
            }
            sb.append(" Interface Address\n");
            sb.append("  Address: ").append(interfaceAddress.getAddress()).append("\n");
            sb.append("  Broadcast: ").append(interfaceAddress.getBroadcast()).append("\n");
            sb.append("  Prefix length: ").append(interfaceAddress.getNetworkPrefixLength()).append("\n");
        }

        Enumeration<NetworkInterface> subIfs = networkInterface.getSubInterfaces();

        for (NetworkInterface subIf : Collections.list(subIfs)) {
            if (subIf == null) {
                sb.append("Skipping null NetworkInterface sub-interface\n");
                continue;
            }
            sb.append(String.format("\tSub Interface Display name: %s\n", subIf.getDisplayName()));
            sb.append(String.format("\tSub Interface Name: %s\n", subIf.getName()));
        }
        sb.append(String.format("Up? %s\n", networkInterface.isUp()));
        sb.append(String.format("Loopback? %s\n", networkInterface.isLoopback()));
        sb.append(String.format("PointToPoint? %s\n", networkInterface.isPointToPoint()));
        sb.append(String.format("Supports multicast? %s\n", networkInterface.supportsMulticast()));
        sb.append(String.format("Virtual? %s\n", networkInterface.isVirtual()));
        sb.append(String.format("Hardware address: %s\n", Arrays.toString(networkInterface.getHardwareAddress())));
        sb.append(String.format("MTU: %s\n", networkInterface.getMTU()));
        Timber.i(sb.toString());
    }

}
