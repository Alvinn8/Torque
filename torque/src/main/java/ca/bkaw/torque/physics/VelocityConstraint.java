package ca.bkaw.torque.physics;

import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * A single-axis velocity constraint, solved as an impulse applied to a rigid body at a
 * contact point.
 * <p>
 * Used to prevent (or limit) relative velocity along {@code direction} at the contact
 * point, e.g. to stop a tire from slipping sideways, or a vehicle from penetrating a
 * surface. The impulse needed to fully satisfy the constraint can be bounded (e.g. by
 * the friction available at the contact), in which case the constraint is only
 * partially satisfied and the leftover relative velocity is the physical "slip" -
 * there is no need to detect a separate low/high speed case, the bound alone decides
 * how much of the requested correction is actually applied.
 * <p>
 * Assumes the other side of the contact (e.g. terrain) is immovable.
 */
public class VelocityConstraint {
    private final Vector3d direction;
    private final Vector3d r; // contact point relative to the body's center of mass
    private final double inverseMass;
    private final Matrix3dc inverseInertiaTensor;
    private final double effectiveMass; // unit: kg

    private double totalImpulse = 0; // unit: Newton-second

    /**
     * @param point The contact point, in world coordinates. Unit: meter.
     * @param centerOfMass The body's center of mass, in world coordinates. Unit: meter.
     * @param direction The (unit) axis this constraint acts along, in world coordinates.
     * @param inverseMass The inverse of the body's mass. Unit: 1/kilogram.
     * @param inverseInertiaTensor The body's inertia tensor inverse, in world coordinates.
     */
    public VelocityConstraint(
        Vector3dc point,
        Vector3dc centerOfMass,
        Vector3dc direction,
        double inverseMass,
        Matrix3dc inverseInertiaTensor
    ) {
        this.direction = new Vector3d(direction);
        this.r = new Vector3d(point).sub(centerOfMass);
        this.inverseMass = inverseMass;
        this.inverseInertiaTensor = inverseInertiaTensor;

        double angularTerm = this.direction.dot(
            new Vector3d(this.r).cross(this.direction)
                .mul(inverseInertiaTensor)
                .cross(this.r, new Vector3d())
        );
        this.effectiveMass = 1.0 / (inverseMass + angularTerm);
    }

    /**
     * Get the current relative velocity at the contact point along the constraint
     * direction: {@code (velocity + angularVelocity × r) · direction}.
     *
     * @param velocity The body's linear velocity. Unit: meter/second.
     * @param angularVelocity The body's angular velocity. Unit: radian/second.
     * @return The relative velocity. Unit: meter/second.
     */
    public double getRelativeVelocity(Vector3dc velocity, Vector3dc angularVelocity) {
        Vector3d pointVelocity = new Vector3d(angularVelocity).cross(this.r).add(velocity);
        return pointVelocity.dot(this.direction);
    }

    /**
     * Solve for the impulse needed to drive the relative velocity to
     * {@code targetVelocity}, clamping the total impulse this constraint has applied
     * to {@code [minImpulse, maxImpulse]}.
     * <p>
     * If the clamp prevents the target from being fully reached, the shortfall is
     * physical slip along {@code direction} - the caller does not need to do anything
     * special to make that happen, it falls out of the clamp.
     * <p>
     * This does not modify {@code velocity}/{@code angularVelocity}; the caller
     * decides what to do with the returned impulse (e.g. convert it to an equivalent
     * force, or apply it immediately via {@link #applyImpulse}).
     *
     * @param velocity The body's current linear velocity. Unit: meter/second.
     * @param angularVelocity The body's current angular velocity. Unit: radian/second.
     * @param targetVelocity The relative velocity to aim for. Unit: meter/second.
     * @param minImpulse The minimum total impulse this constraint may apply. Unit: Newton-second.
     * @param maxImpulse The maximum total impulse this constraint may apply. Unit: Newton-second.
     * @return The incremental impulse to apply as a result of this call. Unit: Newton-second.
     */
    public double solve(Vector3dc velocity, Vector3dc angularVelocity, double targetVelocity, double minImpulse, double maxImpulse) {
        double currentVelocity = this.getRelativeVelocity(velocity, angularVelocity);
        double impulse = (targetVelocity - currentVelocity) * this.effectiveMass;

        double newTotalImpulse = Math.max(minImpulse, Math.min(maxImpulse, this.totalImpulse + impulse));
        double appliedImpulse = newTotalImpulse - this.totalImpulse;
        this.totalImpulse = newTotalImpulse;
        return appliedImpulse;
    }

    /**
     * Apply an impulse along the constraint direction to the given velocity and
     * angular velocity, accounting for torque at the contact point.
     *
     * @param velocity The linear velocity to update.
     * @param angularVelocity The angular velocity to update.
     * @param impulseMagnitude The impulse to apply. Unit: Newton-second.
     */
    public void applyImpulse(Vector3d velocity, Vector3d angularVelocity, double impulseMagnitude) {
        Vector3d impulse = new Vector3d(this.direction).mul(impulseMagnitude);
        velocity.add(new Vector3d(impulse).mul(this.inverseMass));
        angularVelocity.add(new Vector3d(this.r).cross(impulse).mul(this.inverseInertiaTensor));
    }

    public Vector3dc getDirection() {
        return this.direction;
    }

    public double getEffectiveMass() {
        return this.effectiveMass;
    }

    public double getTotalImpulse() {
        return this.totalImpulse;
    }
}
