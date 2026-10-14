package com.raincatcher.model;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A lethal falling bomb. Contact with the basket triggers immediate game over.
 * Highly optimized rendering with pre-cached strokes and reusable paths.
 */
public class Bomb extends FallingObject {

    private static final BasicStroke STROKE_FUSE = new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_BODY_OUTLINE = new BasicStroke(1.2f);

    private static final float[] SPARK_RATIOS = {0.0f, 0.4f, 1.0f};
    private static final Color[] SPARK_COLORS = {Color.WHITE, Color.YELLOW, new Color(255, 60, 0, 0)};

    private static final float[] BODY_RATIOS = {0.0f, 0.45f, 0.85f, 1.0f};
    private static final Color[] BODY_COLORS = {
        new Color(95, 95, 105),
        new Color(45, 45, 52),
        new Color(20, 20, 24),
        new Color(10, 10, 14)
    };

    private static final Color COLOR_FUSE_ROPE = new Color(190, 150, 90);
    private static final Color COLOR_COLLAR_FILL = new Color(90, 90, 100);
    private static final Color COLOR_COLLAR_OUTLINE = new Color(50, 50, 60);
    private static final Color COLOR_SPHERE_SHADOW = new Color(20, 20, 25, 60);
    private static final Color COLOR_BODY_OUTLINE = new Color(12, 12, 16);

    private final Path2D.Float fusePath = new Path2D.Float();
    private double fuseTick = 0;

    public Bomb(double x, double y, int diameter, double speedY) {
        super(x, y, diameter, diameter, speedY);
    }

    @Override
    public void update() {
        super.update();
        fuseTick += 0.25;
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
        float d = width;

        float bodyDiameter = d * 0.82f;
        float bodyX = drawX + (d - bodyDiameter) / 2.0f;
        float bodyY = drawY + d * 0.20f;

        // 1. Draw Fuse
        float neckX = bodyX + bodyDiameter * 0.5f;
        float neckY = bodyY;
        float fuseEndX = neckX + bodyDiameter * 0.35f;
        float fuseEndY = drawY + 2.0f;

        fusePath.reset();
        fusePath.moveTo(neckX, neckY);
        fusePath.curveTo(
            neckX + 5, neckY - 8,
            fuseEndX - 4, neckY - 14,
            fuseEndX, fuseEndY
        );

        g.setColor(COLOR_FUSE_ROPE);
        g.setStroke(STROKE_FUSE);
        g.draw(fusePath);

        // 2. Metallic Collar
        g.setColor(COLOR_COLLAR_FILL);
        g.fillRoundRect((int) (neckX - 4), (int) (neckY - 3), 8, 5, 2, 2);
        g.setColor(COLOR_COLLAR_OUTLINE);
        g.drawRoundRect((int) (neckX - 4), (int) (neckY - 3), 8, 5, 2, 2);

        // 3. Fuse Spark Flame
        double flicker = Math.sin(fuseTick * 2.0) * 2.0;
        float sparkRadius = (float) (5.0 + Math.abs(flicker));
        Point2D sparkCenter = new Point2D.Float(fuseEndX, fuseEndY);
        RadialGradientPaint sparkGrad = new RadialGradientPaint(
            sparkCenter, sparkRadius,
            SPARK_RATIOS, SPARK_COLORS
        );
        g.setPaint(sparkGrad);
        g.fillOval(
            (int) (fuseEndX - sparkRadius), (int) (fuseEndY - sparkRadius),
            (int) (sparkRadius * 2), (int) (sparkRadius * 2)
        );

        // Spark star
        g.setColor(Color.WHITE);
        g.drawLine((int) fuseEndX - 2, (int) fuseEndY, (int) fuseEndX + 2, (int) fuseEndY);
        g.drawLine((int) fuseEndX, (int) fuseEndY - 2, (int) fuseEndX, (int) fuseEndY + 2);

        // 4. Bomb Shadow
        g.setColor(COLOR_SPHERE_SHADOW);
        g.fillOval((int) (bodyX - 1), (int) (bodyY + 1), (int) (bodyDiameter + 2), (int) (bodyDiameter + 2));

        // 5. 3D Shaded Bomb Body
        Point2D lightCenter = new Point2D.Float(bodyX + bodyDiameter * 0.32f, bodyY + bodyDiameter * 0.30f);
        RadialGradientPaint bodyGrad = new RadialGradientPaint(
            lightCenter, bodyDiameter * 0.65f,
            BODY_RATIOS, BODY_COLORS
        );
        g.setPaint(bodyGrad);
        g.fillOval((int) bodyX, (int) bodyY, (int) bodyDiameter, (int) bodyDiameter);

        // Outline
        g.setColor(COLOR_BODY_OUTLINE);
        g.setStroke(STROKE_BODY_OUTLINE);
        g.drawOval((int) bodyX, (int) bodyY, (int) bodyDiameter, (int) bodyDiameter);

        // 6. Hazard Warning Mark
        float skullX = bodyX + bodyDiameter * 0.34f;
        float skullY = bodyY + bodyDiameter * 0.36f;
        float skullSize = bodyDiameter * 0.32f;

        int hazardAlpha = 200 + (int) (40 * Math.sin(fuseTick * 1.5));
        g.setColor(new Color(255, 75, 45, Math.min(255, Math.max(0, hazardAlpha))));

        g.fillRoundRect((int) (skullX + skullSize * 0.38f), (int) skullY, (int) (skullSize * 0.24f), (int) (skullSize * 0.55f), 2, 2);
        g.fillOval((int) (skullX + skullSize * 0.38f), (int) (skullY + skullSize * 0.72f), (int) (skullSize * 0.24f), (int) (skullSize * 0.24f));

        g.dispose();
    }

    @Override
    public Rectangle2D.Double getBounds() {
        double d = width * 0.80;
        double bx = x + (width - d) / 2.0;
        double by = y + width * 0.20;
        return new Rectangle2D.Double(bx + 2, by + 2, d - 4, d - 4);
    }
}

