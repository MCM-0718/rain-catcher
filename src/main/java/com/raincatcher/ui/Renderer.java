package com.raincatcher.ui;

import com.raincatcher.audio.SoundManager;
import com.raincatcher.config.GameConfig;
import com.raincatcher.logic.DifficultyManager;
import com.raincatcher.logic.ScoreManager;
import com.raincatcher.model.Basket;
import com.raincatcher.model.Cloud;
import com.raincatcher.model.FallingObject;
import com.raincatcher.model.GameState;
import com.raincatcher.model.Particle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Handles all graphical rendering using Java2D.
 * Features an atmospheric typhoon/storm theme with top storm cloud canopy,
 * broken shanty houses with rusty corrugated iron roofs (yero),
 * and dynamic animated floodwater.
 */
public class Renderer {

    // Pre-cached Fonts
    private final Font fontTitleLarge = new Font("SansSerif", Font.BOLD, 46);
    private final Font fontTitleMedium = new Font("SansSerif", Font.BOLD, 28);
    private final Font fontHud = new Font("SansSerif", Font.BOLD, 16);
    private final Font fontHudSmall = new Font("SansSerif", Font.BOLD, 13);
    private final Font fontBody = new Font("SansSerif", Font.PLAIN, 15);
    private final Font fontPrompt = new Font("SansSerif", Font.BOLD, 18);

    // Pre-cached BasicStrokes
    private static final BasicStroke STROKE_1_0 = new BasicStroke(1.0f);
    private static final BasicStroke STROKE_1_2 = new BasicStroke(1.2f);
    private static final BasicStroke STROKE_1_5 = new BasicStroke(1.5f);
    private static final BasicStroke STROKE_1_8 = new BasicStroke(1.8f);
    private static final BasicStroke STROKE_2_0 = new BasicStroke(2.0f);
    private static final BasicStroke STROKE_2_2 = new BasicStroke(2.2f);
    private static final BasicStroke STROKE_POLE = new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke STROKE_WIRE = new BasicStroke(1.0f);
    private static final BasicStroke STROKE_CORRUGATION = new BasicStroke(1.2f);

    // Atmospheric Colors - Storm & Scenery
    private static final Color COLOR_STORM_SKY_TOP = new Color(35, 65, 95);
    private static final Color COLOR_STORM_SKY_MID = new Color(75, 115, 145);
    private static final Color COLOR_STORM_SKY_HORIZON = new Color(135, 165, 185);

    // House & Yero Colors
    private static final Color COLOR_WOOD_WALL_1 = new Color(110, 85, 60);
    private static final Color COLOR_WOOD_WALL_2 = new Color(85, 68, 52);
    private static final Color COLOR_CONCRETE_WALL = new Color(125, 132, 132);
    private static final Color COLOR_DOOR_WINDOW = new Color(30, 25, 22, 230);
    private static final Color COLOR_YERO_METAL = new Color(145, 152, 160);
    private static final Color COLOR_YERO_DARK = new Color(90, 96, 104);
    private static final Color COLOR_YERO_RUST_1 = new Color(165, 80, 42);
    private static final Color COLOR_YERO_RUST_2 = new Color(130, 55, 25);
    private static final Color COLOR_BLUE_TARP = new Color(30, 95, 180);
    private static final Color COLOR_TIRE_BLACK = new Color(35, 35, 40);

    // Floodwater Colors
    private static final Color COLOR_FLOOD_DEEP = new Color(32, 55, 62);
    private static final Color COLOR_FLOOD_MID = new Color(52, 95, 105, 220);
    private static final Color COLOR_FLOOD_SURFACE = new Color(85, 145, 158, 210);
    private static final Color COLOR_FLOOD_FOAM = new Color(165, 215, 225, 175);
    private static final Color COLOR_FLOOD_DRIFTWOOD = new Color(105, 75, 45);

    // Top Storm Cloud Colors
    private static final Color COLOR_CLOUD_BASE = new Color(32, 42, 58);
    private static final Color COLOR_CLOUD_MID = new Color(55, 68, 88);
    private static final Color COLOR_CLOUD_LIT = new Color(82, 102, 128);
    private static final Color COLOR_CLOUD_RIM = new Color(135, 160, 190, 140);
    private static final Color COLOR_CLOUD_DRIP = new Color(140, 210, 255, 160);

