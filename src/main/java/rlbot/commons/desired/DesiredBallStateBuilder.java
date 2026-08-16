package rlbot.commons.desired;

import rlbot.flat.*;

/**
 * A builder for a {@link DesiredBallStateT} used for game state setting.
 * <p>
 * Components that are not set will keep their current value in-game.
 */
public class DesiredBallStateBuilder {

    private final DesiredBallStateT state;

    public DesiredBallStateBuilder(DesiredBallStateT state) {
        this.state = state;
        if (this.state.getPhysics() == null) {
            this.state.setPhysics(new DesiredPhysicsT());
        }
    }

    /**
     * Set the desired location of the ball.
     */
    public DesiredBallStateBuilder location(Vector3 location) {
        state.getPhysics().setLocation(vector3Partial(location.x(), location.y(), location.z()));
        return this;
    }

    /**
     * Set the desired location of the ball.
     */
    public DesiredBallStateBuilder location(Vector3T location) {
        state.getPhysics().setLocation(vector3Partial(location.getX(), location.getY(), location.getZ()));
        return this;
    }

    /**
     * Set the desired location of the ball.
     */
    public DesiredBallStateBuilder location(float x, float y, float z) {
        state.getPhysics().setLocation(vector3Partial(x, y, z));
        return this;
    }

    /**
     * Set the X value of the desired location of the ball.
     */
    public DesiredBallStateBuilder locationX(float value) {
        ensureLocation().setX(floatT(value));
        return this;
    }

    /**
     * Set the Y value of the desired location of the ball.
     */
    public DesiredBallStateBuilder locationY(float value) {
        ensureLocation().setY(floatT(value));
        return this;
    }

    /**
     * Set the Z value of the desired location of the ball.
     */
    public DesiredBallStateBuilder locationZ(float value) {
        ensureLocation().setZ(floatT(value));
        return this;
    }

    /**
     * Set the desired velocity of the ball.
     */
    public DesiredBallStateBuilder velocity(Vector3 velocity) {
        state.getPhysics().setVelocity(vector3Partial(velocity.x(), velocity.y(), velocity.z()));
        return this;
    }

    /**
     * Set the desired velocity of the ball.
     */
    public DesiredBallStateBuilder velocity(Vector3T velocity) {
        state.getPhysics().setVelocity(vector3Partial(velocity.getX(), velocity.getY(), velocity.getZ()));
        return this;
    }

    /**
     * Set the desired velocity of the ball.
     */
    public DesiredBallStateBuilder velocity(float x, float y, float z) {
        state.getPhysics().setVelocity(vector3Partial(x, y, z));
        return this;
    }

    /**
     * Set the X value of the desired velocity of the ball.
     */
    public DesiredBallStateBuilder velocityX(float value) {
        ensureVelocity().setX(floatT(value));
        return this;
    }

    /**
     * Set the Y value of the desired velocity of the ball.
     */
    public DesiredBallStateBuilder velocityY(float value) {
        ensureVelocity().setY(floatT(value));
        return this;
    }

    /**
     * Set the Z value of the desired velocity of the ball.
     */
    public DesiredBallStateBuilder velocityZ(float value) {
        ensureVelocity().setZ(floatT(value));
        return this;
    }

    /**
     * Set the desired angular velocity of the ball.
     */
    public DesiredBallStateBuilder angularVelocity(Vector3 angularVelocity) {
        state.getPhysics().setAngularVelocity(vector3Partial(angularVelocity.x(), angularVelocity.y(), angularVelocity.z()));
        return this;
    }

    /**
     * Set the desired angular velocity of the ball.
     */
    public DesiredBallStateBuilder angularVelocity(Vector3T angularVelocity) {
        state.getPhysics().setAngularVelocity(vector3Partial(angularVelocity.getX(), angularVelocity.getY(), angularVelocity.getZ()));
        return this;
    }

    /**
     * Set the desired angular velocity of the ball.
     */
    public DesiredBallStateBuilder angularVelocity(float x, float y, float z) {
        state.getPhysics().setAngularVelocity(vector3Partial(x, y, z));
        return this;
    }

    /**
     * Set the X value of the desired angular velocity of the ball.
     */
    public DesiredBallStateBuilder angularVelocityX(float value) {
        ensureAngularVelocity().setX(floatT(value));
        return this;
    }

