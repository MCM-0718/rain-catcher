package com.raincatcher.model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/**
 * Animated cloud drifting in the sky background to provide parallax depth.
 */
public class Cloud {

    private double x;
    private double y;
    private final double speed;
    private final float scale;
    private final int alpha;

    // Relative circle offsets to form a fluffy cloud
    private final float[] circleOffsetsX = {0.0f, 25.0f, 55.0f, 85.0f, 40.0f};
    private final float[] circleOffsetsY = {15.0f, 5.0f, 0.0f, 15.0f, 18.0f};
    private final float[] circleRadii    = {28.0f, 36.0f, 42.0f, 30.0f, 32.0f};

    public Cloud(double x, double y, double speed, float scale, int alpha) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.scale = scale;
        this.alpha = alpha;
    }

    /**
     * Factory method creating a randomized cloud.
     */
    public static Cloud createRandom(int screenWidth, int screenHeight, Random rand) {
        double startX = rand.nextDouble() * (screenWidth + 200) - 100;
        double startY = 25 + rand.nextDouble() * (screenHeight * 0.38);
        double speed = 0.25 + rand.nextDouble() * 0.65;
        float scale = 0.75f + rand.nextFloat() * 0.65f;
        int alpha = 130 + rand.nextInt(75);
        return new Cloud(startX, startY, speed, scale, alpha);
    }

    /**
     * Updates cloud drift.
     */
    public void update(int screenWidth) {
        x += speed;
        float totalWidth = 120.0f * scale;
        if (x > screenWidth + 50) {
            x = -totalWidth - 30;
        }
    }

    /**
     * Renders cloud.
     */
    public void render(Graphics2D g2d) {
        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Soft cloud white with alpha
        g.setColor(new Color(255, 255, 255, alpha));

        for (int i = 0; i < circleOffsetsX.length; i++) {
            float cx = (float) (x + circleOffsetsX[i] * scale);
            float cy = (float) (y + circleOffsetsY[i] * scale);
            float r = circleRadii[i] * scale;
            g.fill(new Ellipse2D.Float(cx - r / 2.0f, cy - r / 2.0f, r, r));
        }

        g.dispose();
    }
}

