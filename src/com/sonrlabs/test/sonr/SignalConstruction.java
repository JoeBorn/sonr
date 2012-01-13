package com.sonrlabs.test.sonr;

public class SignalConstruction
      implements AudioSupportConstants {

   private int signalMaxSum = 0;
   
   /* Eventually these should be private, once all the common code is moved here. */
   final int[] signals = new int[SAMPLES_PER_BUFFER];
   final int[] movingbuf = new int[MOVING_SIZE];
   final int[] movingsum = new int[TRANSMISSION_LENGTH];
   
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
   
   protected int findSample(int startpos, short[] samples, int numsampleloc, int[] sampleStartIndices) {
      movingsum[0] = 0;
      int start = startpos + MOVING_SIZE;
      for (int i = startpos; i < start; i++) {
         movingbuf[i - startpos] = samples[i];
         movingsum[0] += samples[i];
      }
      int arraypos = 0;
      int end = startpos + SAMPLE_LENGTH - BIT_OFFSET;
      for (int i = start; i < end; i++) {
         movingsum[1] = movingsum[0] - movingbuf[arraypos];
         movingsum[1] += samples[i];
         movingbuf[arraypos] = samples[i];
         arraypos++;
         if (arraypos == MOVING_SIZE) {
            arraypos = 0;
         }
   
         if (isPhaseChange(0)) {
            sampleStartIndices[numsampleloc++] = i - 5;
            if (numsampleloc >= SAMPLES_PER_BUFFER) {
               break;
            }
            // next transmission
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
            sampleStartIndices[numsampleloc++] = i;
            // next transmission
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
            sampleStartIndices[numsampleloc] = i;
            break;
         } else {
            movingsum[0] = movingsum[1];
         }
      }
      return numsampleloc;
   }
   protected void computeSignalMax(short[] samples, int startpos) {
      movingsum[0] = 0;
      for (int i = startpos; i < startpos + MOVING_SIZE; i++) {
         movingbuf[i - startpos] = samples[i];
         movingsum[0] += samples[i];
      }
      signalMaxSum = 0;
      int index = 0;
      int endpos = startpos + PREAMBLE - BEGIN_OFFSET + SAMPLES_PER_BUFFER * (TRANSMISSION_LENGTH + BIT_OFFSET);
      for (int i = startpos + MOVING_SIZE; i < endpos; i++) {
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
