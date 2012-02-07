/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

/**
 *  Static utility methods.
 */
public class AudioUtils
      implements AudioSupportConstants {
   
   private static final String TAG = "AudioUtils";
   
   
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

   private static int bufferSize;
   
   public static int getAudioBufferSize() {
      return bufferSize;
   }
   
   /**
    * Utility method to find the right audio format for a given phone.
    * @return a suitable record, or null if none found.
    */
   public static AudioRecord findAudioRecord() {
      for (short audioFormat : FORMATS) {
         for (short channelConfig : CHANNEL_CONFIGS) {
            Log.d(TAG, "Attempting rate " + SAMPLE_RATE + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
            try {
               int bsize = SAMPLES_PER_BUFFER * AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);
               if (bsize != AudioRecord.ERROR_BAD_VALUE) {
                  // check if we can instantiate and have a success
                  AudioRecord recorder = new AudioRecord(AudioSource.DEFAULT, SAMPLE_RATE, channelConfig, audioFormat, bsize);

                  if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                     bufferSize = bsize;
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
