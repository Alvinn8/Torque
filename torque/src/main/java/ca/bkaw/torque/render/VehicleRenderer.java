package ca.bkaw.torque.render;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.components.RigidBodyComponent;
import ca.bkaw.torque.components.SeatsComponent;
import ca.bkaw.torque.model.VehicleModelPart;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.Player;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.tags.SeatTags;
import ca.bkaw.torque.util.Debug;
import ca.bkaw.torque.vehicle.PartTransformationProvider;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleRenderer {
    private static final Quaternionf ROTATE_Y_180 = new Quaternionf().rotateAxis((float) Math.PI, 0, 1, 0);

    private final @NotNull Vehicle vehicle;
    private final @NotNull RenderEntity primary;
    private final Map<VehicleModelPart, RenderEntity> partEntities = new HashMap<>();
    private final @Nullable SeatTags.Seat viewportSeat;
    private final Map<SeatTags.Seat, RenderEntity> seatEntities = new HashMap<>();

    // Cached from RigidBodyComponent
    private World vehicleWorld;
    private Vector3dc vehiclePosition;
    private Quaternionfc vehicleOrientation;

    public record RenderEntity(@NotNull ItemDisplay display, @NotNull Matrix4f transformation) {}

    public VehicleRenderer(@NotNull Vehicle vehicle, ItemDisplay primaryEntity) {
        this.vehicle = vehicle;

        this.primary = new RenderEntity(
            primaryEntity,
            new Matrix4f()
        );
        this.primary.display.setTeleportDuration(1);
        this.primary.display.setInterpolationDuration(1);

        for (VehicleModelPart vehiclePart : vehicle.getType().model().getParts()) {
            ItemDisplay partEntity = primaryEntity.getWorld().spawnItemDisplay(primaryEntity.getPosition());
            partEntity.setItem(
                vehicle.getTorque().getPlatform().createModelItem(vehiclePart.modelIdentifier())
            );
            partEntity.setTeleportDuration(1);
            partEntity.setInterpolationDuration(1);
            partEntity.mountVehicle(primaryEntity);
            this.partEntities.put(vehiclePart, new RenderEntity(partEntity, new Matrix4f()));
            vehicle.getTorque().getVehicleManager().setVehiclePart(partEntity, vehicle);
        }

        List<SeatTags.Seat> seats = vehicle.getType().model().getTagData(SeatTags.class).orElse(List.of());
        this.viewportSeat = seats.isEmpty() ? null : seats.getFirst();
    }

    public void setup(@NotNull Torque torque) {
        Identifier modelIdentifier = this.vehicle.getType().model().getIdentifier();
        this.primary.display.setItem(torque.getPlatform().createModelItem(new Identifier(modelIdentifier.namespace(), modelIdentifier.key() + "/primary")));
        this.render();
    }

    public @NotNull Vehicle getVehicle() {
        return this.vehicle;
    }

    /**
     * Get the primary entity of the vehicle.
     * <p>
     * The primary entity is where the vehicle is serialized, and is the entity that
     * renders the primary vehicle part.
     *
     * @return The entity.
     */
    @NotNull
    public ItemDisplay getPrimaryEntity() {
        return this.primary.display;
    }

    private Vector3f getSeatTranslation(@NotNull SeatTags.Seat seat) {
        return seat.translation()
            .negate(new Vector3f())
            .rotate(ROTATE_Y_180)
            .rotate(this.vehicleOrientation, new Vector3f());
    }

    /**
     * Get the transformation data for a specific model part by checking all PartTransformationProvider components.
     * 
     * @param partName The name of the model part
     * @return The combined transformation data to apply
     */
    @NotNull
    private PartTransformationProvider.PartTransform getPartTransform(@NotNull String partName) {
        for (VehicleComponent component : this.vehicle.getComponents()) {
            if (component instanceof PartTransformationProvider provider) {
                PartTransformationProvider.PartTransform transform = provider.getPartTransform(partName, this.vehicle);
                if (transform != null) {
                    return transform;
                }
            }
        }
        return new PartTransformationProvider.PartTransform(new Quaternionf(), new Vector3f(), false, null);
    }

    public void render() {
        RigidBodyComponent rigidBody = this.vehicle.getComponent(RigidBodyComponent.class).orElseThrow();

        this.vehicleWorld = rigidBody.getWorld();
        this.vehiclePosition = rigidBody.getPosition();
        this.vehicleOrientation = rigidBody.getOrientation();

        Vector3f viewportTranslation = this.viewportSeat != null
            ? this.getSeatTranslation(this.viewportSeat)
            : new Vector3f();

        VehicleModelPart primaryPart = this.vehicle.getType().model().getPrimary();

        // Transformations apply in reverse order because
        // of how matrix multiplication works
        this.primary.transformation.identity()
            .translate(viewportTranslation)
            .rotate(this.vehicleOrientation)
            .translate(primaryPart.translation())
            .scale(primaryPart.scale())
            .translate(0.0f, 0.5f, 0.0f)
            .rotate(ROTATE_Y_180)
            .translate((float) Math.random() * 0.0001f, 0, 0)
        ;
        this.primary.display.setTransformation(this.primary.transformation);
        this.primary.display.setPosition(this.vehiclePosition.sub(viewportTranslation, new Vector3d()));
        this.primary.display.setStartInterpolation(0);

        // Perform part rendering.
        for (Map.Entry<VehicleModelPart, RenderEntity> entry : this.partEntities.entrySet()) {
            VehicleModelPart modelPart = entry.getKey();
            RenderEntity partEntity = entry.getValue();
            
            // Get the rotation for this part from components
            PartTransformationProvider.PartTransform partTransform = this.getPartTransform(modelPart.name());

            partEntity.display.setGlowing(partTransform.isGlowing());
            Integer glowColor = partTransform.getGlowColor();
            if (glowColor != null) {
                partEntity.display.setGlowColor(glowColor);
            }

            partEntity.transformation.identity()
                .translate(viewportTranslation)
                .rotate(this.vehicleOrientation)
                .translate(modelPart.translation())
                .scale(modelPart.scale())
                .translate(0.0f, 0.5f, 0.0f)
                .rotate(partTransform.getRotation()) // Apply component-controlled rotation
                .rotate(ROTATE_Y_180)
                .translate((float) Math.random() * 0.0001f, 0, 0)
            ;
            partEntity.display.setTransformation(partEntity.transformation);
            partEntity.display.setStartInterpolation(0);
        }

        // Perform seat rendering.
        this.vehicle.getComponent(SeatsComponent.class).ifPresent(this::renderSeats);
    }

    private void renderSeats(SeatsComponent seats) {
        int count = 0;
        for (var entry : seats.getPassengerData().entrySet()) {
            SeatTags.Seat seat = entry.getKey();
            SeatsComponent.PassengerData passengerData = entry.getValue();
            if (!passengerData.isValid()) {
                continue;
            }
            if (seat == this.viewportSeat) {
                // Skip the viewport seat, it is already handled by the primary display.
                // Make sure the passenger is mounted to the primary display.
                if (!this.seatEntities.containsKey(seat)) {
                    this.seatEntities.put(seat, this.primary);
                    passengerData.passenger().mountVehicle(this.primary.display);
                }
                continue;
            }
            count++;

            Vector3d seatPosition = new Vector3d(this.vehiclePosition)
                .add(this.getSeatTranslation(seat));

            RenderEntity renderEntity = this.seatEntities.get(seat);
            if (renderEntity == null) {
                // Create a new render entity for this seat
                ItemDisplay display = this.vehicleWorld.spawnItemDisplay(seatPosition);
                this.vehicle.getTorque().getVehicleManager().setVehiclePart(display, this.vehicle);
                display.setTeleportDuration(1);
                renderEntity = new RenderEntity(display, new Matrix4f());
                this.seatEntities.put(seat, renderEntity);
                passengerData.passenger().mountVehicle(display);
                Debug.print("Creating seat now");
            }

            renderEntity.display.setPosition(seatPosition);
            renderEntity.display.setStartInterpolation(0);
        }

        if (count < this.seatEntities.size()) {
            // Remove any seat entities that are no longer needed
            var iter = this.seatEntities.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                SeatTags.Seat seat = entry.getKey();
                RenderEntity renderEntity = entry.getValue();
                if (seats.getPassengerData().containsKey(seat)) {
                    continue;
                }
                if (renderEntity != this.primary) {
                    // Do not remove primary, but remove any other seat entities.
                    this.vehicle.getTorque().getVehicleManager().setVehiclePart(renderEntity.display, null);
                    renderEntity.display.remove();
                }
                iter.remove();
            }
        }
    }

    public void passengerChanged(@NotNull SeatTags.Seat seat, @Nullable Player passenger) {

    }

}