    /**
     * Set the Y value of the desired angular velocity of the ball.
     */
    public DesiredBallStateBuilder angularVelocityY(float value) {
        ensureAngularVelocity().setY(floatT(value));
        return this;
    }

    /**
     * Set the Z value of the desired angular velocity of the ball.
     */
    public DesiredBallStateBuilder angularVelocityZ(float value) {
        ensureAngularVelocity().setZ(floatT(value));
        return this;
    }

    /**
     * Set the desired rotation of the ball.
     */
    public DesiredBallStateBuilder rotation(Vector3 rotation) {
        state.getPhysics().setRotation(rotatorPartial(rotation.x(), rotation.y(), rotation.z()));
        return this;
    }

    /**
     * Set the desired rotation of the ball.
     */
    public DesiredBallStateBuilder rotation(Vector3T rotation) {
        state.getPhysics().setRotation(rotatorPartial(rotation.getX(), rotation.getY(), rotation.getZ()));
        return this;
    }

    /**
     * Set the desired rotation of the ball.
     */
    public DesiredBallStateBuilder rotation(RotatorT rotation) {
        state.getPhysics().setRotation(rotatorPartial(rotation.getPitch(), rotation.getYaw(), rotation.getRoll()));
        return this;
    }

    /**
     * Set the desired rotation of the ball.
     */
    public DesiredBallStateBuilder rotation(float pitch, float yaw, float roll) {
        state.getPhysics().setRotation(rotatorPartial(pitch, yaw, roll));
        return this;
    }

    /**
     * Set the pitch value of the desired rotation of the ball.
     */
    public DesiredBallStateBuilder rotationPitch(float value) {
        ensureRotation().setPitch(floatT(value));
        return this;
    }

    /**
     * Set the yaw value of the desired rotation of the ball.
     */
    public DesiredBallStateBuilder rotationYaw(float value) {
        ensureRotation().setYaw(floatT(value));
        return this;
    }

    /**
     * Set the roll value of the desired rotation of the ball.
     */
    public DesiredBallStateBuilder rotationRoll(float value) {
        ensureRotation().setRoll(floatT(value));
        return this;
    }

    private Vector3PartialT ensureLocation() {
        var location = state.getPhysics().getLocation();
        if (location == null) {
            location = new Vector3PartialT();
            location.setX(null);
            location.setY(null);
            location.setZ(null);
            state.getPhysics().setLocation(location);
        }
        return location;
    }

    private Vector3PartialT ensureVelocity() {
        var velocity = state.getPhysics().getVelocity();
        if (velocity == null) {
            velocity = new Vector3PartialT();
            velocity.setX(null);
            velocity.setY(null);
            velocity.setZ(null);
            state.getPhysics().setVelocity(velocity);
        }
        return velocity;
    }

    private Vector3PartialT ensureAngularVelocity() {
        var angularVelocity = state.getPhysics().getAngularVelocity();
        if (angularVelocity == null) {
            angularVelocity = new Vector3PartialT();
            angularVelocity.setX(null);
            angularVelocity.setY(null);
            angularVelocity.setZ(null);
            state.getPhysics().setAngularVelocity(angularVelocity);
        }
        return angularVelocity;
    }

    private RotatorPartialT ensureRotation() {
        var rotation = state.getPhysics().getRotation();
        if (rotation == null) {
            rotation = new RotatorPartialT();
            rotation.setPitch(null);
            rotation.setYaw(null);
            rotation.setRoll(null);
            state.getPhysics().setRotation(rotation);
        }
        return rotation;
    }

    private static Vector3PartialT vector3Partial(float x, float y, float z) {
        var partial = new Vector3PartialT();
        partial.setX(floatT(x));
        partial.setY(floatT(y));
        partial.setZ(floatT(z));
        return partial;
    }

    private static RotatorPartialT rotatorPartial(float pitch, float yaw, float roll) {
        var partial = new RotatorPartialT();
        partial.setPitch(floatT(pitch));
        partial.setYaw(floatT(yaw));
        partial.setRoll(floatT(roll));
        return partial;
    }

    private static FloatT floatT(float value) {
        var f = new FloatT();
        f.setVal(value);
        return f;
    }
}
