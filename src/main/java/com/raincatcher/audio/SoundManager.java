package com.raincatcher.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.io.ByteArrayInputStream;
import java.util.Random;

/**
 * Audio manager implementing purely procedural sound generation using standard
 * Java javax.sound.sampled APIs. Requires zero external audio files and handles
 * environments without audio hardware gracefully.
 */
public class SoundManager {

    private static final int SAMPLE_RATE = 44100;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private boolean muted = false;
    private boolean soundAvailable = true;

    private Clip clipCatch;
    private Clip clipExplosion;
    private Clip clipGameOver;
    private Clip clipNewRecord;
    private Clip clipClick;

    private final MusicPlayer musicPlayer;

    public SoundManager() {
        initializeSynthesizer();
        this.musicPlayer = new MusicPlayer("audio/over the moon.mp3", "/audio/over the moon.mp3");
    }

    /**
     * Synthesizes audio clips into memory on startup.
     */
    private void initializeSynthesizer() {
        try {
            clipCatch = createClip(generateWaterBloopSound());
            clipExplosion = createClip(generateExplosionSound());
            clipGameOver = createClip(generateGameOverSound());
            clipNewRecord = createClip(generateNewRecordSound());
            clipClick = createClip(generateClickSound());
        } catch (Throwable t) {
            // Audio line unavailable (e.g. headless environment or no soundcard)
            soundAvailable = false;
        }
    }

    private Clip createClip(byte[] pcmData) throws Exception {
        Clip clip = AudioSystem.getClip();
        clip.open(FORMAT, pcmData, 0, pcmData.length);
        return clip;
    }

    private void playClip(Clip clip) {
        if (muted || !soundAvailable || clip == null) {
            return;
        }
        try {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        } catch (Throwable ignored) {
            // Safe fallback
        }
    }

    public void playCatch() {
        playClip(clipCatch);
    }

    public void playExplosion() {
        playClip(clipExplosion);
    }

    public void playGameOver() {
        playClip(clipGameOver);
    }

    public void playNewRecord() {
        playClip(clipNewRecord);
    }

    public void playClick() {
        playClip(clipClick);
    }

    public void toggleMute() {
        this.muted = !this.muted;
        if (musicPlayer != null) {
            musicPlayer.setMuted(this.muted);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (musicPlayer != null) {
            musicPlayer.setMuted(muted);
        }
    }

    public void playMusic() {
        if (musicPlayer != null) {
            musicPlayer.play();
        }
    }

    public void pauseMusic() {
        if (musicPlayer != null) {
            musicPlayer.pause();
        }
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            musicPlayer.stop();
        }
    }

    public boolean isSoundAvailable() {
        return soundAvailable;
    }

    // -------------------------------------------------------------
    // Procedural Audio Synthesizers (16-bit Mono PCM, 44.1kHz)
    // -------------------------------------------------------------

    /**
     * Synthesizes a sweet, bubbly water drop chime.
     */
    private byte[] generateWaterBloopSound() {
        int durationMs = 140;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        double startFreq = 580.0;
        double endFreq = 960.0;
        double phase = 0;

        for (int i = 0; i < numSamples; i++) {
            double progress = (double) i / numSamples;
            double freq = startFreq + (endFreq - startFreq) * Math.pow(progress, 0.7);
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;

            // Exponential decay envelope
            double envelope = Math.exp(-progress * 5.0) * Math.sin(progress * Math.PI);
            // Harmonics
            double sampleVal = Math.sin(phase) * 0.75 + Math.sin(phase * 2.0) * 0.25;
            short pcm = (short) (sampleVal * envelope * 28000);

            // Little-endian 16-bit
            buffer[i * 2] = (byte) (pcm & 0xFF);
            buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Synthesizes a crunchy, heavy explosive boom.
     */
    private byte[] generateExplosionSound() {
        int durationMs = 500;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];
        Random rand = new Random(42);

        double subPhase = 0;
        double lastNoise = 0;

        for (int i = 0; i < numSamples; i++) {
            double progress = (double) i / numSamples;
            double envelope = Math.exp(-progress * 6.5);

            // Low sub-bass drop (110 Hz down to 35 Hz)
            double subFreq = 110.0 - progress * 75.0;
            subPhase += 2.0 * Math.PI * subFreq / SAMPLE_RATE;
            double subWave = Math.sin(subPhase);

            // Filtered white noise
            double rawNoise = (rand.nextDouble() * 2.0 - 1.0);
            double filteredNoise = lastNoise * 0.6 + rawNoise * 0.4;
            lastNoise = filteredNoise;

            double sampleVal = (subWave * 0.60 + filteredNoise * 0.40) * envelope;
            short pcm = (short) (Math.max(-1.0, Math.min(1.0, sampleVal)) * 30000);

            buffer[i * 2] = (byte) (pcm & 0xFF);
            buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Synthesizes a melancholic descending game over jingle.
     */
    private byte[] generateGameOverSound() {
        int durationMs = 600;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        double[] freqs = {392.0, 349.2, 311.1, 261.6}; // G4, F4, Eb4, C4
        int noteLength = numSamples / freqs.length;

        for (int i = 0; i < numSamples; i++) {
            int noteIndex = Math.min(freqs.length - 1, i / noteLength);
            int noteSample = i % noteLength;
            double noteProgress = (double) noteSample / noteLength;

            double freq = freqs[noteIndex];
            double phase = 2.0 * Math.PI * freq * i / SAMPLE_RATE;
            double env = Math.exp(-noteProgress * 4.0) * (1.0 - noteProgress * 0.2);

            double sampleVal = (Math.sin(phase) + 0.3 * Math.sin(phase * 2.0)) * env * 0.7;
            short pcm = (short) (sampleVal * 26000);

            buffer[i * 2] = (byte) (pcm & 0xFF);
            buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Synthesizes a bright, celebratory new record fanfare.
     */
    private byte[] generateNewRecordSound() {
        int durationMs = 550;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        double[] freqs = {523.25, 659.25, 783.99, 1046.50}; // C5, E5, G5, C6
        int noteLength = numSamples / freqs.length;

        for (int i = 0; i < numSamples; i++) {
            int noteIndex = Math.min(freqs.length - 1, i / noteLength);
            int noteSample = i % noteLength;
            double noteProgress = (double) noteSample / noteLength;

            double freq = freqs[noteIndex];
            double phase = 2.0 * Math.PI * freq * i / SAMPLE_RATE;
            double env = Math.exp(-noteProgress * 3.5);

            double sampleVal = Math.sin(phase) * env * 0.8;
            short pcm = (short) (sampleVal * 26000);

            buffer[i * 2] = (byte) (pcm & 0xFF);
            buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Synthesizes a subtle button click / menu tap.
     */
    private byte[] generateClickSound() {
        int durationMs = 35;
        int numSamples = (SAMPLE_RATE * durationMs) / 1000;
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double progress = (double) i / numSamples;
            double phase = 2.0 * Math.PI * 880.0 * i / SAMPLE_RATE;
            double env = Math.exp(-progress * 15.0);
            short pcm = (short) (Math.sin(phase) * env * 22000);

            buffer[i * 2] = (byte) (pcm & 0xFF);
            buffer[i * 2 + 1] = (byte) ((pcm >> 8) & 0xFF);
        }
        return buffer;
    }
}

