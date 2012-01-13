/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import android.util.Log;

final class TransmissionFinder
      extends SignalConstruction {
   private final int[][] trans_buf = new int[SAMPLES_PER_BUFFER][TRANSMISSION_LENGTH + BIT_OFFSET];
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

   void processSample(int count, short[] samples, int[] sampleStartIndices) {
      if (samplelocsize < 2) {
         // don't bother
         return;
      }
      /* copy transmission down because the buffer could get overwritten */
      for (int j = 0; j < samplelocsize; j++) {
         for (int i = 0; i < TRANSMISSION_LENGTH; i++) {
            int sampleStartIndex = sampleStartIndices[j % SAMPLES_PER_BUFFER] + i;
            trans_buf[j][i] = samples[sampleStartIndex];
         }
      }
      for (int signalIndex = 0; signalIndex < samplelocsize; signalIndex++) {
         int arraypos = 0;
         movingsum[0] = 0;
         for (int i = 0; i < MOVING_SIZE; i++) {
            movingbuf[i] = trans_buf[signalIndex][i];
            movingsum[0] += trans_buf[signalIndex][i];
         }

         for (int i = MOVING_SIZE; i < TRANSMISSION_LENGTH; i++) {
            movingsum[i] = movingsum[i - 1] - movingbuf[arraypos];
            movingsum[i] += trans_buf[signalIndex][i];
            movingbuf[arraypos] = trans_buf[signalIndex][i];
            arraypos++;
            if (arraypos == MOVING_SIZE) {
               arraypos = 0;
            }
         }
         constructSignal(signalIndex);
      }
      try {
         processSignal();
      } catch (SpuriousSignalException e) {
         String message = e.generateLog(samplelocsize, samples, sampleStartIndices, signals);
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
      int code0 = signals[0];
      int code1 = signals[1];
      int code2 = signals[2];
      if (testMatch(code0, code1)) {
         AudioProcessorQueue.singleton.processAction(code0);
      } else if (samplelocsize > 2 && testMatch(code0, code2)) {
         AudioProcessorQueue.singleton.processAction(code0);
      } else if (samplelocsize > 2 && testMatch(code1, code2)) {
         AudioProcessorQueue.singleton.processAction(code1);
      }
   }
   
   private boolean testMatch(int first, int second) {
      return first != 0 & first != BOUNDARY && first == second;
   }

   private void autoGainControl(short[] samples, int[] sampleStartIndices) {
      computeSignalMax(samples, sampleStartIndices[0]);
   }
}