    // HUD & Badge Colors
    private static final Color COLOR_PILL_SCORE_BG = new Color(20, 45, 80, 215);
    private static final Color COLOR_PILL_SCORE_BORDER = new Color(100, 175, 245);
    private static final Color COLOR_PILL_BEST_BG = new Color(50, 40, 18, 215);
    private static final Color COLOR_PILL_BEST_BORDER = new Color(255, 195, 65);
    private static final Color COLOR_PILL_DIFF_BG = new Color(22, 32, 48, 215);
    private static final Color COLOR_PILL_DIFF_BORDER = new Color(125, 155, 205);
    private static final Color COLOR_MENU_BACKDROP = new Color(15, 25, 45, 160);
    private static final Color COLOR_MENU_CARD_BG = new Color(20, 32, 55, 225);
    private static final Color COLOR_MENU_CARD_BORDER = new Color(85, 135, 200);
    private static final Color COLOR_PAUSE_BACKDROP = new Color(10, 18, 30, 190);
    private static final Color COLOR_PAUSE_CARD_BG = new Color(20, 34, 58, 235);
    private static final Color COLOR_PAUSE_CARD_BORDER = new Color(105, 150, 225);
    private static final Color COLOR_GAMEOVER_BACKDROP = new Color(35, 10, 15, 205);
    private static final Color COLOR_GAMEOVER_CARD_BG = new Color(32, 16, 22, 245);
    private static final Color COLOR_GAMEOVER_CARD_BORDER = new Color(235, 75, 60);

    // Reusable Paths for Waves and Clouds
    private final Path2D.Float wavePath = new Path2D.Float();
    private final Path2D.Float waveBackPath = new Path2D.Float();
    private double animTick = 0;

    public void updateAnimation() {
        animTick += 0.05;
    }

    /**
     * Master render routine.
     */
    public void render(Graphics2D g2d,
                       int width,
                       int height,
                       GameState state,
                       Basket basket,
                       List<FallingObject> fallingObjects,
                       List<Particle> particles,
                       List<Cloud> clouds,
                       ScoreManager scoreManager,
                       DifficultyManager difficultyManager,
                       SoundManager soundManager,
                       double shakeOffsetX,
                       double shakeOffsetY) {

        Graphics2D g = (Graphics2D) g2d.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Apply screen shake transform (if any)
        if (Math.abs(shakeOffsetX) > 0.1 || Math.abs(shakeOffsetY) > 0.1) {
            g.translate(shakeOffsetX, shakeOffsetY);
        }

        // 1. Stormy Sky & Distant Atmosphere
        drawStormySky(g, width, height);

        // 2. Parallax Drifting Clouds
        if (clouds != null) {
            for (int i = 0; i < clouds.size(); i++) {
                clouds.get(i).render(g);
            }
        }

        // 3. Flooded Village & Weathered Houses with Broken Yero Roofs
        drawFloodedVillage(g, width, height);

        // 4. Animated Floodwater Layer
        drawFloodwater(g, width, height);

        // 5. Falling Objects (Raindrops & Bombs)
        if (fallingObjects != null) {
            for (int i = 0; i < fallingObjects.size(); i++) {
                fallingObjects.get(i).render(g);
            }
        }

        // 6. Player Basket
        if (basket != null) {
            basket.render(g);
        }

        // 7. Visual Particles (Splashes, Explosions, Sparks)
        if (particles != null) {
            for (int i = 0; i < particles.size(); i++) {
                particles.get(i).render(g);
            }
        }

        // 8. Top Storm Clouds Canopy (Raindrops visibly emerge from here)
        drawTopStormClouds(g, width);

        // 9. HUD (Visible during PLAYING and PAUSED)
        if (state == GameState.PLAYING || state == GameState.PAUSED) {
            drawHUD(g, width, height, scoreManager, difficultyManager, soundManager);
        }

        // 10. Overlays based on state
        switch (state) {
            case MENU:
                drawMenu(g, width, height, scoreManager, soundManager);
                break;
            case PAUSED:
                drawPauseOverlay(g, width, height);
                break;
            case GAME_OVER:
                drawGameOverOverlay(g, width, height, scoreManager, difficultyManager);
                break;
            case PLAYING:
            default:
                break;
        }

        g.dispose();
    }

    /**
     * Draws moody overcast storm sky.
     */
    private void drawStormySky(Graphics2D g, int width, int height) {
        GradientPaint skyGrad = new GradientPaint(
            0, 0, COLOR_STORM_SKY_TOP,
            0, GameConfig.PLAY_AREA_BOTTOM, COLOR_STORM_SKY_HORIZON
        );
        g.setPaint(skyGrad);
        g.fillRect(0, 0, width, height);

        // Ambient rain haze streaks in the distance
        g.setColor(new Color(180, 205, 225, 25));
        for (int i = 0; i < 20; i++) {
            int hx = (int) ((i * 45 + animTick * 120) % (width + 60)) - 30;
            g.drawLine(hx, 60, hx - 25, GameConfig.PLAY_AREA_BOTTOM);
        }
    }

