package com.warmthdawn.mods.yaoyaocoin.misc;

public class Rectangle2i {
    private int x0;
    private int y0;

    private int width;
    private int height;

    public Rectangle2i(int x0, int y0, int width, int height) {
        this.x0 = x0;
        this.y0 = y0;
        this.width = width;
        this.height = height;
    }

    public boolean intersects(Rectangle2i other) {
        return x0 < other.x0 + other.width && x0 + width > other.x0 && y0 < other.y0 + other.height && y0 + height > other.y0;
    }

    public boolean contains(Vector2i point) {
        return point.getX() >= x0 && point.getX() < x0 + width && point.getY() >= y0 && point.getY() < y0 + height;
    }

    public boolean contains(Rectangle2i other) {
        return x0 <= other.x0 && y0 <= other.y0 && x0 + width >= other.x0 + other.width && y0 + height >= other.y0 + other.height;
    }

    public int getX() {
        return x0;
    }

    public void setX(int x0) {
        this.x0 = x0;
    }

    public int getY() {
        return y0;
    }

    public int getY1() {
        return y0 + height;
    }

    public int getX1() {
        return x0 + width;
    }

    public void setY(int y0) {
        this.y0 = y0;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Rectangle2i scaled(int scale) {
        return new Rectangle2i(x0 * scale, y0 * scale, width * scale, height * scale);
    }

    public Rectangle2i expand(int amount) {
        return new Rectangle2i(x0 - amount, y0 - amount, width + 2 * amount, height + 2 * amount);
    }

    public Rectangle2i translated(Vector2i offset) {
        return new Rectangle2i(x0 + offset.getX(), y0 + offset.getY(), width, height);
    }

    public Rectangle2i scaleInPlace(int scale) {
        x0 *= scale;
        y0 *= scale;
        width *= scale;
        height *= scale;
        return this;
    }

    public Rectangle2i translateInPlace(Vector2i offset) {
        x0 += offset.getX();
        y0 += offset.getY();
        return this;
    }

    public Rectangle2i expandInPlace(int amount) {
        x0 -= amount;
        y0 -= amount;
        width += 2 * amount;
        height += 2 * amount;
        return this;
    }


}
