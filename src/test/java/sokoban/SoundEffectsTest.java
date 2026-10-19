package sokoban;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Check the made-up jingle without needing any speakers. */
class SoundEffectsTest {
    @Test
    void jingleLastsAboutOneSecond() {
        short[] samples = SoundEffects.createLevelCompleteSamples();

        double seconds = samples.length / 44_100.0;
        assertTrue(seconds > 0.8 && seconds < 1.5, "The jingle lasts " + seconds + " seconds.");
    }

    @Test
    void jingleIsNeitherSilentNorTooLoud() {
        short[] samples = SoundEffects.createLevelCompleteSamples();

        int loudest = 0;
        for (short sample : samples) {
            loudest = Math.max(loudest, Math.abs(sample));
        }
        // The top of a 16-bit sample is 32767. The jingle should clearly be heard, but stay
        // well below the top so that it does not drown out the music or hurt anyone's ears.
        assertTrue(loudest > 0.2 * Short.MAX_VALUE, "Too quiet: " + loudest);
        assertTrue(loudest < 0.5 * Short.MAX_VALUE, "Too loud: " + loudest);
    }

    @Test
    void jingleStartsAndEndsInSilenceSoThereAreNoClicks() {
        short[] samples = SoundEffects.createLevelCompleteSamples();

        assertEquals(0, samples[0]);
        assertTrue(Math.abs(samples[samples.length - 1]) < 50);
    }

    @Test
    void jingleChangesOverTimeInsteadOfBeingOneFlatTone() {
        short[] samples = SoundEffects.createLevelCompleteSamples();

        // Compare the loudness of the first 0.1 seconds with the last 0.1 seconds.
        int window = 4_410;
        int start = 0;
        int end = 0;
        for (int number = 0; number < window; number++) {
            start = Math.max(start, Math.abs(samples[number]));
            end = Math.max(end, Math.abs(samples[samples.length - 1 - number]));
        }
        assertTrue(start > 3 * end, "The jingle should fade away.");
    }

    @Test
    void playingNeverThrowsEvenWithoutASoundCard() {
        // On a machine with no audio device this prints a message instead of failing.
        assertDoesNotThrow(() -> new SoundEffects().playLevelComplete());
    }
}