    /**
     * Draws the top storm cloud canopy across the entire top of the screen.
     * Raindrops visibly drop out from under these clouds.
     */
    private void drawTopStormClouds(Graphics2D g, int width) {
        // 1. Dark upper overcast ceiling
        g.setColor(COLOR_CLOUD_BASE);
        g.fillRect(0, 0, width, 32);

        // 2. Back cloud billow layer (darker slate)
        g.setColor(COLOR_CLOUD_MID);
        int billowW = 100;
        for (int x = -40; x <= width + 50; x += 75) {
            double sway = Math.sin(animTick * 0.8 + x * 0.03) * 3.0;
            g.fillOval(x, 8 + (int) sway, billowW, 58);
        }

        // 3. Foreground cloud billow layer (puffy storm clouds)
        g.setColor(COLOR_CLOUD_LIT);
        for (int x = -20; x <= width + 40; x += 85) {
            double sway = Math.cos(animTick * 1.0 + x * 0.04) * 4.0;
            g.fillOval(x, 14 + (int) sway, 90, 54);
        }

        // 4. Silver/gray underside rim highlight
        g.setColor(COLOR_CLOUD_RIM);
        g.setStroke(STROKE_1_5);
        for (int x = -15; x <= width + 30; x += 85) {
            double sway = Math.cos(animTick * 1.0 + x * 0.04) * 4.0;
            g.drawArc(x + 10, 32 + (int) sway, 70, 34, 190, 160);
        }

        // 5. Condensation drips along the bottom of the clouds
        g.setColor(COLOR_CLOUD_DRIP);
        for (int i = 0; i < 12; i++) {
            int dripX = (int) ((i * 70 + 35) % width);
            double phase = (animTick * 2.0 + i) % Math.PI;
            int dripY = (int) (52 + Math.sin(phase) * 6.0);
            g.fillOval(dripX, dripY, 3, 5);
        }
    }

    /**
     * Draws weathered rural/shanty houses with broken, rusty corrugated iron roofs (yero).
     */
    private void drawFloodedVillage(Graphics2D g, int width, int height) {
        int groundY = GameConfig.PLAY_AREA_BOTTOM;

        // Leaning Wooden Power Pole (Left)
        g.setColor(new Color(60, 48, 38));
        g.setStroke(STROKE_POLE);
        g.drawLine(70, groundY - 2, 78, groundY - 140);
        // Crossbar
        g.setStroke(STROKE_2_0);
        g.drawLine(65, groundY - 132, 92, groundY - 130);
        // Sagging Power Lines swaying in the wind
        g.setColor(new Color(45, 52, 60, 180));
        g.setStroke(STROKE_WIRE);
        Path2D.Float wire1 = new Path2D.Float();
        wire1.moveTo(78, groundY - 132);
        wire1.curveTo(200, groundY - 95, 450, groundY - 100, width - 40, groundY - 125);
        g.draw(wire1);

        // HOUSE 1 (Left Shack): Wooden planks with slanted rusty yero roof, curled/bent edge
        drawShantyHouse(g, 95, groundY - 80, 115, 68,
                COLOR_WOOD_WALL_1, true, -16, 0.45f, true);

        // HOUSE 2 (Center-Left Two-Story Shanty): Concrete base, wood upper floor, blue tarp patched yero
        drawTwoStoryHouse(g, 240, groundY - 105, 130, 95);

        // HOUSE 3 (Center-Right Weathered Cottage): Missing iron sheet exposing roof frame
        drawShantyHouse(g, 430, groundY - 78, 120, 66,
                COLOR_CONCRETE_WALL, false, 14, 0.70f, false);

        // HOUSE 4 (Right Shack on Stilts): Broken jagged yero roof, leaning post
        drawStiltShanty(g, 600, groundY - 88, 135, 76);
    }

    /**
     * Helper to render a weathered shanty house with corrugated iron roof (yero).
     */
    private void drawShantyHouse(Graphics2D g, int x, int y, int w, int h,
                                Color wallColor, boolean woodenWall, int roofPitch,
                                float rustCoverage, boolean brokenCorner) {
        // 1. House Walls
        g.setColor(wallColor);
        g.fillRect(x, y + 16, w, h - 16);

        // Wooden wall planks
        if (woodenWall) {
            g.setColor(COLOR_WOOD_WALL_2);
            g.setStroke(STROKE_1_0);
            for (int py = y + 24; py < y + h; py += 10) {
                g.drawLine(x, py, x + w, py);
            }
        }

        // Wall Outline
        g.setColor(new Color(40, 32, 25));
        g.setStroke(STROKE_1_5);
        g.drawRect(x, y + 16, w, h - 16);

        // Dark window opening
        g.setColor(COLOR_DOOR_WINDOW);
        g.fillRect(x + w / 2 - 12, y + 28, 24, 20);
        g.setColor(new Color(75, 60, 48));
        g.drawRect(x + w / 2 - 12, y + 28, 24, 20);

        // 2. Corrugated Iron Roof (Yero)
        int roofLeftX = x - 10;
        int roofLeftY = y + 14 - roofPitch;
        int roofRightX = x + w + 10;
        int roofRightY = y + 14 + roofPitch;

        // Base metallic gray yero sheet
        Path2D.Float roof = new Path2D.Float();
        roof.moveTo(roofLeftX, roofLeftY);
        roof.lineTo(roofRightX, roofRightY);
        roof.lineTo(roofRightX - 4, roofRightY + 10);
        roof.lineTo(roofLeftX + 4, roofLeftY + 10);
        roof.closePath();

        g.setColor(COLOR_YERO_METAL);
        g.fill(roof);

        // Corrugated vertical ribs
        g.setColor(COLOR_YERO_DARK);
        g.setStroke(STROKE_CORRUGATION);
        int steps = 14;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            int topX = (int) (roofLeftX + t * (roofRightX - roofLeftX));
            int topY = (int) (roofLeftY + t * (roofRightY - roofLeftY));
            g.drawLine(topX, topY, topX + 2, topY + 9);
        }

