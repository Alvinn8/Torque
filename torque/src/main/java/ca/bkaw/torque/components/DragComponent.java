package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class DragComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "drag"),
        DragComponent::new
    );

    public DragComponent(Vehicle vehicle, DataInput data) {}

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {

    }

    @Override
    public void tick(Vehicle vehicle) {
        vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
            // Drag
            Vector3dc position = rbc.getPosition();
            Vector3d velocity = rbc.getVelocity();
            double velocitySquared = velocity.lengthSquared();
            if (velocitySquared < 0.01) {
                // No drag if the vehicle is not moving. Avoid divide by zero when normalizing.
                return;
            }
            double dragCoefficient = 0.5; // unitless
            double crossSectionalArea = 2.0; // unit: m^2 (assumed frontal area)
            double density = 1.225; // unit: kg/m^3 (air density)
            double dragForceMagnitude = 0.5 * dragCoefficient * density * crossSectionalArea * velocitySquared;
            Vector3d dragForce = new Vector3d(velocity).normalize().negate().mul(dragForceMagnitude);
            rbc.addForce(dragForce, position);

            Vector3d angularVelocity = rbc.getAngularVelocity();
            if (angularVelocity.lengthSquared() < 0.1) {
                angularVelocity.mul(0.2f);
            } else {
                angularVelocity.mul(0.8f);
            }
            rbc.setAngularVelocity(angularVelocity);
        });
    }
}
