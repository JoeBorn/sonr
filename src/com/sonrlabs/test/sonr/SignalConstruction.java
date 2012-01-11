package com.sonrlabs.test.sonr;

public class SignalConstruction
      implements AudioSupportConstants {

   /* Eventually these should be private, once all the common code is moved here. */
   final int[] signals = new int[SAMPLES_PER_BUFFER];
   final int[] movingbuf = new int[MOVING_SIZE];
   final int[] movingsum = new int[TRANSMISSION_LENGTH];
   int signalMaxSum = 0;
   
   protected void constructSignal(int signalIndex) {
      /* we start out with a phase shift and 0 signal */
      boolean inphase = true, switchphase = true;
      signals[signalIndex] = 0;
   
      for (int i = FRAMES_PER_BIT + 1, bitnum=0; i < TRANSMISSION_LENGTH; i++) {
         if (switchphase && isPhaseChange(i-1)) {
            inphase = !inphase;
            switchphase = false;
         }
   
         boolean atFrameBoundary = i % FRAMES_PER_BIT == 0;
         if (atFrameBoundary) {
            if (!inphase) {
               signals[signalIndex] |= 0x1 << bitnum;
            }
            bitnum++;
            /* reached a bit, can now switch */
            switchphase = true;
         }
      }
   }
   protected boolean isPhaseChange(int movingSumIndex) {
      int sum1 = movingsum[movingSumIndex];
      int sum2 = movingsum[movingSumIndex+1];
      return Math.abs(sum1 - sum2) > signalMaxSum;
   }
}
