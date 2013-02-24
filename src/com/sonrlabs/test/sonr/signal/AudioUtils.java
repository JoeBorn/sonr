/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 * Questions/Comments: joe@sonrlabs.com
 * 
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
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
      //AudioFormat.ENCODING_PCM_8BIT
      /* Any others ? */
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
      
      for (short audioFormat : FORMATS) {
         for (short channelConfig : CHANNEL_CONFIGS) {
            String baseMessage = "rate" + SAMPLE_RATE + "Hz, bits: " + audioFormat + ", channel: " + channelConfig;
            String message = callingClassName + " Attempting " + baseMessage;
            SonrLog.d(TAG, message);
            int bsize = TRANSMISSIONS_PER_BUFFER * AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);
            if (bsize != AudioRecord.ERROR_BAD_VALUE) {
               AudioRecord audioRecorder;
               try {
                  audioRecorder =  new AudioRecord(AudioSource.DEFAULT, SAMPLE_RATE, channelConfig, audioFormat, bsize);
               } catch (IllegalArgumentException e) {
                  String errmsg = callingClassName + " unable to allocate for: " + baseMessage  + ": " + e;
                  SonrLog.e(TAG, errmsg);
                  continue;
               }
               /* successfully made the recorder, now validate the state */
               switch (audioRecorder.getState()) {
                  case AudioRecord.STATE_INITIALIZED:
                     bufferSize = bsize;
                     return audioRecorder;
                     
                  default:
                     audioRecorder.release();
                     break;
               }
            }
         }
      }
      /* If we get here we did not find a working recorder. */
      SonrLog.e(TAG, callingClassName + " returning null AudioRecord...");
      return null;
   }

}
