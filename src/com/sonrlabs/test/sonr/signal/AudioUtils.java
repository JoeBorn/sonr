/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;

import com.sonrlabs.test.sonr.SonrLog;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

/**
 *  Static utility methods.
 */
public class AudioUtils implements AudioSupportConstants {
   
   private static final String TAG = "AudioUtils";
   
   private static final short[] FORMATS = {
      AudioFormat.ENCODING_PCM_16BIT,
      //AudioFormat.ENCODING_DEFAULT,
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
   public static AudioRecord findAudioRecord(String callingClassName) {

      AudioRecord audioRecorder = null;
      
      for (short audioFormat : FORMATS) {
         for (short channelConfig : CHANNEL_CONFIGS) {
            SonrLog.d(TAG, callingClassName + " Attempting rate " + SAMPLE_RATE + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
            
            try {
                  int bsize = SAMPLES_PER_BUFFER * AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);
                  
                  if (bsize != AudioRecord.ERROR_BAD_VALUE) {
                     // check if we can instantiate and have a success
                     audioRecorder = new AudioRecord(AudioSource.DEFAULT, SAMPLE_RATE, channelConfig, audioFormat, bsize);
                     
                     switch (audioRecorder.getState()) {
                        case AudioRecord.STATE_INITIALIZED:
                           bufferSize = bsize;
                           return audioRecorder;
                           //break;
                        case AudioRecord.STATE_UNINITIALIZED:
                        default:
                           audioRecorder.release();
                           audioRecorder = null;
                           break;
                     }
                  }
               
            } catch (Exception e) {
               SonrLog.e(TAG, callingClassName + " unable to allocate for: " + SAMPLE_RATE + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
               SonrLog.e(TAG, "Exception " + e);
            }
         }
      }
      if (audioRecorder == null) {
         SonrLog.e(TAG, callingClassName + " returning null AudioRecord...");
      }
      
      return audioRecorder;
   }

}
