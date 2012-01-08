/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import android.util.Log;

final class TransmissionFinder
      implements AudioSupportConstants {
   private final int[] signals = new int[SAMPLES_PER_BUFFER];
   private final int[] movingbuf = new int[MOVING_SIZE];
   private final int[][] trans_buf = new int[SAMPLES_PER_BUFFER][TRANSMISSION_LENGTH + BIT_OFFSET];
   private final int[] movingsum = new int[TRANSMISSION_LENGTH];
   private int samplelocsize;
   private int signalMaxSum = 0;
   private boolean needsAgc = true;

   void newConnection() {
      needsAgc = true;
   }

   boolean findSamples(short[] samples, int sampleCount, int[] sampleStartIndices) {
      samplelocsize = 0;
      if (needsAgc) {
         autoGainControl(samples, sampleStartIndices);
         needsAgc = false;
      }
      for (int n = 0, numsampleloc = 0; n < sampleCount; n++) {
         numsampleloc = findPSKTransmissions(samples, numsampleloc, sampleStartIndices);
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
      for (int s = 0; s < samplelocsize; s++) {
         int arraypos = 0;
         movingsum[0] = 0;
         for (int i = 0; i < MOVING_SIZE; i++) {
            movingbuf[i] = trans_buf[s][i];
            movingsum[0] += trans_buf[s][i];
         }

         for (int i = MOVING_SIZE; i < TRANSMISSION_LENGTH; i++) {
            movingsum[i] = movingsum[i - 1] - movingbuf[arraypos];
            movingsum[i] += trans_buf[s][i];
            movingbuf[arraypos] = trans_buf[s][i];
            arraypos++;
            if (arraypos == MOVING_SIZE) {
               arraypos = 0;
            }
         }

         /* we start out with a phase shift */
         boolean isinphase = true, switchphase = true;
         int bitnum = 0;
         signals[s] = 0;

         for (int i = FRAMES_PER_BIT + 1; i < TRANSMISSION_LENGTH; i++) {
            if (isPhase(movingsum[i - 1], movingsum[i], signalMaxSum) && switchphase) {
               isinphase = !isinphase;
               /* already switched */
               switchphase = false;
            }

            if (i % FRAMES_PER_BIT == 0) {
               if (!isinphase) {
                  /* i/MicSerialListener.FRAMES_PER_BIT-1 */
                  signals[s] |= 0x1 << bitnum;
               }
               bitnum++;
               /* reached a bit, can now switch */
               switchphase = true;
            }
         }
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

   /*
    * 2. cycle through the found PSK locations and find the specific start
    * points of individual transmissions.
    * 
    * Works in part by side-effecting <code>samplelocsize</code>
    */
   private int findPSKTransmissions(short[] samples, int numsampleloc, int[] sampleStartIndices) {
      int arraypos = 0;
      movingsum[0] = 0;
      int firstIndex = sampleStartIndices[0];
      int start = firstIndex + MOVING_SIZE;
      for (int i = firstIndex; i < start; i++) {
         movingbuf[i - firstIndex] = samples[i];
         movingsum[0] += samples[i];
      }
      int end = firstIndex + SAMPLE_LENGTH - BIT_OFFSET;
      for (int i = start; i < end; i++) {
         movingsum[1] = movingsum[0] - movingbuf[arraypos];
         movingsum[1] += samples[i];
         movingbuf[arraypos] = samples[i];
         arraypos++;
         if (arraypos == MOVING_SIZE) {
            arraypos = 0;
         }

         if (isPhase(movingsum[0], movingsum[1], signalMaxSum)) {
            sampleStartIndices[numsampleloc % SAMPLES_PER_BUFFER] = i - 5;

            samplelocsize = ++numsampleloc;
            if (numsampleloc >= SAMPLES_PER_BUFFER) {
               return samplelocsize;
            }
            /* next transmission */
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
            sampleStartIndices[numsampleloc % SAMPLES_PER_BUFFER] = i;
            ++numsampleloc;
            /* next transmission */
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
            sampleStartIndices[numsampleloc % SAMPLES_PER_BUFFER] = i;
            samplelocsize = ++numsampleloc;

            /*
             * finished with this signal, go back to search through next signal
             */
            break;
         } else {
            movingsum[0] = movingsum[1];
         }
      }
      return numsampleloc;
   }

   private boolean isPhase(int sum1, int sum2, int max) {
      return Math.abs(sum1 - sum2) > max;
   }

   private void autoGainControl(short[] samples, int[] sampleStartIndices) {
      signalMaxSum = 0;
      int index = 0;
      int firstIndex = sampleStartIndices[0];
      movingsum[0] = 0;
      for (int i = firstIndex; i < firstIndex + MOVING_SIZE; i++) {
         movingbuf[i - firstIndex] = samples[i];
         movingsum[0] += samples[i];
      }
      int endpos = firstIndex + PREAMBLE - BEGIN_OFFSET + SAMPLES_PER_BUFFER * (TRANSMISSION_LENGTH + BIT_OFFSET);
      for (int i = firstIndex + MOVING_SIZE; i < endpos; i++) {
         movingsum[1] = movingsum[0] - movingbuf[index];
         movingsum[1] += samples[i];
         movingbuf[index] = samples[i];
         index++;
         if (index == MOVING_SIZE) {
            index = 0;
         }

         int temp = Math.abs(movingsum[0] - movingsum[1]);
         if (temp > signalMaxSum) {
            signalMaxSum = temp;
         }

         movingsum[0] = movingsum[1];
      }

      signalMaxSum /= 1.375;
   }
}