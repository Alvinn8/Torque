package ca.bkaw.torque.vehicle;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

/**
 * Interface for vehicle components that can provide rotations for model parts.
 * <p>
 * Components implementing this interface can control the rotation of specific
 * model parts during rendering.
 */
public interface PartRotationProvider {
    
    /**
     * Get the rotation for a specific model part.
     * 
     * @param partName The name of the model part
     * @param vehicle The vehicle instance
     * @return The rotation to apply to the part, or null if this component doesn't control this part
     */
    Quaternionf getPartRotation(@NotNull String partName, @NotNull Vehicle vehicle);
}
