package com.raincatcher.model;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * A falling raindrop. Catching it with the basket awards 1 point
 */
public class Raindrop extends FallingObject {

    private double shimmerAngle = 0.0;

    public Raindrop(double x, double y, int width, int height, double speedY) {
        super(x, y, width, height, speedY);
        // Random starting shimmer phase for variety
        this.shimmerAngle = Math.random() * Math.PI * 2;
    }

    @Override
    public void update() {
        super.update();
        shimmerAngle += 0.08;
    }

    @Override
    public void render(Graphics2D g2d) {
        if (!active) {
            return;
        }

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float drawX = (float) x;
        float drawY = (float) y;
        float w = width;
        float h = height;

        // Construct classic smooth teardrop path
        Path2D.Double dropPath = new Path2D.Double();
        dropPath.moveTo(drawX + w * 0.5f, drawY); // Top tip
        // Right side curve down to bottom lobe
        dropPath.curveTo(
            drawX + w * 0.85f, drawY + h * 0.35f,
            drawX + w,         drawY + h * 0.65f,
            drawX + w * 0.5f,  drawY + h
        );
        // Left side curve back up to top tip
        dropPath.curveTo(
            drawX,             drawY + h * 0.65f,
            drawX + w * 0.15f, drawY + h * 0.35f,
            drawX + w * 0.5f,  drawY
        );
        dropPath.closePath();

        // Outer soft glow/shimmer
        g.setColor(new Color(150, 220, 255, 60));
        g.fill(dropPath);

        // Body gradient (Aqua cyan to vibrant deep sky blue)
        GradientPaint dropGrad = new GradientPaint(
            drawX, drawY, new Color(130, 215, 255),
            drawX, drawY + h, new Color(25, 120, 235)
        );
        g.setPaint(dropGrad);
        g.fill(dropPath);

        // Subtle dark outline
        g.setColor(new Color(15, 80, 180, 180));
        g.draw(dropPath);

        // Inner 3D specular highlight glint (curved bubble gleam)
        g.setColor(new Color(255, 255, 255, 210));
        Ellipse2D.Double glint = new Ellipse2D.Double(
            drawX + w * 0.28,
            drawY + h * 0.32,
            w * 0.28,
            h * 0.30
        );
        g.fill(glint);

        // Tiny secondary shine dot
        g.setColor(new Color(255, 255, 255, 160));
        g.fill(new Ellipse2D.Double(drawX + w * 0.40, drawY + h * 0.68, w * 0.15, h * 0.14));

        g.dispose();
    }

    @Override
    public Rectangle2D.Double getBounds() {
        // Tight calibrated hitbox matching the droplet core
        return new Rectangle2D.Double(x + 2, y + 4, width - 4, height - 6);
    }
}
