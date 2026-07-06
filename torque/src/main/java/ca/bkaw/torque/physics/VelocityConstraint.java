package ca.bkaw.torque.physics;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * A single-axis velocity constraint, solved as an impulse applied to a rigid body at a
 * contact point.
 * <p>
 * Used to prevent (or limit) relative velocity along {@code direction} at the contact
 * point, e.g. to stop a tire from slipping sideways, or a vehicle from penetrating a
 * surface. The impulse the constraint may apply can be bounded (e.g. by the friction
 * available at the contact), in which case the constraint is only partially satisfied
 * and the leftover relative velocity is the physical "slip" - there is no need to
 * detect a separate low/high speed case, the bound alone decides how much of the
 * requested correction is actually applied.
 * <p>
 * Constraints are registered on the rigid body and solved together with all other
 * constraints acting on it in an iterative sequential-impulse (projected Gauss-Seidel)
 * pass, so that constraints that affect each other - e.g. a wheel whose correction
 * rotates the vehicle and changes what another wheel or a collision contact sees -
 * negotiate within the same tick instead of fighting across ticks. Each call to
 * {@link #solveIteration} performs one Gauss-Seidel update: it computes the impulse
 * that would drive the relative velocity to the target, clamps the accumulated total
 * impulse to the allowed bounds, and applies only the increment. Iterating over all
 * constraints repeatedly converges toward the simultaneous solution, and because the
 * total impulse is clamped, an unsatisfiable set of constraints degrades gracefully
 * into residual slip rather than diverging.
 * <p>
 * Assumes the other side of the contact (e.g. terrain) is immovable.
 */
public class VelocityConstraint {
    private final Vector3d direction;
    private final Vector3d r; // contact point relative to the body's center of mass
    private final double inverseMass;
    private final Matrix3dc inverseInertiaTensor;
    private final double effectiveMass; // unit: kg

    private double targetVelocity = 0; // unit: meter/second
    private double minImpulse = -Double.MAX_VALUE; // unit: Newton-second
    private double maxImpulse = Double.MAX_VALUE; // unit: Newton-second
    private @Nullable VelocityConstraint frictionOf;
    private double frictionCoefficient;

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
     * Set the relative velocity along the constraint direction that this constraint
     * drives toward. Defaults to zero (no relative motion, e.g. no slip and no
     * penetration with zero restitution).
     *
     * @param targetVelocity The target relative velocity. Unit: meter/second.
     * @return This constraint, for chaining.
     */
    public VelocityConstraint targetVelocity(double targetVelocity) {
        this.targetVelocity = targetVelocity;
        return this;
    }

    /**
     * Bound the total impulse this constraint may apply. For a friction-limited
     * constraint with a known normal load, use symmetric bounds
     * {@code (-limit, limit)}. For a contact that can only push, never pull, use
     * {@code (0, Double.MAX_VALUE)}.
     *
     * @param minImpulse The minimum total impulse. Unit: Newton-second.
     * @param maxImpulse The maximum total impulse. Unit: Newton-second.
     * @return This constraint, for chaining.
     */
    public VelocityConstraint impulseBounds(double minImpulse, double maxImpulse) {
        this.minImpulse = minImpulse;
        this.maxImpulse = maxImpulse;
        return this;
    }

    /**
     * Make this constraint a friction constraint of the given normal (non-penetration)
     * constraint. Each solver iteration, the total impulse is additionally bounded to
     * {@code ±frictionCoefficient × normal.getTotalImpulse()}, re-evaluated against
     * the normal constraint's <em>current</em> accumulated impulse. This couples
     * friction to the actual normal load the solver is computing, rather than to a
     * precomputed assumption, which requires both constraints to be solved in the
     * same iteration loop.
     *
     * @param normal The normal constraint whose accumulated impulse decides the friction limit.
     * @param frictionCoefficient The coefficient of friction. Unitless.
     * @return This constraint, for chaining.
     */
    public VelocityConstraint frictionOf(VelocityConstraint normal, double frictionCoefficient) {
        this.frictionOf = normal;
        this.frictionCoefficient = frictionCoefficient;
        return this;
    }

    /**
     * Warm start this constraint with the impulse it resolved to last tick. The solver
     * applies this impulse up front before iterating, which greatly improves how close
     * to the true solution a fixed number of iterations gets when the constraint
     * persists across ticks (e.g. a wheel that is steadily cornering). The iterations
     * can still reduce or undo the warm-start impulse, since clamping applies to the
     * accumulated total.
     *
     * @param impulse Last tick's total impulse. Unit: Newton-second.
     * @return This constraint, for chaining.
     */
    public VelocityConstraint warmStart(double impulse) {
        this.totalImpulse = Math.max(this.minImpulse, Math.min(this.maxImpulse, impulse));
        return this;
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
     * Perform one Gauss-Seidel iteration: compute the impulse needed to drive the
     * relative velocity to the target, clamp the accumulated total impulse to the
     * allowed bounds, and apply the increment to the given velocity and angular
     * velocity so that subsequently solved constraints see the effect.
     * <p>
     * If the clamp prevents the target from being fully reached, the shortfall is
     * physical slip along {@code direction} - the caller does not need to do anything
     * special to make that happen, it falls out of the clamp.
     *
     * @param velocity The body's linear velocity, updated in place. Unit: meter/second.
     * @param angularVelocity The body's angular velocity, updated in place. Unit: radian/second.
     * @return The incremental impulse that was applied. Unit: Newton-second.
     */
    public double solveIteration(Vector3d velocity, Vector3d angularVelocity) {
        double min = this.minImpulse;
        double max = this.maxImpulse;
        if (this.frictionOf != null) {
            double frictionLimit = Math.abs(this.frictionCoefficient * this.frictionOf.totalImpulse);
            min = Math.max(min, -frictionLimit);
            max = Math.min(max, frictionLimit);
        }

        double currentVelocity = this.getRelativeVelocity(velocity, angularVelocity);
        double impulse = (this.targetVelocity - currentVelocity) * this.effectiveMass;

        double newTotalImpulse = Math.max(min, Math.min(max, this.totalImpulse + impulse));
        double appliedImpulse = newTotalImpulse - this.totalImpulse;
        this.totalImpulse = newTotalImpulse;

        this.applyImpulse(velocity, angularVelocity, appliedImpulse);
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

    /**
     * Get the total impulse this constraint has accumulated. After the solver has run,
     * this is the impulse the constraint actually applied this tick; dividing by the
     * tick delta time gives the equivalent average force, which is useful for
     * debugging and for warm starting next tick's constraint.
     *
     * @return The total impulse. Unit: Newton-second.
     */
    public double getTotalImpulse() {
        return this.totalImpulse;
    }
}
