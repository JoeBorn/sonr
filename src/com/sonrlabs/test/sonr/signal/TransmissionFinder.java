/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;


import com.sonrlabs.test.sonr.ISampleBuffer;

import android.util.Log;

final class TransmissionFinder
      extends SignalConstructor {
   
   private int samplelocsize;
   
   void nextSample(ISampleBuffer buffer, int sampleCount, int[] sampleStartIndices) {
      short[] samples = buffer.getArray();
      samplelocsize = 0;
      autoGainControl(samples, sampleStartIndices);
      int startpos = sampleStartIndices[0];
      for (int n = 0; n < sampleCount; n++) {
         samplelocsize = findSample(startpos, samples, samplelocsize, sampleStartIndices);
      }
      if (samplelocsize > 0) {
         processSample(samples, sampleStartIndices);
      }
   }

   @Override
   protected boolean checkSignal(int signal) {
      return true;
   }

   private void processSample(short[] samples, int[] sampleStartIndices) {
      if (samplelocsize < 2) {
         // don't bother
         return;
      }
      constructSignal(samples, sampleStartIndices);
      try {
         processSignal();
      } catch (SpuriousSignalException e) {
         String message = generateLog(samplelocsize, samples, sampleStartIndices, e.getSignalCode());
         Log.d("Spurious Signal", message);
      }
   }

   /*
    * Look for two out of three match.
    * 
    * Works by side-effect which is why the bodies of the if statements are empty.
    * Note the we're depending on SAMPLES_PER_BUFFER being 3 here.
    */
   private void processSignal()
         throws SpuriousSignalException {
      if (processSignalIfMatch(0, 1)) {
      } else if (samplelocsize > 2 && processSignalIfMatch(0, 2)) {
      } else if (samplelocsize > 2 && processSignalIfMatch(1, 2)) {
      }
   }
   

   private void autoGainControl(short[] samples, int[] sampleStartIndices) {
      computeSignalMax(samples, sampleStartIndices[0]);
   }
}