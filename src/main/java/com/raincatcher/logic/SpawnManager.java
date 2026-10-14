package com.raincatcher.logic;

import com.raincatcher.config.GameConfig;
import com.raincatcher.model.Bomb;
import com.raincatcher.model.FallingObject;
import com.raincatcher.model.Raindrop;

import java.util.List;
import java.util.Random;

/** THIS PAGE IS FOR: basahin nyo maayos
 * Manages procedural spawning of raindrops and bombs.
 * Applies fairness rules to prevent unfair unavoidable bomb walls.
 */
public class SpawnManager {

    private final Random random;
    private long timeSinceLastSpawn = 0;
    private int consecutiveBombs = 0;
    private double lastBombX = -1;

    public SpawnManager() {
        this(new Random());
    }

    public SpawnManager(Random random) {
        this.random = random;
    }

    /**
     * Resets spawn timers and fairness counters.
     */
    public void reset() {
        timeSinceLastSpawn = 0;
        consecutiveBombs = 0;
        lastBombX = -1;
    }

    /**
     * Updates spawn accumulator and generates new objects when intervals elapse.
     *
     * @param deltaMillis Elapsed milliseconds this tick
     * @param difficultyManager Source of dynamic speed, interval, and bomb probability
     * @param activeObjects List to append newly spawned objects to
     * @param screenWidth Current playable screen width
     */
    public void update(long deltaMillis, DifficultyManager difficultyManager,
                       List<FallingObject> activeObjects, int screenWidth) {
        timeSinceLastSpawn += deltaMillis;
        int targetInterval = difficultyManager.getCurrentSpawnIntervalMs();

        if (timeSinceLastSpawn >= targetInterval) {
            timeSinceLastSpawn = 0;
            spawnNextObject(difficultyManager, activeObjects, screenWidth);
        }
    }

    /**
     * Spawns a raindrop or bomb applying fairness constraints.
     */
    public FallingObject spawnNextObject(DifficultyManager difficultyManager,
                                         List<FallingObject> activeObjects,
                                         int screenWidth) {
        double bombProb = difficultyManager.getCurrentBombProbability();
        double speed = difficultyManager.getCurrentFallingSpeed();

        // Add slight random speed variance (+/- 10%) so objects don't fall in rigid locks
        double variance = (random.nextDouble() * 0.20 - 0.10) * speed;
        double objectSpeed = Math.max(1.5, speed + variance);

        boolean spawnBomb = random.nextDouble() < bombProb;

        // Fairness rule: prevent too many consecutive bombs
        if (consecutiveBombs >= GameConfig.MAX_CONSECUTIVE_BOMBS) {
            spawnBomb = false;
        }

        FallingObject created;
        int margin = 20;

        if (spawnBomb) {
            int diameter = GameConfig.BOMB_DIAMETER;
            int maxSpawnX = screenWidth - margin - diameter;
            double spawnX;

            // Fairness rule: ensure bomb doesn't spawn right on top of previous bomb's lane
            int attempts = 0;
            do {
                spawnX = margin + random.nextInt(Math.max(1, maxSpawnX - margin));
                attempts++;
            } while (attempts < 5 && lastBombX >= 0
                    && Math.abs(spawnX - lastBombX) < GameConfig.MIN_BOMB_HORIZONTAL_SEPARATION);

            lastBombX = spawnX;
            consecutiveBombs++;
            created = new Bomb(spawnX, GameConfig.SPAWN_START_Y, diameter, objectSpeed);
        } else {
            int width = GameConfig.RAINDROP_WIDTH;
            int height = GameConfig.RAINDROP_HEIGHT;
            int maxSpawnX = screenWidth - margin - width;
            double spawnX = margin + random.nextInt(Math.max(1, maxSpawnX - margin));

            consecutiveBombs = 0;
            created = new Raindrop(spawnX, GameConfig.SPAWN_START_Y, width, height, objectSpeed);
        }

        if (activeObjects != null) {
            activeObjects.add(created);
        }
        return created;
    }
}
