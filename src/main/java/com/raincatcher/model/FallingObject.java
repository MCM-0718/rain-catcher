package com.raincatcher.model;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * Base class ng falling items (Raindrops, Bombs)
 */
public abstract class FallingObject {

    protected double x;
    protected double y;
    protected int width;
    protected int height;
    protected double speedY;
    protected double speedX;
    protected boolean active;
    protected boolean collected;

    public FallingObject(double x, double y, int width, int height, double speedY) {
        this(x, y, width, height, speedY, 0.0);
    }

    public FallingObject(double x, double y, int width, int height, double speedY, double speedX) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.speedY = speedY;
        this.speedX = speedX;
        this.active = true;
        this.collected = false;
    }

    /**
     * Updates object position by velocity
     */
    public void update() {
        y += speedY;
        x += speedX;
    }

    /**
     * Renders the falling object using Java2D
     *
     * @param g2d Graphics2D rendering context
     */
    public abstract void render(Graphics2D g2d);

    /**
     * Returns the bounding rectangle used for collision checks
     */
    public Rectangle2D.Double getBounds() {
        return new Rectangle2D.Double(x, y, width, height);
    }

    /**
     * Checks if the object has completely left the bottom of the screen
     */
    public boolean isOutOfBounds(int screenHeight) {
        return y > screenHeight + 20;
    }

    // Getters and Setters
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getSpeedY() {
        return speedY;
    }

    public void setSpeedY(double speedY) {
        this.speedY = speedY;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public double getCenterX() {
        return x + width / 2.0;
    }

    public double getCenterY() {
        return y + height / 2.0;
    }
}
