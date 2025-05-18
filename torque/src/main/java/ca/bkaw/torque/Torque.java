package ca.bkaw.torque;

import ca.bkaw.torque.platform.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * Main class for Torque.
 */
public class Torque {
    private final @NotNull Platform platform;

    public Torque(@NotNull Platform platform) {
        this.platform = platform;
    }
}
