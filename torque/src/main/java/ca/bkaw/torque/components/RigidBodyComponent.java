package ca.bkaw.torque.components;

import ca.bkaw.torque.vehicle.VehicleComponent;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class RigidBodyComponent implements VehicleComponent {
    // Properties
    private double mass;
    private Matrix3d initialInertiaTensorInverse;

    // Position and velocity
    private Vector3d position;
    private Vector3d velocity;
    private Quaterniond orientation;
    private Vector3d angularVelocity;

    // Accumulated each frame
    private Vector3d netForce;
    private Vector3d netTorque;
}
