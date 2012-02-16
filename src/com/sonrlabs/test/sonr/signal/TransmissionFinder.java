/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;


import com.sonrlabs.test.sonr.ISampleBuffer;
import com.sonrlabs.test.sonr.SonrLog;

final class TransmissionFinder
      extends SignalConstructor {
   
   @Override
   String debugTag() {
      return "TransmissionFinder";
   }

   void nextSample(ISampleBuffer buffer, int sampleCount, int[] sampleStartIndices) {
      short[] samples = buffer.getArray();
      int startpos = sampleStartIndices[0];
      computeSignalMax(samples, startpos);
      int samplelocsize = 0;
      for (int n = 0; n < sampleCount; n++) {
         samplelocsize = findSample(startpos, samples, samplelocsize, sampleStartIndices);
      }
      if (samplelocsize >= 2) {
         processSample(samples, sampleStartIndices);
      }
   }

   private void processSample(short[] samples, int[] sampleStartIndices) {
      constructSignal(samples, sampleStartIndices);
      try {
         processSignalIfMatch();
      } catch (SpuriousSignalException e) {
         SonrLog.d(debugTag(), "Spurious Signal");
      }
   }
}