package com.raincatcher.logic;

import com.raincatcher.audio.SoundManager;
import com.raincatcher.config.GameConfig;
import com.raincatcher.model.Basket;
import com.raincatcher.model.Bomb;
import com.raincatcher.model.FallingObject;
import com.raincatcher.model.Particle;
import com.raincatcher.model.Raindrop;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Handles collision detection between the basket and falling objects,
 * ground impacts, particle effect generation, and score triggers.
 */
public class CollisionManager {

    private final Random random = new Random();

    /**
     * Checks collisions and updates active objects.
     *
     * @param basket The player basket
     * @param fallingObjects Active falling objects list
     * @param particles Active particle effect list
     * @param scoreManager Score manager
     * @param soundManager Sound manager
     * @param screenHeight Visible screen height
     * @return true if a lethal bomb collision occurred (causing Game Over)
     */
    public boolean checkCollisions(Basket basket,
                                   List<FallingObject> fallingObjects,
                                   List<Particle> particles,
                                   ScoreManager scoreManager,
                                   SoundManager soundManager,
                                   int screenHeight) {
        boolean gameOver = false;
        Rectangle2D.Double basketCatchZone = basket.getCatchBounds();
        Rectangle2D.Double basketBodyZone = basket.getBodyBounds();

        Iterator<FallingObject> iterator = fallingObjects.iterator();
        while (iterator.hasNext()) {
            FallingObject obj = iterator.next();
            if (!obj.isActive() || obj.isCollected()) {
                iterator.remove();
                continue;
            }

            Rectangle2D.Double objBounds = obj.getBounds();

            if (obj instanceof Raindrop) {
                // Raindrop caught in top basket opening
                if (basketCatchZone.intersects(objBounds)) {
                    obj.setCollected(true);
                    obj.setActive(false);
                    iterator.remove();

                    scoreManager.addPoints(GameConfig.RAINDROP_POINTS);
                    soundManager.playCatch();
                    createSplash(particles, obj.getCenterX(), basket.getY() + 6, 12);
                    continue;
                }
            } else if (obj instanceof Bomb) {
                // Bomb colliding with basket body
                if (basketBodyZone.intersects(objBounds)) {
                    obj.setCollected(true);
                    obj.setActive(false);
                    iterator.remove();

                    soundManager.playExplosion();
                    createExplosion(particles, obj.getCenterX(), obj.getCenterY());
                    gameOver = true;
                    continue;
                }
            }

            // Check if object fell past the bottom of the screen
            if (obj.isOutOfBounds(screenHeight)) {
                obj.setActive(false);
                iterator.remove();

                // Subtle ground splash for missed drops
                if (obj instanceof Raindrop) {
                    createGroundSplash(particles, obj.getCenterX(), GameConfig.PLAY_AREA_BOTTOM);
                }
            }
        }

        return gameOver;
    }

    /**
     * Generates a burst of water droplets when a raindrop is caught.
     */
    public void createSplash(List<Particle> particles, double x, double y, int count) {
        if (particles == null) return;
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI * (0.15 + 0.70 * random.nextDouble()); // Upward arc
            double speed = 2.0 + random.nextDouble() * 4.5;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            double gravity = 0.22;
            float size = 3.0f + random.nextFloat() * 3.5f;
            float growth = -0.05f;
            int maxLife = 18 + random.nextInt(14);
            Color c = random.nextBoolean()
                    ? new Color(140, 220, 255, 230)
                    : new Color(255, 255, 255, 240);
            particles.add(new Particle(x, y, vx, vy, gravity, size, growth, c, maxLife, Particle.ParticleType.WATER_SPLASH));
        }
    }

    /**
     * Generates a subtle puddle splash when a drop hits the ground.
     */
    public void createGroundSplash(List<Particle> particles, double x, double y) {
        if (particles == null) return;
        for (int i = 0; i < 4; i++) {
            double vx = (random.nextDouble() - 0.5) * 3.0;
            double vy = -1.0 - random.nextDouble() * 2.0;
            particles.add(new Particle(x, y, vx, vy, 0.20, 2.5f, -0.05f,
                    new Color(170, 225, 255, 180), 12, Particle.ParticleType.WATER_SPLASH));
        }
    }

    /**
     * Generates a dramatic multi-layered explosion when a bomb strikes the basket.
     */
    public void createExplosion(List<Particle> particles, double x, double y) {
        if (particles == null) return;

        // Expanding shockwave ring
        particles.add(new Particle(x, y, 0, 0, 0, 10.0f, 6.0f,
                new Color(255, 220, 100, 240), 20, Particle.ParticleType.SHOCKWAVE));

        // Fire burst particles
        for (int i = 0; i < 35; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.5 + random.nextDouble() * 7.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            float size = 6.0f + random.nextFloat() * 10.0f;
            float growth = -0.15f;
            int life = 20 + random.nextInt(25);
            Color fireColor = random.nextBoolean()
                    ? new Color(255, 70, 20, 240)
                    : new Color(255, 200, 40, 240);
            particles.add(new Particle(x, y, vx, vy, 0.05, size, growth, fireColor, life, Particle.ParticleType.EXPLOSION_FIRE));
        }

        // Dark billowing smoke puffs
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.5 + random.nextDouble() * 3.5;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed - 0.8; // Drifts upward
            float size = 12.0f + random.nextFloat() * 14.0f;
            float growth = 0.4f;
            int life = 30 + random.nextInt(30);
            Color smokeColor = new Color(50, 50, 55, 180);
            particles.add(new Particle(x, y, vx, vy, -0.02, size, growth, smokeColor, life, Particle.ParticleType.EXPLOSION_SMOKE));
        }
    }
}

