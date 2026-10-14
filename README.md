# RAIN CATCHER — BBM INSPIRED

> **Presentation & Defense Survival Manual**: This document provides a complete, line-by-line and section-by-section breakdown of every class, method, algorithm, and data structure in the **Rain Catcher** codebase. Use this guide during code defense and live presentation to explain the engineering decisions, answer teacher questions, and quickly debug any changes the teacher makes during evaluation.

---

## Table of Contents
1. [Quick Presentation Overview & OOP Design](#1-quick-presentation-overview--oop-design)
2. [Teacher Defense Cheat-Sheet (Fast Lookup)](#2-teacher-defense-cheat-sheet-fast-lookup)
3. [Section-by-Section Code Review](#3-section-by-section-code-review)
   - [Config: `GameConfig.java`](#config-gameconfigjava)
   - [Bootstrap: `Main.java`](#bootstrap-mainjava)
   - [State Machine: `GameState.java`](#state-machine-gamestatejava)
   - [Player Entity: `Basket.java`](#player-entity-basketjava)
   - [Base Class: `FallingObject.java`](#base-class-fallingobjectjava)
   - [Falling Entities: `Raindrop.java` & `Bomb.java`](#falling-entities-raindropjava--bombjava)
   - [Effects: `Particle.java`](#effects-particlejava)
   - [Atmosphere: `Cloud.java`](#atmosphere-cloudjava)
   - [Progression Engine: `DifficultyManager.java`](#progression-engine-difficultymanagerjava)
   - [Procedural Spawner: `SpawnManager.java`](#procedural-spawner-spawnmanagerjava)
   - [Physics & Collisions: `CollisionManager.java`](#physics--collisions-collisionmanagerjava)
   - [Persistence: `ScoreManager.java`](#persistence-scoremanagerjava)
   - [Procedural Audio: `SoundManager.java`](#procedural-audio-soundmanagerjava)
   - [BGM Streaming: `MusicPlayer.java`](#bgm-streaming-musicplayerjava)
   - [Graphics & Java2D: `Renderer.java`](#graphics--java2d-rendererjava)
   - [Main Loop & Controller: `GamePanel.java`](#main-loop--controller-gamepaneljava)
   - [Window Lifecycle: `GameWindow.java`](#window-lifecycle-gamewindowjava)
   - [Automated Testing: `GameLogicTest.java`](#automated-testing-gamelogictestjava)
4. [Live Defense Debugging Scenarios ("Teacher Messed With Code")](#4-live-defense-debugging-scenarios-teacher-messed-with-code)
5. [Windows PowerShell Commands to Compile, Test & Run](#5-windows-powershell-commands-to-compile-test--run)

---

## 1. Quick Presentation Overview & OOP Design

### Core Concept
**Rain Catcher** is a native 2D desktop arcade survival game written **strictly in Java** using **Java Swing** (`JFrame`, `JPanel`, `javax.swing.Timer`) and **Java2D** (`Graphics2D`). The player maneuvers a basket along the bottom of the screen to catch falling raindrops (+1 point) while dodging lethal falling bombs (instant Game Over).

### Applied Object-Oriented Principles:
1. **Encapsulation**: Every class protects its internal state (`private` fields with validated getters/setters). For example, `Basket` coordinates are strictly clamped to window boundaries inside `update()`.
2. **Inheritance & Polymorphism**: `FallingObject` is an abstract base class. `Raindrop` and `Bomb` extend `FallingObject` and provide custom `render()` geometry, particle triggers, and collision consequences. The `CollisionManager` and `GamePanel` interact with `List<FallingObject>` polymorphically.
3. **Separation of Concerns (MVC-inspired)**:
   - **Model**: `Basket`, `Raindrop`, `Bomb`, `Particle`, `Cloud`, `GameState`.
   - **Logic / Controller**: `SpawnManager`, `CollisionManager`, `DifficultyManager`, `ScoreManager`.
   - **View / Presentation**: `Renderer`, `GamePanel`, `GameWindow`.
   - **Audio Engine**: `SoundManager` (PCM procedural waveform generator) and `MusicPlayer` (background daemon thread MP3 streamer).
4. **Zero-Allocation Rendering**: `Renderer` uses primitive drawing calls (`fillRoundRect`, `fillOval`, pre-cached `BasicStroke` and `GradientPaint`) during the 60 FPS render loop to prevent garbage collection stutter.

---

## 2. Teacher Defense Cheat-Sheet (Fast Lookup)

If the teacher asks *"Where is X located?"*, use this table:

| Question / Feature | File Location | Key Method or Constant |
| :--- | :--- | :--- |
| **Where are the game parameters (speed, size, delay)?** | `com.raincatcher.config.GameConfig` | Static constants: `INITIAL_FALLING_SPEED`, `BOMB_DIAMETER`, etc. |
| **Where does the 60 FPS game loop tick?** | `com.raincatcher.ui.GamePanel` | `gameLoopStep()` driven by `javax.swing.Timer` |
| **How does the basket move and stop at borders?** | `com.raincatcher.model.Basket` | `update(int screenWidth)` |
| **Where is collision detection calculated?** | `com.raincatcher.logic.CollisionManager` | `checkCollisions(...)` using `Rectangle2D.intersects()` |
| **Why doesn't the basket catch bombs in the rim?** | `com.raincatcher.model.Basket` | Separate `getCatchBounds()` (top rim) vs `getBodyBounds()` (full basket) |
| **Where is progressive difficulty scaled?** | `com.raincatcher.logic.DifficultyManager` | `update(deltaMillis, score)` using blended time + score interpolation |
| **How are impossible bomb walls prevented?** | `com.raincatcher.logic.SpawnManager` | `spawnNextObject()`: `MAX_CONSECUTIVE_BOMBS` & `MIN_BOMB_HORIZONTAL_SEPARATION` |
| **Where is high score saved permanently?** | `com.raincatcher.logic.ScoreManager` | `saveHighScore()` via `java.util.prefs.Preferences` + `.raincatcher_highscore.dat` |
| **How are sound effects played without .wav files?** | `com.raincatcher.audio.SoundManager` | `generateWaterBloopSound()`, `generateExplosionSound()` (math PCM synthesis) |
| **Where does background music play and pause?** | `com.raincatcher.audio.MusicPlayer` | Daemon thread streaming `audio/over the moon.mp3` via JLayer |
| **Where are the broken houses & floodwater drawn?** | `com.raincatcher.ui.Renderer` | `drawFloodedVillage()` and `drawFloodwater()` |
| **Where do raindrops emerge from clouds?** | `com.raincatcher.logic.SpawnManager` | Spawns at `y = GameConfig.SPAWN_START_Y` (48px, under cloud base) |

---

## 3. Section-by-Section Code Review

---

### Config: `GameConfig.java`
- **Location**: `src/main/java/com/raincatcher/config/GameConfig.java`
- **Purpose**: Central repository for all numeric balance constants, dimensions, and difficulty thresholds. Prevents "magic numbers" in logic classes.

#### Key Lines Explained:
```java
public static final int WINDOW_WIDTH = 800;
public static final int WINDOW_HEIGHT = 650;
public static final int TARGET_FPS = 60;
public static final int TIMER_DELAY_MS = 1000 / TARGET_FPS; // ~16 ms
```
- `TIMER_DELAY_MS`: Set to `16` milliseconds ($1000 / 60 \approx 16.6\text{ ms}$). This is passed into `javax.swing.Timer` to achieve a stable 60 updates per second.
```java
public static final int TOP_CLOUD_HEIGHT = 75;
public static final int SPAWN_START_Y = 48;
```
- `SPAWN_START_Y = 48`: Items spawn at $Y = 48$, directly beneath the puffy cloud underside ($Y = 32\text{--}70$), making items visibly emerge from the clouds.
```java
public static final double INITIAL_FALLING_SPEED = 2.5; // Pixels per frame
public static final double MAX_FALLING_SPEED = 10.5;
public static final int INITIAL_SPAWN_INTERVAL_MS = 1100; // Milliseconds between spawns
public static final int MIN_SPAWN_INTERVAL_MS = 350;
public static final double INITIAL_BOMB_PROBABILITY = 0.12; // 12% bomb chance at start
public static final double MAX_BOMB_PROBABILITY = 0.25;      // 25% max bomb chance
```
- Defines the difficulty progression envelope.
```java
public static final int MIN_BOMB_HORIZONTAL_SEPARATION = 160;
public static final int MAX_CONSECUTIVE_BOMBS = 2;
```
- **Fairness Rules**: Guarantees that at least one basket width ($96\text{ px}$) of clearance remains open, and prevents more than 2 bombs in a row.

#### If Teacher Messes With This:
- **Teacher sets `INITIAL_FALLING_SPEED = 100`**: Objects move 100 pixels per 16ms frame, skipping over the basket hitbox.
  - *Fix*: Keep speed between `2.0` and `14.0`.
- **Teacher sets `MIN_SPAWN_INTERVAL_MS = 0`**: Causes infinite objects to spawn on a single frame.
  - *Fix*: Keep spawn interval $\ge 150\text{ ms}$.

---

### Bootstrap: `Main.java`
- **Location**: `src/main/java/com/raincatcher/Main.java`
- **Purpose**: Desktop application entry point.

#### Key Lines Explained:
```java
public static void main(String[] args) {
    System.setProperty("sun.java2d.d3d", "true"); // Direct3D acceleration on Windows
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable ignored) {}

    SwingUtilities.invokeLater(() -> {
        GameWindow window = new GameWindow();
        window.setVisible(true);
    });
}
```
- `System.setProperty("sun.java2d.d3d", "true")`: Activates hardware-accelerated Direct3D blitting on Windows.
- `SwingUtilities.invokeLater(...)`: **Critical Swing concept**. Guarantees that `GameWindow` is created and displayed on the **Event Dispatch Thread (EDT)**. In Swing, creating GUI elements on the main thread causes race conditions and UI lockups.

---

### State Machine: `GameState.java`
- **Location**: `src/main/java/com/raincatcher/model/GameState.java`
- **Purpose**: Defines four discrete game states:
  - `MENU`: Shows title, instructions, high score. No physics updates, background music paused.
  - `PLAYING`: Active 60 FPS physics updates, collision checks, spawn updates, and looping BGM.
  - `PAUSED`: Freezes physics updates; overlays pause dialog; pauses BGM.
  - `GAME_OVER`: Displays death screen, final score, new record ribbon, and pauses BGM.

---

### Player Entity: `Basket.java`
- **Location**: `src/main/java/com/raincatcher/model/Basket.java`
- **Purpose**: Handles player basket positioning, keyboard/mouse input smoothing, tilt animation, and dual hitboxes.

#### Key Logic & Physics:
```java
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
        if (movingLeft && !movingRight) { velocity = -speed; x -= speed; }
        else if (movingRight && !movingLeft) { velocity = speed; x += speed; }
    }

    // Boundary Clamping
    if (x < 4) { x = 4; velocity = 0; }
    else if (x > screenWidth - width - 4) { x = screenWidth - width - 4; velocity = 0; }

    // Dynamic Tilt Interpolation
    if (velocity < -0.5) targetTilt = -0.06;      // ~ -3.5 degrees
    else if (velocity > 0.5) targetTilt = 0.06;   // ~ +3.5 degrees
    else targetTilt = 0.0;
    currentTilt += (targetTilt - currentTilt) * 0.2; // Smooth lerp
}
```
- **Boundary Clamping**: Prevents the basket from ever moving off the visible screen.
- **Tilt Lerp (`currentTilt += (targetTilt - currentTilt) * 0.2`)**: Implements exponential smoothing (Linear Interpolation) so the basket gently leans into movement rather than snapping rigidly.

#### The Dual Hitbox System:
```java
public Rectangle2D.Double getCatchBounds() {
    return new Rectangle2D.Double(x + 4, y + 2, width - 8, 18); // Top rim opening only
}

public Rectangle2D.Double getBodyBounds() {
    return new Rectangle2D.Double(x + 6, y + 6, width - 12, height - 8); // Full basket body
}
```
- **Why two hitboxes?** Raindrops should only count as "caught" when they land inside the basket's top rim opening (`getCatchBounds`). Bombs, however, are lethal if they strike any part of the basket (`getBodyBounds`).

---

### Base Class: `FallingObject.java`
- **Location**: `src/main/java/com/raincatcher/model/FallingObject.java`
- **Purpose**: Abstract template for all falling entities using OOP inheritance.

#### Key Methods:
- `update()`: Executes standard velocity integration: `y += speedY; x += speedX;`.
- `getBounds()`: Returns `Rectangle2D.Double(x, y, width, height)` for collision checks.
- `isOutOfBounds(int screenHeight)`: Returns `y > screenHeight + 20` to signal when missed objects should be removed from memory.
- `abstract void render(Graphics2D g)`: Forces subclasses (`Raindrop`, `Bomb`) to implement their own visuals.

---

### Falling Entities: `Raindrop.java` & `Bomb.java`

#### `Raindrop.java`:
- Constructs a teardrop shape using a cubic Bézier curve (`Path2D.Float`):
  ```java
  dropPath.moveTo(drawX + w * 0.5f, drawY);
  dropPath.curveTo(drawX + w * 0.85f, drawY + h * 0.35f, drawX + w, drawY + h * 0.65f, drawX + w * 0.5f, drawY + h);
  dropPath.curveTo(drawX, drawY + h * 0.65f, drawX + w * 0.15f, drawY + h * 0.35f, drawX + w * 0.5f, drawY);
  ```
- Fills with an aqua-to-deep-blue gradient (`new GradientPaint(...)`) and draws a 3D specular highlight oval to look like clear liquid.
- Hitbox is calibrated tightly around the droplet core: `new Rectangle2D.Double(x + 2, y + 4, width - 4, height - 6)`.

#### `Bomb.java`:
- Renders a 3D cast-iron sphere using `RadialGradientPaint` with 4 color stops (specular white-gray highlight offset to top-left, charcoal body, dark rim).
- Draws a curved twine fuse with an animated sparkling tip:
  ```java
  double flicker = Math.sin(fuseTick * 2.0) * 2.0;
  float sparkRadius = (float) (5.0 + Math.abs(flicker));
  ```
- Draws a red/amber hazard symbol on the front face that pulses in opacity.

---

### Effects: `Particle.java`
- **Location**: `src/main/java/com/raincatcher/model/Particle.java`
- **Purpose**: Lightweight particle engine supporting 5 distinct visual effects:
  1. `WATER_SPLASH`: Blue/white droplets bursting in upward arcs with gravity decay (`vy += gravity`).
  2. `EXPLOSION_FIRE`: Expanding yellow/orange fire puffs.
  3. `EXPLOSION_SMOKE`: Billowing gray smoke drifting upward (`vy -= 0.8`).
  4. `SHOCKWAVE`: Expanding circular shockwave ring drawn with stroke expansion.
  5. `SPARK`: Flickering fuse spark.
- **Alpha Fading**: Opacity maps to remaining lifetime: `int alpha = (int) (((float) life / maxLife) * color.getAlpha());`.

---

### Atmosphere: `Cloud.java`
- **Location**: `src/main/java/com/raincatcher/model/Cloud.java`
- **Purpose**: Creates parallax background motion.
- Clouds drift horizontally at speeds between `0.25` and `0.90` px/frame.
- When a cloud drifts past the right border (`x > screenWidth + 50`), it wraps back to the left (`x = -totalWidth - 30`).

---

### Progression Engine: `DifficultyManager.java`
- **Location**: `src/main/java/com/raincatcher/logic/DifficultyManager.java`
- **Purpose**: Smoothly scales difficulty over time and player score without sudden spikes.

#### The Progression Math:
```java
public void update(long deltaMillis, int currentScore) {
    elapsedMillis += deltaMillis;
    double timeRatio = Math.min(1.0, elapsedMillis / (GameConfig.SECONDS_TO_MAX_DIFFICULTY * 1000.0));
    double scoreRatio = Math.min(1.0, (double) currentScore / GameConfig.SCORE_FOR_MAX_DIFFICULTY);

    // Blended: 60% time weight, 40% score weight
    this.difficultyFactor = Math.min(1.0, 0.60 * timeRatio + 0.40 * scoreRatio);
}
```
- `difficultyFactor` is normalized between `0.0` (start) and `1.0` (peak).
- Dynamic outputs:
  - **Falling Speed**: $\text{speed} = \text{Initial} + \text{factor} \times (\text{Max} - \text{Initial})$ ($2.5 \to 10.5\text{ px}$).
  - **Spawn Interval**: $\text{interval} = \text{Initial} - \text{factor} \times (\text{Initial} - \text{Min})$ ($1100\text{ ms} \to 350\text{ ms}$).
  - **Bomb Probability**: $\text{prob} = \text{Initial} + \text{factor} \times (\text{Max} - \text{Initial})$ ($12\% \to 25\%$).

---

### Procedural Spawner: `SpawnManager.java`
- **Location**: `src/main/java/com/raincatcher/logic/SpawnManager.java`
- **Purpose**: Generates items at intervals determined by `DifficultyManager`.

#### The Fairness Algorithm:
```java
// Rule 1: Cap consecutive bombs
if (consecutiveBombs >= GameConfig.MAX_CONSECUTIVE_BOMBS) {
    spawnBomb = false;
}

// Rule 2: Enforce horizontal clearance between bombs
int attempts = 0;
do {
    spawnX = margin + random.nextInt(Math.max(1, maxSpawnX - margin));
    attempts++;
} while (attempts < 5 && lastBombX >= 0 
        && Math.abs(spawnX - lastBombX) < GameConfig.MIN_BOMB_HORIZONTAL_SEPARATION);
```
- **Why this is critical**: In random spawning, pure RNG can generate 4 bombs in adjacent lanes at the same instant, making dodging mathematically impossible. These two rules guarantee that the game remains skill-based and fair at all difficulty levels.

---

### Physics & Collisions: `CollisionManager.java`
- **Location**: `src/main/java/com/raincatcher/logic/CollisionManager.java`
- **Purpose**: Executes Axis-Aligned Bounding Box (AABB) intersection tests between basket and active objects.

#### Safe Iterator Removal Pattern:
```java
Iterator<FallingObject> iterator = fallingObjects.iterator();
while (iterator.hasNext()) {
    FallingObject obj = iterator.next();
    if (basketCatchZone.intersects(objBounds)) {
        obj.setCollected(true);
        obj.setActive(false);
        iterator.remove(); // Safely removes from collection without ConcurrentModificationException
        scoreManager.addPoints(1);
        soundManager.playCatch();
        createSplash(particles, obj.getCenterX(), basket.getY() + 6, 12);
    }
}
```
- Uses `java.util.Iterator.remove()` to safely remove caught/exploded items from the list during iteration without throwing `ConcurrentModificationException`.
- Each object has `collected` and `active` flags to guarantee **each item can only collide exactly once**.

---

### Persistence: `ScoreManager.java`
- **Location**: `src/main/java/com/raincatcher/logic/ScoreManager.java`
- **Purpose**: Manages current score, all-time record, and cross-session persistence.

#### How High Scores are Saved:
1. **Primary Mechanism**: Standard Java `java.util.prefs.Preferences` (`Preferences.userNodeForPackage(ScoreManager.class)`). On Windows, this stores the high score directly in the Windows Registry under `HKCU\Software\JavaSoft\Prefs\raincatcher`.
2. **Automatic Fallback Mechanism**: If the Windows Registry is restricted or throws a `SecurityException`, it automatically writes to `.raincatcher_highscore.dat` in the user's home directory (`System.getProperty("user.home")`).
- This guarantees persistence across application restarts regardless of OS permission restrictions.

---

### Procedural Audio: `SoundManager.java`
- **Location**: `src/main/java/com/raincatcher/audio/SoundManager.java`
- **Purpose**: Generates retro arcade sound effects purely in Java memory using mathematical PCM waveform generation. **Requires zero external audio files**.

#### The Sound Synthesis Math (44.1 kHz, 16-bit Mono PCM):
1. **Water Catch Bloop (`generateWaterBloopSound()`)**:
   - Sweeps frequency from $580\text{ Hz}$ up to $960\text{ Hz}$ over $140\text{ ms}$ with exponential decay envelope:
     $$\text{freq}(t) = 580 + (960 - 580) \cdot t^{0.7}$$
     $$\text{envelope}(t) = e^{-5t} \cdot \sin(\pi t)$$
   - Produces a sweet, bouncy water drop chime.
2. **Bomb Explosion (`generateExplosionSound()`)**:
   - Layers filtered white noise with a deep sub-bass drop from $110\text{ Hz}$ down to $35\text{ Hz}$ over $500\text{ ms}$.
3. **Sad Game Over Jingle (`generateGameOverSound()`)**:
   - Descending 4-note minor arpeggio ($G_4 \to F_4 \to E\flat_4 \to C_4$).
4. **New Record Fanfare (`generateNewRecordSound()`)**:
   - Ascending 4-note major arpeggio ($C_5 \to E_5 \to G_5 \to C_6$).
- All sounds are pre-rendered into `javax.sound.sampled.Clip` instances on startup for instant zero-latency playback.
- If audio hardware is missing (e.g. headless servers, VMs), `try-catch` blocks ensure the game continues running smoothly without crashing.

---

### BGM Streaming: `MusicPlayer.java`
- **Location**: `src/main/java/com/raincatcher/audio/MusicPlayer.java`
- **Purpose**: Streams and loops background music (`audio/over the moon.mp3`) via the pure-Java JLayer library on a background daemon thread.

#### Music Lifecycle:
- **`active = false` (Silent Menu)**: When the game launches, the background thread sleeps (`Thread.sleep(150)`). No music plays in the Main Menu.
- **`play()` (Game Started)**: When the player presses Space/Enter in `GamePanel.startGame()`, `active` becomes `true`. JLayer opens `audio/over the moon.mp3` and streams audio to the speakers.
- **`pause()` (Paused or Game Over)**: Calls `activePlayer.close()`, which immediately stops audio playback so the death sound effects can be heard cleanly.
- **`setMuted(boolean)`**: Toggled with the `M` key.

---

### Graphics & Java2D: `Renderer.java`
- **Location**: `src/main/java/com/raincatcher/ui/Renderer.java`
- **Purpose**: All Java2D vector artwork rendering.

#### Scenery Elements Rendered:
1. **Top Storm Cloud Canopy (`drawTopStormClouds`)**:
   - Dark storm slate base (`Color(32, 42, 58)`), overlapping billow layers animated with sinusoidal sway, silver underside rims, and animated condensation drip dots.
2. **Weathered Shanties with Broken Yero Roofs (`drawFloodedVillage`)**:
   - **House 1**: Slanted wooden shack with rusted corrugated iron roof (*yero*) with vertical corrugation ribs and a curled/bent sheet corner.
   - **House 2**: Two-story shanty with cinderblock base, upper wooden floor, and blue waterproof typhoon tarp held down by old car tires.
   - **House 3**: Shanty with missing iron sheets exposing raw wooden timber rafters.
   - **House 4**: Shack built on wooden stilts sitting in rising water.
   - **Utility Pole**: Leaning wooden electric pole with sagging wires cutting across the sky.
3. **Animated Floodwater (`drawFloodwater`)**:
   - Dual-frequency rolling wave simulation:
     $$Y(x, t) = \text{baseY} + \sin(2.0t + 0.025x) \cdot 3.2 + \cos(1.3t + 0.05x) \cdot 1.8$$
   - Murky teal runoff gradient with foam crest highlights and bobbing driftwood planks.
4. **Heads-Up Display (HUD)**:
   - Rounded glassmorphic pill badges with water drop icon, high score trophy, and dynamic difficulty level progress bar.

---

### Main Loop & Controller: `GamePanel.java`
- **Location**: `src/main/java/com/raincatcher/ui/GamePanel.java`
- **Purpose**: The central controller connecting input, game loop, entities, and renderer.

#### Controlled Swing Timer:
```java
this.gameLoopTimer = new Timer(GameConfig.TIMER_DELAY_MS, e -> gameLoopStep());
this.gameLoopTimer.start();
```
- **Single Master Timer**: Only **one** `Timer` is ever created. When restarting the game, `startGame()` resets variables without creating a new timer. This prevents timer duplication bugs.
- **Delta-Time Clamping**:
  ```java
  long delta = now - lastTickTime;
  if (delta > 100) delta = 100; // Clamped to 100ms
  ```
  Prevents massive physics jumps if the OS stutters or the window is dragged.
- **`Toolkit.getDefaultToolkit().sync()`**: Called at the end of `paintComponent()` to force the native display buffer to sync on Windows.

---

### Window Lifecycle: `GameWindow.java`
- **Location**: `src/main/java/com/raincatcher/ui/GameWindow.java`
- **Purpose**: Encapsulates `JFrame`.
- `setResizable(false)`: Enforces fixed coordinate rendering ($800 \times 650$).
- `setIconImage(createAppIcon())`: Generates a cute raindrop taskbar icon using `BufferedImage` in code.
- `windowClosing`: Properly calls `gamePanel.stopGameTimer()` and `soundManager.stopMusic()`.

---

### Automated Testing: `GameLogicTest.java`
- **Location**: `src/test/java/com/raincatcher/GameLogicTest.java`
- **Purpose**: Pure Java automated unit test suite.
- Contains 9 automated test routines:
  1. `testBasketMovementAndClamping()`: Verifies boundary clamping at left ($X=4$) and right ($X=700$).
  2. `testRaindropCatchAndScoring()`: Verifies raindrop catch awards exactly $+1$ point and creates splash particles.
  3. `testBombCollisionGameOver()`: Verifies bomb contact triggers Game Over and explosion particles.
  4. `testMissedObjectsRemoval()`: Verifies off-screen objects are deleted without memory leaks.
  5. `testDifficultyProgression()`: Verifies speed, spawn rate, and bomb probabilities scale upward over time.
  6. `testSpawnFairnessRules()`: Verifies consecutive bombs never exceed `MAX_CONSECUTIVE_BOMBS`.
  7. `testScoreAndHighScorePersistence()`: Verifies scores persist across re-instantiation.
  8. `testStateTransitionsAndTimer()`: Verifies `MENU \to PLAYING \to PAUSED` states.
  9. `testSoundSynthesisStability()`: Verifies sound generation functions without audio hardware crashes.

---

## 4. Live Defense Debugging Scenarios ("Teacher Messed With Code")

During your presentation, your teacher might intentionally modify lines of code to see if you understand the system. Here is how to diagnose and fix the top scenarios:

### Scenario 1: Teacher changes falling speed and raindrops pass through the basket
- **Symptom**: Raindrops fall right through the basket without scoring.
- **Cause**: The teacher increased `INITIAL_FALLING_SPEED` or `MAX_FALLING_SPEED` in `GameConfig.java` to a huge number (e.g. `50`). The raindrop moves 50 pixels in a single frame, leaping completely over the 18-pixel-tall catch hitbox.
- **How to explain & fix**:
  - Explain: *"This is the classic 'tunneling' problem in discrete collision detection. The speed exceeds the hitbox height."*
  - Fix: In [`GameConfig.java`](file:///a:/Balde%20Catcher/src/main/java/com/raincatcher/config/GameConfig.java), restore `INITIAL_FALLING_SPEED = 2.5` and `MAX_FALLING_SPEED = 10.5`, or increase the catch bounds height in `Basket.getCatchBounds()`.

### Scenario 2: Teacher removes boundary clamping and the basket disappears
- **Symptom**: Moving left or right causes the basket to vanish off the screen.
- **Cause**: The teacher commented out the clamping lines in `Basket.update()`:
  ```java
  // if (x < 4) x = 4;
  // else if (x > screenWidth - width - 4) x = screenWidth - width - 4;
  ```
- **How to explain & fix**:
  - Explain: *"Without clamping, position $X$ decrements indefinitely into negative coordinates or exceeds screen width."*
  - Fix: In [`Basket.java`](file:///a:/Balde%20Catcher/src/main/java/com/raincatcher/model/Basket.java#L87-L91), uncomment the boundary checks.

### Scenario 3: Teacher sets bomb probability to 100% or removes fairness rules
- **Symptom**: Every falling object is a bomb; survival is impossible.
- **Cause**: Teacher set `INITIAL_BOMB_PROBABILITY = 1.0` or disabled `consecutiveBombs >= MAX_CONSECUTIVE_BOMBS` in `SpawnManager.java`.
- **How to explain & fix**:
  - Explain: *"Our fairness algorithm enforces `MAX_CONSECUTIVE_BOMBS = 2` to guarantee safe dodging lanes. When disabled, RNG can create impenetrable bomb walls."*
  - Fix: In [`SpawnManager.java`](file:///a:/Balde%20Catcher/src/main/java/com/raincatcher/logic/SpawnManager.java#L73-L76), re-enable the consecutive bomb check.

### Scenario 4: Teacher deletes `audio/over the moon.mp3` to test resilience
- **Symptom**: Does the game crash with `FileNotFoundException`?
- **Result**: **No! The game continues running smoothly.**
- **How to explain**:
  - Open [`MusicPlayer.java`](file:///a:/Balde%20Catcher/src/main/java/com/raincatcher/audio/MusicPlayer.java#L110-L135).
  - Explain: *"Our `MusicPlayer.openAudioStream()` checks `file.exists() && file.isFile()` and falls back to checking the JAR classpath. If neither is found, it sleeps safely and does not throw an unhandled exception. The game and sound effects continue without crashing."*

### Scenario 5: Teacher modifies code and gets `ConcurrentModificationException`
- **Symptom**: Crash when catching a raindrop.
- **Cause**: Teacher changed `iterator.remove()` in `CollisionManager.java` to `fallingObjects.remove(obj)` inside a `for-each` loop:
  ```java
  // BROKEN:
  for (FallingObject obj : fallingObjects) {
      if (...) fallingObjects.remove(obj);
  }
  ```
- **How to explain & fix**:
  - Explain: *"In Java, modifying a Collection directly while iterating through it with an enhanced for-loop invalidates the collection's modCount, throwing `ConcurrentModificationException`."*
  - Fix: Restore the `Iterator<FallingObject> it = fallingObjects.iterator()` pattern with `it.remove()`.

### Scenario 6: Teacher asks "How do we make the game harder faster?"
- Open [`GameConfig.java`](file:///a:/Balde%20Catcher/src/main/java/com/raincatcher/config/GameConfig.java#L60-L64).
- Explain: *"Reduce `SECONDS_TO_MAX_DIFFICULTY` from `100.0` down to `20.0`, and reduce `SCORE_FOR_MAX_DIFFICULTY` from `80` to `15`. The game will reach peak storm speed and bomb frequency in just 20 seconds."*

---

## 5. Windows PowerShell Commands to Compile, Test & Run

### 1. Ensure Java is on PATH
```powershell
$env:PATH = "$env:LOCALAPPDATA\Programs\openjdk\jdk-25.0.3\bin;$env:PATH"
```

### 2. Compile All Sources with Java 8 Compatibility
```powershell
$sources = (Get-ChildItem -Recurse -Filter "*.java" src\main\java).FullName
javac --release 8 -cp "lib\jlayer-1.0.1.jar" -d bin $sources
```

### 3. Run the Automated Test Suite
```powershell
$testSources = (Get-ChildItem -Recurse -Filter "*.java" src\test\java).FullName
javac --release 8 -cp "bin;lib\jlayer-1.0.1.jar" -d bin $testSources
java -cp bin com.raincatcher.GameLogicTest
```

### 4. Package the Standalone Executable JAR
```powershell
jar --create --file rain-catcher.jar --main-class com.raincatcher.Main -C bin com -C bin javazoom -C bin audio
```

### 5. Launch the Game
```powershell
java -jar rain-catcher.jar
```
*(Or double-click `rain-catcher.jar` in Windows Explorer).*
