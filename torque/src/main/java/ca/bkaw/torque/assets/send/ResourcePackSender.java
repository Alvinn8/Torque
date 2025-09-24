package ca.bkaw.torque.assets.send;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.platform.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Level;

/**
 * A sender responsible for sending resource packs to players.
 * <p>
 * The server can only send resource packs to clients using an HTTP url. Resource
 * pack senders are responsible for crafting a URL to send to clients.
 */
public interface ResourcePackSender {
    /**
     * Send the torque resource pack to the player.
     *
     * @param player The player to send the resource pack to.
     * @param required Whether accepting the resource pack is mandatory.
     * @param prompt The optional plain text prompt to display.
     */
    void send(@NotNull Player player, boolean required, @Nullable String prompt);

    /**
     * Called when the resource pack sender is being removed. Can be used to clean up.
     */
    void remove() throws ReflectiveOperationException;

    /**
     * Utilities and shared code for {@link ResourcePackSender} implementations.
     */
    final class Utils {
        private static final String CHECK_IP_URL = "https://checkip.amazonaws.com/";

        private static String localHostname;
        private static String remoteHostname;

        /**
         * Get the hostname that the player can use to connect to this server.
         * <p>
         * This will be the server's public ip unless the player connected from a local
         * address.
         *
         * @param player The player.
         * @return The hostname the player can use to connect to this server.
         */
        @NotNull
        public static String getHostnameFor(Player player) {
            // Check if the player joined from a local address
            // and in that case return local
            InetSocketAddress socketAddress = player.getAddress();
            if (socketAddress != null) {
                InetAddress address = socketAddress.getAddress();
                if (address.isSiteLocalAddress() || address.isLoopbackAddress()) {
                    return getLocalhost();
                }
            }

            // Otherwise, it's a normal remote player, return the public ip
            return getRemoteHostname();
        }

        /**
         * Return the local ip that can be used to connect to the server from the same
         * network.
         *
         * @return The local ip.
         */
        @NotNull
        private static String getLocalhost() {
            if (localHostname == null) {
                try {
                    localHostname = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    Torque.LOGGER.log(Level.WARNING, "Failed to get local ip for getting the url " +
                        "to send to players when sending resource packs.", e);
                    localHostname = "localhost";
                }
            }
            return localHostname;
        }

        /**
         * Get the server's public ip.
         *
         * @return The remote hostname.
         */
        @NotNull
        private static String getRemoteHostname() {
            if (remoteHostname != null) {
                return remoteHostname;
            }
            String configHostname = null; // config.sender().common().hostname(); // TODO config
            if (configHostname != null) {
                remoteHostname = configHostname;
            } else {
                try (InputStream stream = URI.create(CHECK_IP_URL).toURL().openStream()) {
                    remoteHostname = new String(stream.readAllBytes()).trim();
                } catch (IOException e) {
                    Torque.LOGGER.log(Level.SEVERE, "Failed to get public ip for getting the " +
                        "url to send to players when sending praeter resources.", e);
                    remoteHostname = getLocalhost();
                }
            }
            return remoteHostname;
        }
    }
}
