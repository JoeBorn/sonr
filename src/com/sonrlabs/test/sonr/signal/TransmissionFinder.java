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
         processSignalIfMatch();
      } catch (SpuriousSignalException e) {
         Log.d("TransmissionFinder", "Spurious Signal");
      }
   }

   private void autoGainControl(short[] samples, int[] sampleStartIndices) {
      computeSignalMax(samples, sampleStartIndices[0]);
   }
}