package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

public class HitboxComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.builder(
            new Identifier("torque", "hitbox")
        )
        .configParser(HitboxComponent::parseConfig)
        .create(HitboxComponent::new);

    public record HitboxConfig(float width, float height) {}

    private static HitboxConfig parseConfig(JsonObject jsonObject) {
        if (!jsonObject.has("width") || !jsonObject.has("height")) {
            throw new IllegalArgumentException("Missing required hitbox config: width or height");
        }
        float width = jsonObject.get("width").getAsFloat();
        float height = jsonObject.get("height").getAsFloat();
        return new HitboxConfig(width, height);
    }

    private final HitboxConfig config;

    public HitboxComponent(Vehicle vehicle, HitboxConfig config, DataInput data) {
        this.config = config;
    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    @Override
    public void tick(Vehicle vehicle) {}

    public HitboxConfig getConfig() {
        return this.config;
    }
}
