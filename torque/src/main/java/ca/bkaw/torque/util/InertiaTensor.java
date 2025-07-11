package ca.bkaw.torque.util;

import ca.bkaw.torque.assets.model.ModelElement;
import ca.bkaw.torque.assets.model.ModelElementList;
import ca.bkaw.torque.assets.model.ModelElementRotation;
import org.joml.Matrix3d;
import org.joml.Vector3d;

/**
 * Utility class for calculating the inertia tensor of a model.
 * <p>
 * The inertia tensor is calculated based on the mass and the geometry of the
 * model elements. It is used in physics simulations to determine how the model
 * behaves when torque is applied.
 */
public class InertiaTensor {
    private InertiaTensor() {}

    /**
     * Calculate the inertia tensor for a set of model elements at the origin
     * of the model.
     *
     * @param mass The total mass of the model. Unit: kg
     * @param elements The elements.
     * @return The inertia tensor. Unit: kg*m^2
     */
    public static Matrix3d calculateInertiaTensor(
        double mass,
        ModelElementList elements
    ) {
        Matrix3d inertiaTensor = new Matrix3d();

        double volume = getVolume(elements); // Unit: m^3
        double averageDensity = mass / volume; // Unit: kg/m^3

        for (ModelElement element : elements.getElements()) {
            String name = element.getName();
            if (name != null && name.startsWith(".")) {
                // Skip hidden elements.
                continue;
            }
            double elementVolume = getVolume(element); // Unit: m^3
            double elementMass = averageDensity * elementVolume; // Unit: kg

            Vector3d halfExtents = element.getTo().sub(element.getFrom()).absolute().mul(0.5 / 16); // Unit: m
            Vector3d elementCenter = element.getFrom().add(element.getTo()).mul(0.5 / 16); // Unit: m

            // Calculate the inertia tensor in the local space of the element.
            Matrix3d elementInertiaTensor = new Matrix3d().zero()
                .m00((float) (halfExtents.y * halfExtents.y + halfExtents.z * halfExtents.z))
                .m11((float) (halfExtents.x * halfExtents.x + halfExtents.z * halfExtents.z))
                .m22((float) (halfExtents.x * halfExtents.x + halfExtents.y * halfExtents.y))
                .scale((float) ((1.0 / 12) * elementMass)); // Unit: kg*m^2

            // If we have rotation, we need to calculate where the center ends up after
            // rotation, and we need to rotate the inertia tensor.
            ModelElementRotation rotation = element.getRotation();
            if (rotation != null) {
                // Find rotation matrix.
                Matrix3d rotationMatrix = new Matrix3d().identity();
                double angleRad = Math.toRadians(rotation.getAngle()); // Unit: rad
                switch (rotation.getAxis()) {
                    case X -> rotationMatrix.rotateX(angleRad);
                    case Y -> rotationMatrix.rotateY(angleRad);
                    case Z -> rotationMatrix.rotateZ(angleRad);
                }

                // Move the center since it may move when rotating around the given origin.
                Vector3d origin = rotation.getOrigin().div(16); // Unit: m
                // c_world = R(c_local - o) + o
                Vector3d transformedCenter = elementCenter.sub(origin, new Vector3d()); // Unit: m
                elementCenter = transformedCenter.mul(rotationMatrix).add(origin);

                // Rotate the inertia tensor. Store result in elementInertiaTensor.
                rotationMatrix.mul(elementInertiaTensor, elementInertiaTensor).mul(rotationMatrix.transpose());
            }

            // We now need to move the inertia tensor from the local space of the element
            // to the world space. We use the parallel axis theorem for this. Move by
            // the center of the element.
            // I_world = I_local + m * ( (r^2 * I) - r * r^T )
            // Where in our case, r = the center of the element in world space.
            // And I = the 3x3 identity matrix.
            elementInertiaTensor = elementInertiaTensor.add(
                 (new Matrix3d().identity().scale(elementCenter.lengthSquared()) // r^2 * I
                    .sub(outerProduct(elementCenter, elementCenter))) // - r * r^T
                     .scale(elementMass)
            );

            inertiaTensor.add(elementInertiaTensor);
        }

        return inertiaTensor;
    }

    /**
     * Get the volume in cubic meters of all the elements.
     *
     * @param elements The elements to calculate the volume of.
     * @return The total volume. Unit: m^3
     */
    private static double getVolume(ModelElementList elements) {
        return elements.getElements().stream()
            .filter(element -> element.getName() == null || !element.getName().startsWith("."))
            .mapToDouble(InertiaTensor::getVolume)
            .sum();
    }

    /**
     * Get the volume of an element in cubic meters.
     *
     * @param element The element.
     * @return The volume. Unit: m^3
     */
    private static double getVolume(ModelElement element) {
        // Convert units ("pixels") to meters by dividing by 16.
        Vector3d boxLengths = element.getTo().sub(element.getFrom()).div(16).absolute();
        return boxLengths.x * boxLengths.y * boxLengths.z;
    }

    /**
     * Compose the outer product of two vectors. Or equivalently, a * b^T.
     *
     * @param a The first vector.
     * @param b The second vector.
     * @return The outer product matrix.
     */
    private static Matrix3d outerProduct(Vector3d a, Vector3d b) {
        return new Matrix3d()
            .m00(a.x * b.x)
            .m01(a.x * b.y)
            .m02(a.x * b.z)
            .m10(a.y * b.x)
            .m11(a.y * b.y)
            .m12(a.y * b.z)
            .m20(a.z * b.x)
            .m21(a.z * b.y)
            .m22(a.z * b.z);
    }

    public static Vector3d getCenterOfMass(ModelElementList elements) {
        Vector3d centerOfMass = new Vector3d();
        double volume = getVolume(elements); // Unit: m^3

        for (ModelElement element : elements.getElements()) {
            double elementVolume = getVolume(element); // Unit: m^3
            Vector3d elementCenter = element.getFrom().add(element.getTo()).mul(0.5 / 16); // Unit: m
            centerOfMass.add(elementCenter.mul(elementVolume / volume));
        }
        return centerOfMass;
    }
}
