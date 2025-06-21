package ca.bkaw.torque.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.net.InetSocketAddress;
import java.util.UUID;

public interface Player {
    /**
     * Send a resource pack to the player.
     *
     * @param id The unique identifier for the resource pack.
     * @param url The url the client should send a request to.
     * @param hash The SHA-1 hash of the resource pack file.
     * @param required Whether the client must accept the resource pack.
     * @param prompt The optional plain text text prompt so show the user.
     */
    void sendResourcePack(UUID id, String url, byte[] hash, boolean required, @Nullable String prompt);

    /**
     * Get the IP address the player connects from.
     *
     * @return The IP address, or null if unknown.
     */
    @Nullable InetSocketAddress getAddress();

    /**
     * Get the player's movement input.
     *
     * @param input The input object to fill with the player's input.
     */
    void getInput(@NotNull Input input);

    /**
     * Mount the player to an entity.
     *
     * @param entity The entity to mount.
     */
    void mountVehicle(ItemDisplay entity);

    /**
     * Dismount from the vehicle if the player is in one.
     */
    void dismountVehicle();

    /**
     * Get the player's current position in the world.
     *
     * @return The position.
     */
    @NotNull Vector3d getPosition();

}
