package rlbot.commons.math;

import rlbot.flat.Vector3T;

/**
 * An immutable three dimensional vector with single precision components.
 */
public record Vector3(float x, float y, float z) {

    /**
     * The zero vector.
     */
    public static final Vector3 ZERO = new Vector3(0, 0, 0);

    /**
     * Creates a {@link Vector3} from a flatbuffer {@link Vector3T}.
     *
     * @param vector the flatbuffer vector to convert.
     * @return the resulting vector.
     */
    public static Vector3 fromVector3T(Vector3T vector) {
        return new Vector3(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * @param other the vector to add.
     * @return the sum of this vector and the given vector.
     */
    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    /**
     * @param x the x component to add.
     * @param y the y component to add.
     * @param z the z component to add.
     * @return the sum of this vector and the given components.
     */
    public Vector3 add(float x, float y, float z) {
        return new Vector3(this.x + x, this.y + y, this.z + z);
    }

    /**
     * @param other the vector to subtract.
     * @return the difference between this vector and the given vector.
     */
    public Vector3 sub(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    /**
     * @param x the x component to subtract.
     * @param y the y component to subtract.
     * @param z the z component to subtract.
     * @return the difference between this vector and the given components.
     */
    public Vector3 sub(float x, float y, float z) {
        return new Vector3(this.x - x, this.y - y, this.z - z);
    }

    /**
     * @param scalar the scalar to multiply by.
     * @return this vector multiplied by the given scalar.
     */
    public Vector3 mul(float scalar) {
        return new Vector3(x * scalar, y * scalar, z * scalar);
    }

    /**
     * @param other the vector to multiply by.
     * @return this vector multiplied component-wise by the given vector.
     */
    public Vector3 mul(Vector3 other) {
        return new Vector3(x * other.x, y * other.y, z * other.z);
    }

    /**
     * @param scalar the scalar to divide by.
     * @return this vector divided by the given scalar.
     */
    public Vector3 div(float scalar) {
        return new Vector3(x / scalar, y / scalar, z / scalar);
    }

    /**
     * @return this vector negated.
     */
    public Vector3 negate() {
        return new Vector3(-x, -y, -z);
    }

    /**
     * @return the squared length of this vector.
     */
    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    /**
     * @return the length of this vector.
     */
    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    /**
     * @param other the other vector.
     * @return the distance between this vector and the given vector.
     */
    public float distanceTo(Vector3 other) {
        return other.sub(this).length();
    }

    /**
     * @param other the other vector.
     * @return the squared distance between this vector and the given vector.
     */
    public float distanceToSquared(Vector3 other) {
        return other.sub(this).lengthSquared();
    }

    /**
     * @param other the other vector.
     * @return the dot product of this vector and the given vector.
     */
    public float dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    /**
     * @param other the other vector.
     * @return the cross product of this vector and the given vector.
     */
    public Vector3 cross(Vector3 other) {
        return new Vector3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }

    /**
     * @return a vector with the same direction as this vector and a length of 1,
     * or the zero vector if this vector has a length of 0.
     */
    public Vector3 normalize() {
        float len = length();
        return len == 0 ? ZERO : new Vector3(x / len, y / len, z / len);
    }

    /**
     * @return this vector as a flatbuffer {@link Vector3T}.
     */
    public Vector3T toVector3T() {
        var vector = new Vector3T();
        vector.setX(x);
        vector.setY(y);
        vector.setZ(z);
        return vector;
    }
}
