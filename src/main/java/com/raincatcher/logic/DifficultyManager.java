package com.raincatcher.logic;

import com.raincatcher.config.GameConfig;

/** INTINDIHIN NYO HA
 * Manages progressive gameplay difficulty. As time elapses and score increases,
 * falling speed increases, spawn intervals shorten, and bomb probability climbs.
 */
public class DifficultyManager {

    private long elapsedMillis = 0;
    private double difficultyFactor = 0.0; // Normalized 0.0 (easiest) to 1.0 (hardest)

    public DifficultyManager() {
        reset();
    }

    /**
     * Resets difficulty tracking.
     */
    public void reset() {
        elapsedMillis = 0;
        difficultyFactor = 0.0;
    }

    /**
     * Updates elapsed time and recalculates difficulty parameters.
     *
     * @param deltaMillis Elapsed milliseconds since last update
     * @param currentScore Current player score
     */
    public void update(long deltaMillis, int currentScore) {
        elapsedMillis += deltaMillis;

        // Difficulty scales smoothly using both elapsed time and score achieved
        double timeRatio = Math.min(1.0, elapsedMillis / (GameConfig.SECONDS_TO_MAX_DIFFICULTY * 1000.0));
        double scoreRatio = Math.min(1.0, (double) currentScore / GameConfig.SCORE_FOR_MAX_DIFFICULTY);

        // Blended progression (60% time, 40% score for responsive ramping)
        this.difficultyFactor = Math.min(1.0, 0.60 * timeRatio + 0.40 * scoreRatio);
    }

    /**
     * Calculates current falling speed in pixels per frame.
     */
    public double getCurrentFallingSpeed() {
        return GameConfig.INITIAL_FALLING_SPEED
                + difficultyFactor * (GameConfig.MAX_FALLING_SPEED - GameConfig.INITIAL_FALLING_SPEED);
    }

    /**
     * Calculates current spawn interval in milliseconds.
     */
    public int getCurrentSpawnIntervalMs() {
        return (int) Math.round(GameConfig.INITIAL_SPAWN_INTERVAL_MS
                - difficultyFactor * (GameConfig.INITIAL_SPAWN_INTERVAL_MS - GameConfig.MIN_SPAWN_INTERVAL_MS));
    }

    /**
     * Calculates current bomb spawn probability [0.0 - 1.0].
     */
    public double getCurrentBombProbability() {
        return GameConfig.INITIAL_BOMB_PROBABILITY
                + difficultyFactor * (GameConfig.MAX_BOMB_PROBABILITY - GameConfig.INITIAL_BOMB_PROBABILITY);
    }

    /**
     * Returns the 1-based difficulty level (1 to 5).
     */
    public int getLevel() {
        if (difficultyFactor < 0.20) return 1;
        if (difficultyFactor < 0.45) return 2;
        if (difficultyFactor < 0.70) return 3;
        if (difficultyFactor < 0.90) return 4;
        return 5;
    }

    /**
     * Returns a thematic descriptive title for the current difficulty tier.
     */
    public String getLevelTitle() {
        switch (getLevel()) {
            case 1:  return "Gentle Drizzle";
            case 2:  return "Steady Shower";
            case 3:  return "Sudden Downpour";
            case 4:  return "Heavy Storm";
            default: return "Flash Flood!";
        }
    }

    public double getDifficultyFactor() {
        return difficultyFactor;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }
}
