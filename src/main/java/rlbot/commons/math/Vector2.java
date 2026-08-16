package rlbot.commons.math;

import rlbot.flat.Vector2T;

/**
 * An immutable two dimensional vector with single precision components.
 */
public record Vector2(float x, float y) {

    /**
     * The zero vector.
     */
    public static final Vector2 ZERO = new Vector2(0, 0);

    /**
     * Creates a {@link Vector2} from a flatbuffer {@link Vector2T}.
     *
     * @param vector the flatbuffer vector to convert.
     * @return the resulting vector.
     */
    public static Vector2 fromVector2T(Vector2T vector) {
        return new Vector2(vector.getX(), vector.getY());
    }

    /**
     * @param other the vector to add.
     * @return the sum of this vector and the given vector.
     */
    public Vector2 add(Vector2 other) {
        return new Vector2(x + other.x, y + other.y);
    }

    /**
     * @param x the x component to add.
     * @param y the y component to add.
     * @return the sum of this vector and the given components.
     */
    public Vector2 add(float x, float y) {
        return new Vector2(this.x + x, this.y + y);
    }

    /**
     * @param other the vector to subtract.
     * @return the difference between this vector and the given vector.
     */
    public Vector2 sub(Vector2 other) {
        return new Vector2(x - other.x, y - other.y);
    }

    /**
     * @param x the x component to subtract.
     * @param y the y component to subtract.
     * @return the difference between this vector and the given components.
     */
    public Vector2 sub(float x, float y) {
        return new Vector2(this.x - x, this.y - y);
    }

    /**
     * @param scalar the scalar to multiply by.
     * @return this vector multiplied by the given scalar.
     */
    public Vector2 mul(float scalar) {
        return new Vector2(x * scalar, y * scalar);
    }

    /**
     * @param other the vector to multiply by.
     * @return this vector multiplied component-wise by the given vector.
     */
    public Vector2 mul(Vector2 other) {
        return new Vector2(x * other.x, y * other.y);
    }

    /**
     * @param scalar the scalar to divide by.
     * @return this vector divided by the given scalar.
     */
    public Vector2 div(float scalar) {
        return new Vector2(x / scalar, y / scalar);
    }

    /**
     * @return this vector negated.
     */
    public Vector2 negate() {
        return new Vector2(-x, -y);
    }

    /**
     * @return the squared length of this vector.
     */
    public float lengthSquared() {
        return x * x + y * y;
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
    public float distanceTo(Vector2 other) {
        return other.sub(this).length();
    }

    /**
     * @param other the other vector.
     * @return the squared distance between this vector and the given vector.
     */
    public float distanceToSquared(Vector2 other) {
        return other.sub(this).lengthSquared();
    }

    /**
     * @param other the other vector.
     * @return the dot product of this vector and the given vector.
     */
    public float dot(Vector2 other) {
        return x * other.x + y * other.y;
    }

    /**
     * @return a vector with the same direction as this vector and a length of 1,
     * or the zero vector if this vector has a length of 0.
     */
    public Vector2 normalize() {
        float len = length();
        return len == 0 ? ZERO : new Vector2(x / len, y / len);
    }

    /**
     * @return this vector as a flatbuffer {@link Vector2T}.
     */
    public Vector2T toVector2T() {
        var vector = new Vector2T();
        vector.setX(x);
        vector.setY(y);
        return vector;
    }
}
