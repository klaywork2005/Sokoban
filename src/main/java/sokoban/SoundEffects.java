package sokoban;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

/**
 * Short sound effects for the game.
 *
 * <p>
 * The sounds are made up in code from sine waves, so the game needs no sound
 * files for
 * them. A sound is just a long list of numbers: each number says how far the
 * speaker should be
 * pushed out at one tiny moment in time, and 44,100 of them are played every
 * second.
 */
final class SoundEffects {
  private static final float SAMPLES_PER_SECOND = 44_100f;
  // A 16-bit sample goes from -32768 to 32767. Staying well below the top keeps
  // the sound
  // comfortable and leaves room for the background music underneath it.
  private static final double LOUDNESS = 0.35;

  // The level-complete jingle is a rising chord: C, E, G, then C an octave up (in
  // hertz).
  private static final double[] NOTES = { 523.25, 659.25, 783.99, 1046.50 };
  private static final double SECONDS_BETWEEN_NOTES = 0.12;
  private static final double SECONDS_A_NOTE_RINGS = 0.7;
  // Each note fades in quickly (so it starts without a click) and then dies away
  // slowly.
  private static final double FADE_IN_SECONDS = 0.01;
  private static final double DIE_AWAY_SPEED = 5.0;
  // The whole sound fades out at the very end, so it stops without a click too.
  private static final double END_FADE_SECONDS = 0.05;

  // Mono, 16 bits per sample, signed numbers, least significant byte first.
  private static final AudioFormat FORMAT = new AudioFormat(SAMPLES_PER_SECOND, 16, 1, true, false);

  // The jingle, worked out once and kept ready to play.
  private final byte[] levelCompleteSound = toBytes(createLevelCompleteSamples());

  /**
   * Play the jingle for finishing a level. It never blocks, and it copes with no
   * sound card.
   */
  void playLevelComplete() {
    try {
      Clip clip = AudioSystem.getClip();
      clip.open(FORMAT, levelCompleteSound, 0, levelCompleteSound.length);
      // When the sound has finished playing, give the clip's resources back. The code
      // in
      // event -> { ... } is a small task that the audio system runs when something
      // happens.
      clip.addLineListener(event -> {
        if (event.getType() == LineEvent.Type.STOP) {
          clip.close();
        }
      });
      clip.start();
    } catch (LineUnavailableException | IllegalArgumentException problem) {
      // A missing sound card must not stop the game, so just say so and carry on.
      System.err.println("Could not play sound effect: " + problem.getMessage());
    }
  }

  /**
   * Work out the samples of the level-complete jingle. Each note is a sine wave
   * plus a
   * quieter one an octave higher, which sounds a little like a bell. The notes
   * start one
   * after another but keep ringing, so they blend into a chord.
   */
  static short[] createLevelCompleteSamples() {
    double totalSeconds = (NOTES.length - 1) * SECONDS_BETWEEN_NOTES + SECONDS_A_NOTE_RINGS;
    int sampleCount = (int) (totalSeconds * SAMPLES_PER_SECOND);
    double[] mix = new double[sampleCount];

    for (int note = 0; note < NOTES.length; note++) {
      int firstSample = (int) (note * SECONDS_BETWEEN_NOTES * SAMPLES_PER_SECOND);
      int noteLength = (int) (SECONDS_A_NOTE_RINGS * SAMPLES_PER_SECOND);
      for (int number = 0; number < noteLength && firstSample + number < sampleCount; number++) {
        double seconds = number / SAMPLES_PER_SECOND;
        double wave = Math.sin(2 * Math.PI * NOTES[note] * seconds)
            + 0.3 * Math.sin(2 * Math.PI * 2 * NOTES[note] * seconds);
        double fadeIn = Math.min(1.0, seconds / FADE_IN_SECONDS);
        double dieAway = Math.exp(-DIE_AWAY_SPEED * seconds);
        mix[firstSample + number] += wave * fadeIn * dieAway;
      }
    }
    return scaleAndFadeOut(mix);
  }

  /**
   * Turn the mixed sound into 16-bit numbers, with the loudest point at
   * {@link #LOUDNESS}.
   */
  private static short[] scaleAndFadeOut(double[] mix) {
    double loudest = 0;
    for (double value : mix) {
      loudest = Math.max(loudest, Math.abs(value));
    }

    int endFadeSamples = (int) (END_FADE_SECONDS * SAMPLES_PER_SECOND);
    short[] samples = new short[mix.length];
    for (int number = 0; number < mix.length; number++) {
      int samplesLeft = mix.length - number;
      double endFade = Math.min(1.0, (double) samplesLeft / endFadeSamples);
      double value = mix[number] / loudest * LOUDNESS * endFade;
      samples[number] = (short) Math.round(value * Short.MAX_VALUE);
    }
    return samples;
  }

  /** Write each 16-bit sample as two bytes, the less significant byte first. */
  private static byte[] toBytes(short[] samples) {
    byte[] bytes = new byte[samples.length * 2];
    for (int number = 0; number < samples.length; number++) {
      bytes[number * 2] = (byte) samples[number];
      bytes[number * 2 + 1] = (byte) (samples[number] >> 8);
    }
    return bytes;
  }
}
