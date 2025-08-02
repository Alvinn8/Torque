package ca.bkaw.torque.model;

import ca.bkaw.torque.platform.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3fc;

public record VehicleModelPart(
    String name,
    Identifier modelIdentifier,
    float scale,
    @NotNull Vector3fc translation
) {
}
