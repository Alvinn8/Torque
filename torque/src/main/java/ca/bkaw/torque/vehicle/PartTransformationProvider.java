package ca.bkaw.torque.vehicle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Interface for vehicle components that can provide transformations and visual effects for model parts.
 * <p>
 * Components implementing this interface can control the rotation, translation, glowing state,
 * and glow color of specific model parts during rendering.
 */
public interface PartTransformationProvider {

    /**
     * Get the transformation data for a specific model part.
     * 
     * @param partName The name of the model part
     * @param vehicle The vehicle instance
     * @return The transformation data to apply to the part, or null if this component doesn't control this part
     */
    @Nullable
    PartTransform getPartTransform(@NotNull String partName, @NotNull Vehicle vehicle);

    /**
     * Data class containing all transformation and visual effect data for a model part.
     */
    class PartTransform {
        private final @NotNull Quaternionf rotation;
        private final @NotNull Vector3f translation;
        private final boolean glowing;
        private final @Nullable Integer glowColor;

        /**
         * Create a part transform with only rotation.
         * 
         * @param rotation The rotation to apply
         */
        public PartTransform(@NotNull Quaternionf rotation) {
            this(rotation, new Vector3f(), false, null);
        }

        /**
         * Create a part transform with all properties.
         * 
         * @param rotation The rotation to apply
         * @param translation The translation to apply
         * @param glowing Whether the part should glow
         * @param glowColor The glow color (RGB values 0-1), or null for default glow color
         */
        public PartTransform(@NotNull Quaternionf rotation, @NotNull Vector3f translation,
                           boolean glowing, @Nullable Integer glowColor) {
            this.rotation = rotation;
            this.translation = translation;
            this.glowing = glowing;
            this.glowColor = glowColor;
        }

        /**
         * Get the rotation to apply to the part.
         * 
         * @return The rotation, or null if no rotation should be applied
         */
        @NotNull
        public Quaternionf getRotation() {
            return this.rotation;
        }

        /**
         * Get the translation to apply to the part.
         * 
         * @return The translation, or null if no translation should be applied
         */
        @NotNull
        public Vector3f getTranslation() {
            return this.translation;
        }

        /**
         * Check if the part should glow.
         * 
         * @return True if the part should glow
         */
        public boolean isGlowing() {
            return glowing;
        }

        /**
         * Get the glow color for the part.
         * 
         * @return The glow color (RGB values 0-1), or null for default glow color
         */
        @Nullable
        public Integer getGlowColor() {
            return this.glowColor;
        }
    }
}
