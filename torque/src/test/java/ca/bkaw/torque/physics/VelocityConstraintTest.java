package ca.bkaw.torque.physics;

import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VelocityConstraintTest {
    private static final double MASS = 1000; // unit: kilogram
    private static final Vector3d ORIGIN = new Vector3d();

    /**
     * Inertia tensor inverse of zero = infinite rotational inertia, so constraints
     * only affect linear velocity. Keeps the expected values easy to compute by hand.
     */
    private static final Matrix3d NO_ROTATION = new Matrix3d().zero();

    @Test
    void constraintAtCenterOfMassCancelsVelocityInOneIteration() {
        VelocityConstraint constraint = new VelocityConstraint(
            ORIGIN, ORIGIN, new Vector3d(1, 0, 0), 1.0 / MASS, NO_ROTATION
        );
        Vector3d velocity = new Vector3d(3, 0, -2);
        Vector3d angularVelocity = new Vector3d();

        constraint.solveIteration(velocity, angularVelocity);

        assertEquals(0.0, velocity.x, 1e-9);
        assertEquals(-2.0, velocity.z, 1e-9); // other axes untouched
        assertEquals(-3.0 * MASS, constraint.getTotalImpulse(), 1e-6);
    }

    @Test
    void clampedImpulseLeavesResidualSlip() {
        // Only allow enough impulse to cancel 1 m/s of the 3 m/s relative velocity.
        double maxImpulse = MASS * 1.0;
        VelocityConstraint constraint = new VelocityConstraint(
            ORIGIN, ORIGIN, new Vector3d(1, 0, 0), 1.0 / MASS, NO_ROTATION
        ).impulseBounds(-maxImpulse, maxImpulse);
        Vector3d velocity = new Vector3d(3, 0, 0);
        Vector3d angularVelocity = new Vector3d();

        for (int i = 0; i < 8; i++) {
            constraint.solveIteration(velocity, angularVelocity);
        }

        // 2 m/s of slip remains, no matter how many iterations run.
        assertEquals(2.0, velocity.x, 1e-9);
        assertEquals(-maxImpulse, constraint.getTotalImpulse(), 1e-6);
    }

    @Test
    void contactConstraintPushesButNeverPulls() {
        // A contact normal constraint: impulse bounds [0, inf).
        VelocityConstraint contact = new VelocityConstraint(
            ORIGIN, ORIGIN, new Vector3d(0, 1, 0), 1.0 / MASS, NO_ROTATION
        ).impulseBounds(0, Double.MAX_VALUE);

        // Separating: moving away from the surface, must not be pulled back.
        Vector3d velocity = new Vector3d(0, 2, 0);
        Vector3d angularVelocity = new Vector3d();
        contact.solveIteration(velocity, angularVelocity);
        assertEquals(2.0, velocity.y, 1e-9);
        assertEquals(0.0, contact.getTotalImpulse(), 1e-9);

        // Approaching: moving into the surface, must be stopped.
        VelocityConstraint contact2 = new VelocityConstraint(
            ORIGIN, ORIGIN, new Vector3d(0, 1, 0), 1.0 / MASS, NO_ROTATION
        ).impulseBounds(0, Double.MAX_VALUE);
        velocity.set(0, -2, 0);
        contact2.solveIteration(velocity, angularVelocity);
        assertEquals(0.0, velocity.y, 1e-9);
        assertEquals(2.0 * MASS, contact2.getTotalImpulse(), 1e-6);
    }

    @Test
    void offCenterConstraintSplitsCorrectionBetweenLinearAndAngular() {
        // Mass 1, identity inverse inertia, contact 1 m to the side of the center of
        // mass, constraining the z axis: effective mass = 1 / (1/m + angular term) = 0.5.
        VelocityConstraint constraint = new VelocityConstraint(
            new Vector3d(1, 0, 0), ORIGIN, new Vector3d(0, 0, 1), 1.0, new Matrix3d()
        );
        assertEquals(0.5, constraint.getEffectiveMass(), 1e-9);

        Vector3d velocity = new Vector3d(0, 0, 1);
        Vector3d angularVelocity = new Vector3d();
        constraint.solveIteration(velocity, angularVelocity);

        // The relative velocity at the contact point is fully cancelled...
        assertEquals(0.0, constraint.getRelativeVelocity(velocity, angularVelocity), 1e-9);
        // ...by a combination of linear and angular correction, not linear alone.
        assertNotEquals(0.0, velocity.z);
        assertNotEquals(0.0, angularVelocity.y);
    }

    @Test
    void frictionImpulseIsBoundedByNormalImpulse() {
        // A body landing on the ground (2 m/s down) while sliding (4 m/s along x),
        // with friction coefficient 0.5. The normal constraint resolves an impulse of
        // 2 m/s * mass; friction may then use at most half of that, cancelling only
        // 1 m/s of the slide.
        VelocityConstraint normal = new VelocityConstraint(
            ORIGIN, ORIGIN, new Vector3d(0, 1, 0), 1.0 / MASS, NO_ROTATION
        ).impulseBounds(0, Double.MAX_VALUE);
        VelocityConstraint friction = new VelocityConstraint(
            ORIGIN, ORIGIN, new Vector3d(1, 0, 0), 1.0 / MASS, NO_ROTATION
        ).frictionOf(normal, 0.5);

        Vector3d velocity = new Vector3d(4, -2, 0);
        Vector3d angularVelocity = new Vector3d();

        for (int i = 0; i < 8; i++) {
            normal.solveIteration(velocity, angularVelocity);
            friction.solveIteration(velocity, angularVelocity);
        }

        assertEquals(0.0, velocity.y, 1e-9); // penetration stopped
        assertEquals(3.0, velocity.x, 1e-9); // slide reduced by mu * normal impulse only
        assertEquals(2.0 * MASS, normal.getTotalImpulse(), 1e-6);
        assertEquals(-1.0 * MASS, friction.getTotalImpulse(), 1e-6);
    }

    @Test
    void warmStartIsAppliedAndCanBeUndone() {
        // Warm start with last tick's impulse, but this tick the body is not moving,
        // so the iterations must undo the warm-started impulse again.
        VelocityConstraint constraint = new VelocityConstraint(
            ORIGIN, ORIGIN, new Vector3d(1, 0, 0), 1.0 / MASS, NO_ROTATION
        ).warmStart(-2.0 * MASS);
        Vector3d velocity = new Vector3d();
        Vector3d angularVelocity = new Vector3d();

        // The solver applies the warm-start impulse up front...
        constraint.applyImpulse(velocity, angularVelocity, constraint.getTotalImpulse());
        assertEquals(-2.0, velocity.x, 1e-9);

        // ...and the iterations correct it back to zero.
        for (int i = 0; i < 8; i++) {
            constraint.solveIteration(velocity, angularVelocity);
        }
        assertEquals(0.0, velocity.x, 1e-9);
        assertEquals(0.0, constraint.getTotalImpulse(), 1e-6);
    }

    @Test
    void twoOpposingConstraintsConvergeInsteadOfFighting() {
        // Two constraints on opposite sides of the body, both wanting zero lateral
        // velocity at their point, with the body also rotating. Solving them
        // iteratively against shared state must converge to a solution both accept
        // (here: no linear drift and no rotation), rather than overshooting.
        Matrix3d identityInertiaInverse = new Matrix3d();
        VelocityConstraint front = new VelocityConstraint(
            new Vector3d(0, 0, -1), ORIGIN, new Vector3d(1, 0, 0), 1.0, identityInertiaInverse
        );
        VelocityConstraint back = new VelocityConstraint(
            new Vector3d(0, 0, 1), ORIGIN, new Vector3d(1, 0, 0), 1.0, identityInertiaInverse
        );

        Vector3d velocity = new Vector3d(1, 0, 0);
        Vector3d angularVelocity = new Vector3d(0, 0.5, 0);

        for (int i = 0; i < 16; i++) {
            front.solveIteration(velocity, angularVelocity);
            back.solveIteration(velocity, angularVelocity);
        }

        assertEquals(0.0, front.getRelativeVelocity(velocity, angularVelocity), 1e-6);
        assertEquals(0.0, back.getRelativeVelocity(velocity, angularVelocity), 1e-6);
        assertTrue(Math.abs(velocity.x) < 1e-6);
        assertTrue(Math.abs(angularVelocity.y) < 1e-6);
    }
}
