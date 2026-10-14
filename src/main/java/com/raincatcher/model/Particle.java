package com.raincatcher.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

/**
 * Visual particle effect for water splashes, explosions, sparks, and smoke
 */
public class Particle {

    public enum ParticleType {
        WATER_SPLASH,
        EXPLOSION_FIRE,
        EXPLOSION_SMOKE,
        SHOCKWAVE,
        SPARK
    }

    private double x;
    private double y;
    private double vx;
    private double vy;
    private double gravity;
    private float size;
    private float growth;
    private Color color;
    private int life;
    private final int maxLife;
    private final ParticleType type;

    public Particle(double x, double y, double vx, double vy, double gravity,
                    float size, float growth, Color color, int maxLife, ParticleType type) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.gravity = gravity;
        this.size = size;
        this.growth = growth;
        this.color = color;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.type = type;
    }

    /**
     * Updates physics and lifespan of the particle
     */
    public void update() {
        x += vx;
        y += vy;
        vy += gravity;
        size += growth;
        if (size < 0.5f) {
            size = 0.5f;
        }
        life--;
    }

    /**
     * Renders particle with opacity mapped to remaining lifespan
     */
    public void render(Graphics2D g2d) {
        if (life <= 0) {
            return;
        }

        float progress = (float) life / maxLife;
        int alpha = (int) (progress * color.getAlpha());
        if (alpha <= 0) {
            return;
        }

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color drawColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        g.setColor(drawColor);

        if (type == ParticleType.SHOCKWAVE) {
            g.setStroke(new BasicStroke(Math.max(1.0f, 3.0f * progress)));
            g.draw(new Ellipse2D.Float((float) (x - size / 2.0), (float) (y - size / 2.0), size, size));
        } else {
            g.fill(new Ellipse2D.Float((float) (x - size / 2.0), (float) (y - size / 2.0), size, size));
        }

        g.dispose();
    }

    public boolean isAlive() {
        return life > 0;
    }
}
