/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;


import android.util.Log;

final class TransmissionFinder
      extends SignalConstructor {
   
   private int samplelocsize;
   
   boolean findSamples(short[] samples, int sampleCount, int[] sampleStartIndices) {
      samplelocsize = 0;
      autoGainControl(samples, sampleStartIndices);
      int startpos = sampleStartIndices[0];
      for (int n = 0; n < sampleCount; n++) {
         samplelocsize = findSample(startpos, samples, samplelocsize, sampleStartIndices);
      }
      return samplelocsize > 0;
   }

   @Override
   protected boolean checkSignal(int signal) {
      return true;
   }

   void processSample(int count, short[] samples, int[] sampleStartIndices) {
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

   private void processSignal()
         throws SpuriousSignalException {
      /*
       * receive byte using best two out of three.
       * 
       * Note the we're depending on SAMPLES_PER_BUFFER being 3 here.
       */
      if (processSignalIfMatch(0, 1)) {
      } else if (samplelocsize > 2 && processSignalIfMatch(0, 2)) {
      } else if (samplelocsize > 2 && processSignalIfMatch(1, 2)) {
      }
   }
   

   private void autoGainControl(short[] samples, int[] sampleStartIndices) {
      computeSignalMax(samples, sampleStartIndices[0]);
   }
}