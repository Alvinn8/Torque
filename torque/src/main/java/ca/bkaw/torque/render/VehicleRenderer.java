package ca.bkaw.torque.render;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.components.RigidBodyComponent;
import ca.bkaw.torque.components.SeatsComponent;
import ca.bkaw.torque.model.Seat;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.Player;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.vehicle.Vehicle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleRenderer {
    private static final Quaternionf ROTATE_Y_180 = new Quaternionf().rotateAxis((float) Math.PI, 0, 1, 0);

    private final @NotNull Vehicle vehicle;
    private final @NotNull RenderEntity primary;
    private final @Nullable Seat viewportSeat;
    private final Map<Seat, RenderEntity> seatEntities = new HashMap<>();

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

        List<Seat> seats = vehicle.getType().model().getSeats();
        this.viewportSeat = seats.isEmpty() ? null : seats.getFirst();
    }

    public void setup(@NotNull Torque torque) {
        this.primary.display.setItem(torque.getPlatform().createModelItem(new Identifier("torque", "vehicle/car/primary")));
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

    private Vector3f getSeatTranslation(@NotNull Seat seat) {
        return seat.getTranslation()
            .negate(new Vector3f())
            .rotate(ROTATE_Y_180)
            .rotate(this.vehicleOrientation, new Vector3f());
    }

    public void render() {
        RigidBodyComponent rigidBody = this.vehicle.getComponent(RigidBodyComponent.class).orElseThrow();

        this.vehicleWorld = rigidBody.getWorld();
        this.vehiclePosition = rigidBody.getPosition();
        this.vehicleOrientation = rigidBody.getOrientation();

        Vector3f viewportTranslation = this.viewportSeat != null
            ? this.getSeatTranslation(this.viewportSeat)
            : new Vector3f();

        // Transformations apply in reverse order because
        // of how matrix multiplication works
        this.primary.transformation.identity()
            .translate(viewportTranslation)
            .rotate(this.vehicleOrientation)
            .translate(this.vehicle.getType().model().getTranslation())
            .scale((float) this.vehicle.getType().model().getScale())
            .translate(0.0f, 0.5f, 0.0f)
            .rotate(ROTATE_Y_180)
        ;
        this.primary.display.setTransformation(this.primary.transformation);
        this.primary.display.setPosition(this.vehiclePosition.sub(viewportTranslation, new Vector3d()));
        this.primary.display.setStartInterpolation(0);

        // Perform seat rendering.
        this.vehicle.getComponent(SeatsComponent.class).ifPresent(this::renderSeats);
    }

    private void renderSeats(SeatsComponent seats) {
        int count = 0;
        for (var entry : seats.getPassengerData().entrySet()) {
            Seat seat = entry.getKey();
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
                display.setTeleportDuration(1);
                renderEntity = new RenderEntity(display, new Matrix4f());
                this.seatEntities.put(seat, renderEntity);
                passengerData.passenger().mountVehicle(display);
                System.out.println("Creating seat now");
            }

            renderEntity.display.setPosition(seatPosition);
            renderEntity.display.setStartInterpolation(0);
        }

        if (count < this.seatEntities.size()) {
            // Remove any seat entities that are no longer needed
            var iter = this.seatEntities.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                Seat seat = entry.getKey();
                RenderEntity renderEntity = entry.getValue();
                if (seats.getPassengerData().containsKey(seat)) {
                    continue;
                }
                if (renderEntity != this.primary) {
                    // Do not remove primary, but remove any other seat entities.
                    renderEntity.display.remove();
                }
                iter.remove();
            }
        }
    }

    public void passengerChanged(@NotNull Seat seat, @Nullable Player passenger) {

    }

}