        // Rust patches on the yero (oxidized red-brown streaks)
        g.setColor(COLOR_YERO_RUST_1);
        int rustW = (int) (w * rustCoverage);
        g.fillRect(x + 12, roofLeftY + (roofPitch < 0 ? 0 : roofPitch / 2), rustW, 5);
        g.setColor(COLOR_YERO_RUST_2);
        g.fillRect(x + 20, roofLeftY + (roofPitch < 0 ? 2 : roofPitch / 2 + 2), rustW / 2, 3);

        // Broken/curled yero sheet corner flapping in storm
        if (brokenCorner) {
            Path2D.Float tornCorner = new Path2D.Float();
            tornCorner.moveTo(roofRightX - 16, roofRightY);
            tornCorner.lineTo(roofRightX + 4, roofRightY - 14); // Curled upward
            tornCorner.lineTo(roofRightX, roofRightY + 8);
            tornCorner.closePath();

            g.setColor(COLOR_YERO_METAL);
            g.fill(tornCorner);
            g.setColor(COLOR_YERO_RUST_1);
            g.draw(tornCorner);
        }
    }

    /**
     * Two-story shanty with blue typhoon tarp held down by old tires.
     */
    private void drawTwoStoryHouse(Graphics2D g, int x, int y, int w, int h) {
        // Lower floor (Concrete/cinderblock)
        g.setColor(COLOR_CONCRETE_WALL);
        g.fillRect(x, y + 42, w, h - 42);
        g.setColor(new Color(60, 65, 65));
        g.setStroke(STROKE_1_0);
        g.drawRect(x, y + 42, w, h - 42);

        // Upper floor (Weathered wood)
        g.setColor(COLOR_WOOD_WALL_1);
        g.fillRect(x + 8, y + 16, w - 16, 28);
        g.setColor(new Color(40, 30, 22));
        g.drawRect(x + 8, y + 16, w - 16, 28);

        // Windows
        g.setColor(COLOR_DOOR_WINDOW);
        g.fillRect(x + 22, y + 22, 16, 16);
        g.fillRect(x + w - 38, y + 22, 16, 16);
        g.fillRect(x + w / 2 - 10, y + 54, 20, 28); // Doorway

        // Corrugated yero roof with Blue Tarp patch
        Path2D.Float roof = new Path2D.Float();
        roof.moveTo(x - 4, y + 16);
        roof.lineTo(x + w + 4, y + 8);
        roof.lineTo(x + w, y + 18);
        roof.lineTo(x, y + 24);
        roof.closePath();

        g.setColor(COLOR_YERO_METAL);
        g.fill(roof);

        // Blue waterproof typhoon tarp covering center of roof
        Path2D.Float tarp = new Path2D.Float();
        tarp.moveTo(x + 24, y + 14);
        tarp.lineTo(x + w - 24, y + 10);
        tarp.lineTo(x + w - 26, y + 20);
        tarp.lineTo(x + 22, y + 22);
        tarp.closePath();
        g.setColor(COLOR_BLUE_TARP);
        g.fill(tarp);

        // Old tire holding down the tarp (classic Philippine typhoon detail)
        g.setColor(COLOR_TIRE_BLACK);
        g.fillOval(x + w / 2 - 8, y + 11, 16, 8);
        g.setColor(COLOR_BLUE_TARP);
        g.fillOval(x + w / 2 - 4, y + 13, 8, 4);

        // TV Antenna pole leaning on roof
        g.setColor(new Color(90, 95, 100));
        g.setStroke(STROKE_1_2);
        g.drawLine(x + w - 12, y + 10, x + w - 5, y - 26);
        g.drawLine(x + w - 10, y - 22, x + w - 2, y - 20);
        g.drawLine(x + w - 9, y - 16, x + w - 3, y - 14);
    }

    /**
     * Shanty built on wooden stilts over rising water.
     */
    private void drawStiltShanty(Graphics2D g, int x, int y, int w, int h) {
        // Wooden stilts submerged in water
        g.setColor(new Color(55, 42, 30));
        g.setStroke(STROKE_POLE);
        g.drawLine(x + 14, y + 35, x + 12, y + h + 10);
        g.drawLine(x + w - 18, y + 35, x + w - 16, y + h + 10);
        g.drawLine(x + w / 2, y + 35, x + w / 2 + 2, y + h + 10);

        // House body
        g.setColor(new Color(98, 75, 52));
        g.fillRect(x, y + 12, w, h - 25);
        g.setColor(new Color(38, 28, 20));
        g.setStroke(STROKE_1_5);
        g.drawRect(x, y + 12, w, h - 25);

        // Window with crooked wooden shutter
        g.setColor(COLOR_DOOR_WINDOW);
        g.fillRect(x + 25, y + 22, 22, 18);
        g.setColor(new Color(115, 85, 58));
        g.fillRect(x + 44, y + 18, 12, 22); // Half-open hanging shutter

        // Broken jagged yero roof (missing pieces)
        Path2D.Float roof = new Path2D.Float();
        roof.moveTo(x - 8, y + 14);
        roof.lineTo(x + w + 6, y + 4);
        roof.lineTo(x + w - 15, y + 18);
        roof.lineTo(x, y + 22);
        roof.closePath();
        g.setColor(COLOR_YERO_METAL);
        g.fill(roof);

        // Exposed roof timber where yero blew away
        g.setColor(new Color(75, 50, 32));
        g.setStroke(STROKE_1_5);
        g.drawLine(x + w - 32, y + 8, x + w - 8, y + 16);
        g.drawLine(x + w - 24, y + 6, x + w - 4, y + 14);

        // Heavy rust streaks
        g.setColor(COLOR_YERO_RUST_1);
        g.fillRect(x + 6, y + 14, 38, 4);
    }

    /**
     * Draws animated rising floodwater at the bottom of the screen.
     */
    private void drawFloodwater(Graphics2D g, int width, int height) {
        int waterBaseY = GameConfig.PLAY_AREA_BOTTOM - 18;

        // 1. Construct Back Wave Path
        waveBackPath.reset();
        waveBackPath.moveTo(0, height);
        waveBackPath.lineTo(0, waterBaseY);
        for (int x = 0; x <= width; x += 16) {
            double waveY = waterBaseY + Math.sin(animTick * 1.4 + x * 0.02) * 2.5
                    + Math.cos(animTick * 0.9 + x * 0.04) * 1.5;
            waveBackPath.lineTo(x, (float) waveY);
        }
        waveBackPath.lineTo(width, height);
        waveBackPath.closePath();

        // Fill Back Wave with darker silt water
        GradientPaint backWaterGrad = new GradientPaint(
            0, waterBaseY, COLOR_FLOOD_DEEP,
            0, height, new Color(20, 35, 42)
        );
        g.setPaint(backWaterGrad);
        g.fill(waveBackPath);

        // 2. Construct Front Wave Path
        wavePath.reset();
        wavePath.moveTo(0, height);
        wavePath.lineTo(0, waterBaseY + 4);
        for (int x = 0; x <= width; x += 14) {
            double waveY = waterBaseY + 4 + Math.sin(animTick * 2.0 + x * 0.025) * 3.2
                    + Math.cos(animTick * 1.3 + x * 0.05) * 1.8;
            wavePath.lineTo(x, (float) waveY);
        }
        wavePath.lineTo(width, height);
        wavePath.closePath();

        // Fill Front Wave with murky teal-brown flood runoff
        GradientPaint frontWaterGrad = new GradientPaint(
            0, waterBaseY + 4, COLOR_FLOOD_SURFACE,
            0, height, COLOR_FLOOD_MID
        );
        g.setPaint(frontWaterGrad);
        g.fill(wavePath);

        // 3. Floodwater Foam / Crest Highlight
        g.setColor(COLOR_FLOOD_FOAM);
        g.setStroke(STROKE_1_2);
        for (int x = 10; x <= width - 10; x += 14) {
            double waveY = waterBaseY + 4 + Math.sin(animTick * 2.0 + x * 0.025) * 3.2
                    + Math.cos(animTick * 1.3 + x * 0.05) * 1.8;
            g.drawLine(x - 4, (int) waveY, x + 4, (int) waveY);
        }

        // 4. Floating Driftwood / Plank bobbing in the flood
        double plankWave = Math.sin(animTick * 2.0 + 140 * 0.025) * 3.2;
        int plankX = 135;
        int plankY = (int) (waterBaseY + 7 + plankWave);
        g.setColor(COLOR_FLOOD_DRIFTWOOD);
        g.fillRect(plankX, plankY, 32, 6);
        g.setColor(new Color(60, 42, 25));
        g.drawRect(plankX, plankY, 32, 6);

        // 5. Floating Coconut Husk / Debris bobbing on right
        double coconutWave = Math.sin(animTick * 2.0 + 620 * 0.025) * 3.2;
        int cocoX = 625;
        int cocoY = (int) (waterBaseY + 8 + coconutWave);
        g.setColor(new Color(75, 48, 28));
        g.fillOval(cocoX, cocoY, 14, 10);
        g.setColor(new Color(45, 28, 15));
        g.drawOval(cocoX, cocoY, 14, 10);
    }

    /**
     * Draws heads-up display badges during active gameplay.
     */
    private void drawHUD(Graphics2D g, int width, int height,
                        ScoreManager scoreManager,
                        DifficultyManager difficultyManager,
                        SoundManager soundManager) {
        // 1. Score Pill (Left)
        drawPillBadge(g, 18, 14, 150, 40, COLOR_PILL_SCORE_BG, COLOR_PILL_SCORE_BORDER);
        drawMiniWaterDrop(g, 30, 22);
        g.setFont(fontHud);
        g.setColor(Color.WHITE);
        g.drawString("SCORE: " + scoreManager.getCurrentScore(), 50, 40);

        // 2. High Score Pill
        drawPillBadge(g, 180, 14, 140, 40, COLOR_PILL_BEST_BG, COLOR_PILL_BEST_BORDER);
        drawMiniStar(g, 194, 32);
        g.setFont(fontHud);
        g.setColor(new Color(255, 230, 120));
        g.drawString("BEST: " + scoreManager.getHighScore(), 212, 40);

        // 3. Difficulty / Level Badge (Center-Right)
        int diffPillWidth = 210;
        int diffPillX = width - diffPillWidth - 95;
        drawPillBadge(g, diffPillX, 14, diffPillWidth, 40, COLOR_PILL_DIFF_BG, COLOR_PILL_DIFF_BORDER);

        g.setFont(fontHudSmall);
        g.setColor(new Color(230, 240, 255));
        String levelStr = "LVL " + difficultyManager.getLevel() + " • " + difficultyManager.getLevelTitle();
        g.drawString(levelStr, diffPillX + 14, 31);

        // Difficulty mini progress bar
        int barW = diffPillWidth - 28;
        int barH = 5;
        int barX = diffPillX + 14;
        int barY = 38;
        g.setColor(new Color(30, 45, 68));
        g.fillRoundRect(barX, barY, barW, barH, 3, 3);

        int fillW = (int) (barW * difficultyManager.getDifficultyFactor());
        GradientPaint barGrad = new GradientPaint(
            barX, barY, new Color(75, 195, 255),
            barX + barW, barY, new Color(255, 110, 75)
        );
        g.setPaint(barGrad);
        g.fillRoundRect(barX, barY, fillW, barH, 3, 3);

        // 4. Sound & BGM Toggle Indicator (Top Right)
        int soundX = width - 80;
        drawPillBadge(g, soundX, 14, 62, 40, COLOR_PILL_DIFF_BG, COLOR_PILL_DIFF_BORDER);
        g.setFont(fontHudSmall);
        g.setColor(soundManager.isMuted() ? new Color(255, 130, 130) : new Color(130, 255, 150));
        g.drawString(soundManager.isMuted() ? "[M] 🔇" : "[M] 🔊", soundX + 9, 39);
    }

    /**
     * Draws the Main Menu screen.
     */
    private void drawMenu(Graphics2D g, int width, int height,
                          ScoreManager scoreManager,
                          SoundManager soundManager) {
        g.setColor(COLOR_MENU_BACKDROP);
        g.fillRect(0, 0, width, height);

        int centerX = width / 2;

        // Floating Title Effect
        double floatOffset = Math.sin(animTick * 1.2) * 6.0;
        int titleY = (int) (140 + floatOffset);

        // Title 3D Shadow
        g.setFont(fontTitleLarge);
        FontMetrics fmTitle = g.getFontMetrics();
        String title = GameConfig.GAME_TITLE;
        int titleWidth = fmTitle.stringWidth(title);

        g.setColor(new Color(10, 20, 40, 200));
        g.drawString(title, centerX - titleWidth / 2 + 3, titleY + 4);

        // Title Gradient (White to Turquoise Sky)
        GradientPaint titleGrad = new GradientPaint(
            centerX - titleWidth / 2, titleY - 35, Color.WHITE,
            centerX + titleWidth / 2, titleY + 10, new Color(110, 225, 255)
        );
        g.setPaint(titleGrad);
        g.drawString(title, centerX - titleWidth / 2, titleY);

        // Subtitle
        g.setFont(fontBody);
        g.setColor(new Color(230, 245, 255));
        String subtitle = "Catch the falling raindrops, avoid the lethal bombs!";
        int subW = g.getFontMetrics().stringWidth(subtitle);
        g.drawString(subtitle, centerX - subW / 2, titleY + 30);

        // Animated "PRESS SPACE TO START" Banner
        int promptY = titleY + 75;
        float pulse = (float) (0.85 + 0.15 * Math.sin(animTick * 3.0));
        int btnW = 320;
        int btnH = 50;
        int btnX = centerX - btnW / 2;

        drawPillBadge(g, btnX, promptY, btnW, btnH,
                new Color(30, 150, 80, (int) (220 * pulse)),
                new Color(120, 255, 170));

        g.setFont(fontPrompt);
        g.setColor(Color.WHITE);
        String prompt = "PRESS SPACE OR ENTER TO START";
        int pW = g.getFontMetrics().stringWidth(prompt);
        g.drawString(prompt, centerX - pW / 2, promptY + 32);

        // Instructions & Controls Card
        int cardW = 500;
        int cardH = 185;
        int cardX = centerX - cardW / 2;
        int cardY = promptY + 68;

        g.setColor(COLOR_MENU_CARD_BG);
        g.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
        g.setColor(COLOR_MENU_CARD_BORDER);
        g.setStroke(STROKE_1_8);
        g.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);

        // Card Content
        g.setFont(fontHud);
        g.setColor(new Color(255, 215, 100));
        g.drawString("HOW TO PLAY & CONTROLS", cardX + 24, cardY + 28);

        g.setFont(fontBody);
        g.setColor(new Color(235, 245, 255));
        int textY = cardY + 54;
        int stepY = 24;
        g.drawString("• [A] / [D] or [◄] / [►] Arrows : Move Basket Left / Right", cardX + 24, textY);
        g.drawString("• [Mouse Cursor] : Smoothly moves basket to cursor", cardX + 24, textY + stepY);
        g.drawString("• [ESC] : Pause / Resume game", cardX + 24, textY + stepY * 2);
        g.drawString("• [M] : Toggle Music & Sound FX (Mute / Unmute)", cardX + 24, textY + stepY * 3);
        g.drawString("• Raindrop = +1 Point  |  Bomb = Instant Game Over!", cardX + 24, textY + stepY * 4);
        g.drawString("• BGM: 'Over the Moon' plays when game starts 🎵", cardX + 24, textY + stepY * 5);

        // High Score Record Pill
        int recordW = 290;
        int recordY = cardY + cardH + 16;
        drawPillBadge(g, centerX - recordW / 2, recordY, recordW, 36,
                new Color(45, 35, 15, 210),
                new Color(255, 195, 60));
        g.setFont(fontHud);
        g.setColor(new Color(255, 225, 110));
        String recStr = "★ ALL-TIME RECORD: " + scoreManager.getHighScore() + " DROPS ★";
        int rW = g.getFontMetrics().stringWidth(recStr);
        g.drawString(recStr, centerX - rW / 2, recordY + 24);
    }

    /**
     * Draws the Pause Overlay.
     */
    private void drawPauseOverlay(Graphics2D g, int width, int height) {
        g.setColor(COLOR_PAUSE_BACKDROP);
        g.fillRect(0, 0, width, height);

        int centerX = width / 2;
        int centerY = height / 2;

        int cardW = 380;
        int cardH = 220;
        int cardX = centerX - cardW / 2;
        int cardY = centerY - cardH / 2 - 10;

        g.setColor(COLOR_PAUSE_CARD_BG);
        g.fillRoundRect(cardX, cardY, cardW, cardH, 24, 24);
        g.setColor(COLOR_PAUSE_CARD_BORDER);
        g.setStroke(STROKE_2_0);
        g.drawRoundRect(cardX, cardY, cardW, cardH, 24, 24);

        g.setFont(fontTitleMedium);
        g.setColor(Color.WHITE);
        String paused = "GAME PAUSED";
        int pW = g.getFontMetrics().stringWidth(paused);
        g.drawString(paused, centerX - pW / 2, cardY + 50);

        g.setFont(fontBody);
        g.setColor(new Color(220, 235, 255));
        int textY = cardY + 95;
        int step = 28;

        String r1 = "Press [ESC] to Resume";
        String r2 = "Press [M] to Toggle Sound & Music";
        String r3 = "Press [Q] for Menu | [SPACE] to Restart";

        g.drawString(r1, centerX - g.getFontMetrics().stringWidth(r1) / 2, textY);
        g.drawString(r2, centerX - g.getFontMetrics().stringWidth(r2) / 2, textY + step);
        g.drawString(r3, centerX - g.getFontMetrics().stringWidth(r3) / 2, textY + step * 2);
    }

    /**
     * Draws the Game Over Overlay.
     */
    private void drawGameOverOverlay(Graphics2D g, int width, int height,
                                    ScoreManager scoreManager,
                                    DifficultyManager difficultyManager) {
        g.setColor(COLOR_GAMEOVER_BACKDROP);
        g.fillRect(0, 0, width, height);

        int centerX = width / 2;
        int centerY = height / 2;

        int cardW = 460;
        int cardH = 340;
        int cardX = centerX - cardW / 2;
        int cardY = centerY - cardH / 2 - 15;

        g.setColor(COLOR_GAMEOVER_CARD_BG);
        g.fillRoundRect(cardX, cardY, cardW, cardH, 24, 24);
        g.setColor(COLOR_GAMEOVER_CARD_BORDER);
        g.setStroke(STROKE_2_2);
        g.drawRoundRect(cardX, cardY, cardW, cardH, 24, 24);

        g.setFont(fontTitleLarge);
        g.setColor(new Color(255, 75, 60));
        String goStr = "GAME OVER";
        int goW = g.getFontMetrics().stringWidth(goStr);
        g.drawString(goStr, centerX - goW / 2, cardY + 60);

        int scoreStartY = cardY + 105;
        if (scoreManager.isNewRecordAchieved()) {
            float pulse = (float) (0.8 + 0.2 * Math.sin(animTick * 4.0));
            int recW = 280;
            drawPillBadge(g, centerX - recW / 2, scoreStartY, recW, 34,
                    new Color(200, 150, 20, (int) (230 * pulse)),
                    new Color(255, 240, 130));
            g.setFont(fontHud);
            g.setColor(Color.WHITE);
            String recMsg = "★ NEW HIGH SCORE! ★";
            g.drawString(recMsg, centerX - g.getFontMetrics().stringWidth(recMsg) / 2, scoreStartY + 23);
            scoreStartY += 48;
        }

        int statW = 180;
        int statH = 55;
        int statGap = 20;
        int statX1 = centerX - statW - statGap / 2;
        int statX2 = centerX + statGap / 2;

        // Current Score Card
        drawPillBadge(g, statX1, scoreStartY, statW, statH,
                new Color(30, 45, 75, 210), new Color(110, 180, 255));
        g.setFont(fontHudSmall);
        g.setColor(new Color(180, 215, 255));
        g.drawString("DROPS CAUGHT", statX1 + 16, scoreStartY + 20);
        g.setFont(fontTitleMedium);
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(scoreManager.getCurrentScore()), statX1 + 16, scoreStartY + 47);

        // High Score Card
        drawPillBadge(g, statX2, scoreStartY, statW, statH,
                new Color(55, 42, 20, 210), new Color(255, 200, 75));
        g.setFont(fontHudSmall);
        g.setColor(new Color(255, 220, 140));
        g.drawString("ALL-TIME BEST", statX2 + 16, scoreStartY + 20);
        g.setFont(fontTitleMedium);
        g.setColor(new Color(255, 230, 110));
        g.drawString(String.valueOf(scoreManager.getHighScore()), statX2 + 16, scoreStartY + 47);

        // Survival info
        g.setFont(fontHudSmall);
        g.setColor(new Color(220, 220, 230));
        long seconds = difficultyManager.getElapsedMillis() / 1000;
        String survInfo = "Survived: " + seconds + "s  |  Reached: Level "
                + difficultyManager.getLevel() + " (" + difficultyManager.getLevelTitle() + ")";
        g.drawString(survInfo, centerX - g.getFontMetrics().stringWidth(survInfo) / 2, scoreStartY + statH + 28);

        // Restart Prompt Button
        int btnW = 320;
        int btnH = 46;
        int btnX = centerX - btnW / 2;
        int btnY = cardY + cardH - 68;

        drawPillBadge(g, btnX, btnY, btnW, btnH,
                new Color(40, 140, 70, 230),
                new Color(130, 255, 175));

        g.setFont(fontPrompt);
        g.setColor(Color.WHITE);
        String prompt = "PRESS SPACE TO PLAY AGAIN";
        g.drawString(prompt, centerX - g.getFontMetrics().stringWidth(prompt) / 2, btnY + 29);

        // Menu subprompt
        g.setFont(fontHudSmall);
        g.setColor(new Color(200, 200, 210));
        String escPrompt = "Press [ESC] for Main Menu";
        g.drawString(escPrompt, centerX - g.getFontMetrics().stringWidth(escPrompt) / 2, cardY + cardH + 24);
    }

    private void drawPillBadge(Graphics2D g, int x, int y, int width, int height,
                              Color bg, Color border) {
        g.setColor(bg);
        g.fillRoundRect(x, y, width, height, height, height);
        g.setColor(border);
        g.setStroke(STROKE_1_5);
        g.drawRoundRect(x, y, width, height, height, height);
    }

    private void drawMiniWaterDrop(Graphics2D g, int x, int y) {
        Path2D.Double drop = new Path2D.Double();
        drop.moveTo(x + 5, y);
        drop.curveTo(x + 9, y + 4, x + 10, y + 9, x + 5, y + 14);
        drop.curveTo(x, y + 9, x + 1, y + 4, x + 5, y);
        drop.closePath();
        g.setColor(new Color(110, 215, 255));
        g.fill(drop);
    }

    private void drawMiniStar(Graphics2D g, int x, int y) {
        g.setColor(new Color(255, 215, 60));
        g.fillOval(x - 6, y - 6, 12, 12);
        g.setColor(Color.WHITE);
        g.fillOval(x - 2, y - 2, 4, 4);
    }
}
