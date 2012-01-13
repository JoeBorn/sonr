/***************************************************************************
 *
 * Copyright 2011 by SONR
 **************************************************************************/

package com.sonrlabs.test.sonr;

import android.util.Log;

/**
 * Messy signal processing used by {@link MicSerialListener} to find
 * and sync up with the dock.
 * 
 */
final class ListenerAudioSupport
      extends SignalConstruction {
   
   private static final String TAG = ListenerAudioSupport.class.getSimpleName();
   
   /**
    * This is the entry point for {@link MicSerialListener}.
    */
   boolean autoGainControl(short[] samples, int count) {

      boolean found = false;
      int startpos = SAMPLE_LENGTH;
      int sampleloc[] = new int[SAMPLES_PER_BUFFER];
      while (startpos < count - 1 && Math.abs(samples[startpos] - samples[startpos + 1]) < THRESHOLD) {
         startpos++;
      }

      if (startpos < count - 1 && startpos >= SAMPLE_LENGTH && startpos < SAMPLE_LENGTH * (SAMPLES_PER_BUFFER-1)) {
         startpos -= SAMPLE_LENGTH;
         while (Math.abs(samples[startpos] - samples[startpos + 1]) < THRESHOLD) {
            // && startpos < numSamples-1)
            startpos++;
         }
      }

      startpos += BEGIN_OFFSET;

      if (startpos < count - (SAMPLE_LENGTH - BEGIN_OFFSET)) {
         Log.d(TAG, "Found a sample...");
         computeSignalMax(samples, startpos);
         findSample(startpos, samples, 0, sampleloc);

         for (int n = 0; n < SAMPLES_PER_BUFFER; n++) {
            if (sampleloc[n] != 0) {
               int index = 0;
               movingsum[0] = 0;
               for (int i = 0; i < MOVING_SIZE; i++) {
                  movingbuf[i] = samples[i + sampleloc[n]];
                  movingsum[0] += samples[i + sampleloc[n]];
               }
               for (int i = MOVING_SIZE; i < TRANSMISSION_LENGTH; i++) {
                  movingsum[i] = movingsum[i - 1] - movingbuf[index];
                  movingsum[i] += samples[i + sampleloc[n]];
                  movingbuf[index] = samples[i + sampleloc[n]];
                  index++;
                  if (index == MOVING_SIZE) {
                     index = 0;
                  }
               }

               constructSignal(n);
            }
         }

         /* If at least two are BOUND, that's a match. */
         int matchCount = 0;
         for (int value : signals) {
            if (value == BOUNDARY) {
               ++matchCount;
            }
         }
         found = matchCount >= 2;
      }

      return found;
   }
}
