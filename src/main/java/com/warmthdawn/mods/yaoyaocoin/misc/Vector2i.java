package com.warmthdawn.mods.yaoyaocoin.misc;

public class Vector2i {
    public static final Vector2i ZERO = new Vector2i(0, 0);
    private int x;
    private int y;

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Vector2i add(Vector2i other) {
        return new Vector2i(x + other.x, y + other.y);
    }

    public int lengthSquared() {
        return x * x + y * y;
    }

    public int length() {
        return (int) Math.sqrt(lengthSquared());
    }

    public int lengthManhattan() {
        return Math.abs(x) + Math.abs(y);
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector2i other) {
            return x == other.x && y == other.y;
        }
        return false;
    }

    public Vector2i subtract(Vector2i newPos) {
        return new Vector2i(x - newPos.x, y - newPos.y);
    }

    public Vector2i scaleInPlace(int scale) {
        x *= scale;
        y *= scale;
        return this;
    }

    public Vector2i invertInPlace() {
        x = -x;
        y = -y;
        return this;
    }

    public Vector2i inverted() {
        return new Vector2i(-x, -y);
    }

    public Vector2i divide(int i) {
        return new Vector2i(x / i, y / i);
    }

    public Vector2i scaled(int scale) {
        return new Vector2i(x * scale, y * scale);
    }

    public Vector2i gridIndex(int gridSize) {
        return new Vector2i((int) Math.floor(1.0 * x / gridSize), (int) Math.floor(1.0 * y / gridSize));
    }

    // 吸附到网格
    public Vector2i gridAdsorption(int gridSize) {
        return gridIndex(gridSize).scaled(gridSize);
    }
}
