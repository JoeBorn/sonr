/***************************************************************************
 *
 * Copyright 2011 by SONR
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;


import android.util.Log;

/**
 * Sync with the dock.
 * 
 */
public class DockDetector
      extends SignalConstructor {
   
   private static final String TAG = DockDetector.class.getSimpleName();
   
   /**
    * This is the entry point for {@link com.sonrlabs.test.sonr.MicSerialListener}.
    * <p>
    * TODO: See if we can merge this method and {@link PreambleCheck#findPSKBegin}.
    */
   public boolean findDock(short[] samples, int count) {

      int startpos = SAMPLE_LENGTH;
      int sampleStartIndices[] = new int[SAMPLES_PER_BUFFER];
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
         findSample(startpos, samples, 0, sampleStartIndices);

         constructSignal(samples, sampleStartIndices);

         /* If at least two are BOUND, that's a match. */
         return countBoundSignals() >= 2;
      }

      return false;
   }

   @Override
   protected boolean checkSignal(int signal) {
      return signal != 0;
   }
}
