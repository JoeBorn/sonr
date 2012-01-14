package com.sonrlabs.test.sonr.signal;

import java.util.List;

import org.acra.ErrorReporter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

import com.sonrlabs.test.sonr.ISampleBuffer;
import com.sonrlabs.test.sonr.SONR;

/**
 * Process an ordered collecton of reusable sample buffers.
 * 
 * This is effectively a singleton as it's only instantiated once by the
 * {@link com.sonrlabs.test.sonr.AudioProcessorQueue} singleton.
 * 
 * There are almost certainly one or more bugs buried in this complex code that
 * generate signals when the user didn't do anything on the remote or the dock.
 * The excessive complexity can easily hide such bugs, so any simplifications
 * are worth doing, even if they don't fix any specific issue. The simpler the
 * code, the easier it is to debug and maintain.
 * 
 * The dock itself might also have firmware issues that cause the same symptom,
 * but it's unlikely to be the only cause.
 */
public final class AudioProcessor
      implements AudioSupportConstants {
   
   private static final String TAG = "AudioProcessor";
   
   private static final short[] FORMATS = {
      AudioFormat.ENCODING_PCM_16BIT
      // AudioRecord.java:467 PCM_8BIT not supported at the moment
   };

   private static final short[] CHANNEL_CONFIGS = {
      AudioFormat.CHANNEL_CONFIGURATION_MONO,
      AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.CHANNEL_CONFIGURATION_DEFAULT,
      AudioFormat.CHANNEL_IN_DEFAULT
   };

   private final TransmissionFinder finder = new TransmissionFinder();
   private final PreambleCheck checker = new PreambleCheck();
   
   /* This funky structure is shared by the checker and the finder. */
   private final int[] sampleStartInidices = new int[SAMPLES_PER_BUFFER];
   
   /**
    * Process the next set of buffers. For now do this one at a time.
    * 
    * In principle we could group up to N together where N is some maximum, by
    * increasing the size of the some arrays by a factor of N and making sloc
    * two-dimensional, with N as the first dimension. This is how the code used
    * to look.
    * 
    * Grouping adds complexity but was (maybe) an advantage in the old model
    * where each run was a new thread. Creating and starting threads is a
    * non-trivial expense, and grouping decreases the number of threads.
    * 
    * With the queue model we have one fixed thread, so the added complexity of
    * groupong would seem to buy us nothing.
    */
   public void nextSamples(List<ISampleBuffer> buffers) {
      for (ISampleBuffer buffer : buffers) {
         try {
            nextSample(buffer);
         } catch (RuntimeException e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
         } finally {
            buffer.release();
         }
      }
   }
   
   private void nextSample(ISampleBuffer buffer) {
      int count = buffer.getCount();
      short[] data = buffer.getArray();
      int numfoundsamples = checker.countSamples(count, data, sampleStartInidices);
      if (numfoundsamples > 0 && finder.findSamples(data, numfoundsamples, sampleStartInidices)) {
         finder.processSample(count, data, sampleStartInidices);
      }
   }
   
   public static AudioRecord findAudioRecord() {
      for (short audioFormat : FORMATS) {
         for (short channelConfig : CHANNEL_CONFIGS) {
            Log.d(TAG, "Attempting rate " + SAMPLE_RATE + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
            try {
               int bufferSize = SAMPLES_PER_BUFFER * AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);
               if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                  // check if we can instantiate and have a success
                  AudioRecord recorder = new AudioRecord(AudioSource.DEFAULT, SAMPLE_RATE, channelConfig, audioFormat, bufferSize);

                  if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                     SONR.bufferSize = bufferSize;
                     return recorder;
                  }
               }
            } catch (RuntimeException e) {
               Log.v(TAG, "Unable to allocate for: " + SAMPLE_RATE + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
               Log.e(TAG, "Exception " + e);
            }
         }
      }
      return null;
   }

}


