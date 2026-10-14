package com.raincatcher.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The player's basket. Caught raindrops add score; bombs cause game over.
 * Highly optimized rendering with pre-cached strokes and zero shape allocations.
 */
public class Basket {

    private static final BasicStroke STROKE_WEAVE_HORIZ = new BasicStroke(1.4f);
    private static final BasicStroke STROKE_BODY_OUTLINE = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_HANDLES = new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_RIM = new BasicStroke(2.0f);

    private static final Color COLOR_SHADOW = new Color(20, 35, 60, 75);
    private static final Color COLOR_WOOD_TOP = new Color(225, 160, 85);
    private static final Color COLOR_WOOD_BOTTOM = new Color(145, 85, 30);
    private static final Color COLOR_WEAVE_DARK = new Color(110, 60, 20, 160);
    private static final Color COLOR_WEAVE_LIGHT = new Color(250, 200, 140, 95);
    private static final Color COLOR_BODY_OUTLINE = new Color(90, 48, 15);
    private static final Color COLOR_HANDLES = new Color(175, 110, 50);
    private static final Color COLOR_RIM_TOP = new Color(245, 185, 110);
    private static final Color COLOR_RIM_BOTTOM = new Color(165, 95, 35);
    private static final Color COLOR_RIM_OUTLINE = new Color(95, 50, 18);
    private static final Color COLOR_RIM_HIGHLIGHT = new Color(255, 240, 200, 180);
    private static final Color COLOR_RIM_INTERIOR = new Color(60, 30, 10, 140);

    private double x;
    private double y;
    private final int width;
    private final int height;
    private double speed;

    private boolean movingLeft = false;
    private boolean movingRight = false;
    private double currentTilt = 0.0;
    private double targetTilt = 0.0;

    private Double mouseTargetX = null;

    // Reusable path for basket body
    private final Path2D.Float bodyPath = new Path2D.Float();

    public Basket(double startX, double startY, int width, int height, double speed) {
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;
        this.speed = speed;
    }

    /**
     * Resets basket position and movement states.
     */
    public void reset(int screenWidth) {
        this.x = (screenWidth - width) / 2.0;
        this.movingLeft = false;
        this.movingRight = false;
        this.currentTilt = 0.0;
        this.targetTilt = 0.0;
        this.mouseTargetX = null;
    }

    /**
     * Updates basket position based on active input and clamps to screen bounds.
     */
    public void update(int screenWidth) {
        double velocity = 0.0;

        if (mouseTargetX != null) {
            double diff = mouseTargetX - (x + width / 2.0);
            if (Math.abs(diff) > 2.0) {
                double step = Math.signum(diff) * Math.min(Math.abs(diff) * 0.45, speed * 1.5);
                x += step;
                velocity = step;
            } else {
                mouseTargetX = null;
            }
        } else {
            if (movingLeft && !movingRight) {
                velocity = -speed;
                x -= speed;
            } else if (movingRight && !movingLeft) {
                velocity = speed;
                x += speed;
            }
        }

        // Clamp to screen boundaries
        if (x < 4) {
            x = 4;
            velocity = 0;
        } else if (x > screenWidth - width - 4) {
            x = screenWidth - width - 4;
            velocity = 0;
        }

        // Tilt effect when moving
        if (velocity < -0.5) {
            targetTilt = -0.06;
        } else if (velocity > 0.5) {
            targetTilt = 0.06;
        } else {
            targetTilt = 0.0;
        }
        currentTilt += (targetTilt - currentTilt) * 0.2;
    }

    /**
     * Renders the stylized woven basket using Java2D.
     */
    public void render(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);

        // Ground shadow
        g.setColor(COLOR_SHADOW);
        g.fillOval(ix + 8, iy + height - 3, width - 16, 9);

        // Apply dynamic tilt around basket center
        AffineTransform oldTx = g.getTransform();
        g.rotate(currentTilt, x + width / 2.0, y + height / 2.0);

