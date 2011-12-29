/***************************************************************************
 * Copyright 2011 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import android.util.Log;

/**
 * Sampling support.
 * 
 * Two sets of methods for now, one used by {@link MicSerialListener} the other
 * used by {@link AudioProcessor}. They should share code at some point.
 * 
 */
final class ListenerAudioSupport
      implements AudioSupportConstants {
   
   static final ListenerAudioSupport singleton = new ListenerAudioSupport();
   
   private static final String TAG = ListenerAudioSupport.class.getSimpleName();
   
   
   private int SIGNAL_MAX_SUM = 0;
   private final int[] movingsum = new int[TRANSMISSION_LENGTH];
   private final int[] movingbuf = new int[9];

   
   private ListenerAudioSupport() {
   }
   
   /**
    * This is the entry point for {@link MicSerialListener}.
    */
   boolean autoGainControl(short[] samples, int numSamples) {

      boolean found = false;
      int startpos = SAMPLE_LENGTH;
      int arraypos = 0;
      int sampleloc[] = new int[3];
      while (startpos < numSamples - 1 && Math.abs(samples[startpos] - samples[startpos + 1]) < THRESHOLD) {
         startpos++;
      }

      if (startpos < numSamples - 1 && startpos >= SAMPLE_LENGTH && startpos < SAMPLE_LENGTH * 2) {
         startpos -= SAMPLE_LENGTH;
         while (Math.abs(samples[startpos] - samples[startpos + 1]) < THRESHOLD) {
            // && startpos < numSamples-1)
            startpos++;
         }
      }

      startpos += BEGIN_OFFSET;

      if (startpos < numSamples - (SAMPLE_LENGTH - BEGIN_OFFSET)) {
         Log.d(TAG, "Found a sample...");

         movingsum[0] = 0;
         for (int i = startpos; i < startpos + 9; i++) {
            movingbuf[i - startpos] = samples[i];
            movingsum[0] += samples[i];
         }
         SIGNAL_MAX_SUM = 0;
         for (int i = startpos + 9; i < startpos + PREAMBLE - BEGIN_OFFSET + 3 * (TRANSMISSION_LENGTH + BIT_OFFSET); i++) {
            movingsum[1] = movingsum[0] - movingbuf[arraypos];
            movingsum[1] += samples[i];
            movingbuf[arraypos] = samples[i];
            arraypos++;
            if (arraypos == 9) {
               arraypos = 0;
            }

            int temp = Math.abs(movingsum[0] - movingsum[1]);
            if (temp > SIGNAL_MAX_SUM) {
               SIGNAL_MAX_SUM = temp;
            }

            // test_buf[i - startpos - 9] = sample_buf1[i];
            // movingsum2[i - startpos - 9] = movingsum[0];
            movingsum[0] = movingsum[1];
         }

         SIGNAL_MAX_SUM /= 1.375;
         findSample(startpos, samples, sampleloc);

         int[] triple = new int[3];
         for (int n = 0; n < 3; n++) {
            if (sampleloc[n] != 0) {
               arraypos = 0;
               movingsum[0] = 0;
               for (int i = 0; i < 9; i++) {
                  movingbuf[i] = samples[i + sampleloc[n]];
                  movingsum[0] += samples[i + sampleloc[n]];
               }
               for (int i = 9; i < TRANSMISSION_LENGTH; i++) {
                  movingsum[i] = movingsum[i - 1] - movingbuf[arraypos];
                  movingsum[i] += samples[i + sampleloc[n]];
                  movingbuf[arraypos] = samples[i + sampleloc[n]];
                  arraypos++;
                  if (arraypos == 9) {
                     arraypos = 0;
                  }
               }

               boolean isinphase = true, switchphase = true;
               /* we start out with a phase shift */
               int bitnum = 0;

               for (int i = FRAMES_PER_BIT + 1; i < TRANSMISSION_LENGTH; i++) {
                  if (isPhase(movingsum[i - 1], movingsum[i], SIGNAL_MAX_SUM) && switchphase) {
                     isinphase = !isinphase;
                     switchphase = false; // already switched
                  }

                  if (i % FRAMES_PER_BIT == 0) {
                     if (!isinphase) {
                        triple[n] |= 0x1 << bitnum;
                     }
                     bitnum++;
                     /* reached a bit, can now switch again if phase shifts */
                     switchphase = true;
                  }
               }

               Log.d(TAG, "TRANSMISSION[" + n + "]: " + "0x" + Integer.toHexString(triple[n]));
               // LogFile.MakeLog("TRANSMISSION[" + n + "]: " + "0x"+
               // Integer.toHexString(byteInDec[n]));
            }
         }

         /* If at least two are 0x27, that's a match. */
         int matchCount = 0;
         for (int value : triple) {
            if (value == 0x27) {
               ++matchCount;
            }
         }
         found = matchCount >= 2;
      }

      return found;
   }
   
   private void findSample(int startpos, short[] samples, int[] sampleloc) {
      int arraypos = 0;
      int numsampleloc = 0;
      movingsum[0] = 0;
      for (int i = startpos; i < startpos + 9; i++) {
         movingbuf[i - startpos] = samples[i];
         movingsum[0] += samples[i];
      }

      for (int i = startpos + 9; i < startpos + SAMPLE_LENGTH - BIT_OFFSET; i++) {
         movingsum[1] = movingsum[0] - movingbuf[arraypos];
         movingsum[1] += samples[i];
         movingbuf[arraypos] = samples[i];
         arraypos++;
         if (arraypos == 9) {
            arraypos = 0;
         }

         if (isPhase(movingsum[0], movingsum[1], SIGNAL_MAX_SUM)) {
            sampleloc[numsampleloc++] = i - 5;
            // next transmission
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
            sampleloc[numsampleloc++] = i;
            // next transmission
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
            sampleloc[numsampleloc] = i;
            return;
         }

         movingsum[0] = movingsum[1];
      }
   }
   
   private boolean isPhase(int sum1, int sum2, int max) {
      return Math.abs(sum1 - sum2) > max;
   }

}