        float bx = (float) x;
        float by = (float) y;
        float bw = width;
        float bh = height;

        // 1. Basket Body Shape
        float inset = 9.0f;
        bodyPath.reset();
        bodyPath.moveTo(bx + 4, by + 10);
        bodyPath.lineTo(bx + bw - 4, by + 10);
        bodyPath.lineTo(bx + bw - inset, by + bh);
        bodyPath.lineTo(bx + inset, by + bh);
        bodyPath.closePath();

        GradientPaint woodGrad = new GradientPaint(
            bx, by + 10, COLOR_WOOD_TOP,
            bx, by + bh, COLOR_WOOD_BOTTOM
        );
        g.setPaint(woodGrad);
        g.fill(bodyPath);

        // 2. Woven Wicker Texture Bands
        g.setStroke(STROKE_WEAVE_HORIZ);
        int bands = 4;
        for (int i = 1; i <= bands; i++) {
            float bandY = by + 10 + (bh - 10) * (i / (float) (bands + 1));
            float sideInset = inset * (bandY - by - 10) / (bh - 10);
            g.setColor(COLOR_WEAVE_DARK);
            g.drawLine((int) (bx + 4 + sideInset), (int) bandY, (int) (bx + bw - 4 - sideInset), (int) bandY);
        }

        g.setColor(COLOR_WEAVE_LIGHT);
        for (int i = 0; i < 7; i++) {
            float wx = bx + 12 + i * 11;
            g.drawLine((int) wx, (int) (by + 11), (int) (wx - 4), (int) (by + bh - 2));
            g.drawLine((int) (wx - 4), (int) (by + 11), (int) wx, (int) (by + bh - 2));
        }

        // Outline
        g.setColor(COLOR_BODY_OUTLINE);
        g.setStroke(STROKE_BODY_OUTLINE);
        g.draw(bodyPath);

        // 3. Side Handles
        g.setColor(COLOR_HANDLES);
        g.setStroke(STROKE_HANDLES);
        g.drawArc(ix - 5, iy + 11, 12, 16, 90, 180);
        g.drawArc(ix + width - 7, iy + 11, 12, 16, 270, 180);

        // 4. Basket Top Rim
        GradientPaint rimGrad = new GradientPaint(
            bx, by + 4, COLOR_RIM_TOP,
            bx, by + 15, COLOR_RIM_BOTTOM
        );
        g.setPaint(rimGrad);
        g.fillRoundRect(ix, iy + 4, width, 11, 8, 8);
        g.setColor(COLOR_RIM_OUTLINE);
        g.setStroke(STROKE_RIM);
        g.drawRoundRect(ix, iy + 4, width, 11, 8, 8);

        // Rim glossy reflection
        g.setColor(COLOR_RIM_HIGHLIGHT);
        g.drawLine(ix + 6, iy + 6, ix + width - 6, iy + 6);

        // 5. Interior opening depth
        g.setColor(COLOR_RIM_INTERIOR);
        g.fillOval(ix + 8, iy + 5, width - 16, 6);

        g.setTransform(oldTx);
        g.dispose();
    }

    /**
     * Hitbox for catching raindrops (the top rim opening).
     */
    public Rectangle2D.Double getCatchBounds() {
        return new Rectangle2D.Double(x + 4, y + 2, width - 8, 18);
    }

    /**
     * Hitbox for bomb collisions (the full basket body).
     */
    public Rectangle2D.Double getBodyBounds() {
        return new Rectangle2D.Double(x + 6, y + 6, width - 12, height - 8);
    }

    // Input handlers
    public void setMovingLeft(boolean movingLeft) {
        this.movingLeft = movingLeft;
        if (movingLeft) {
            this.mouseTargetX = null;
        }
    }

    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
        if (movingRight) {
            this.mouseTargetX = null;
        }
    }

    public void setMouseTargetX(double mouseX) {
        this.mouseTargetX = mouseX;
    }

    // Getters and Setters
    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
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

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
}

