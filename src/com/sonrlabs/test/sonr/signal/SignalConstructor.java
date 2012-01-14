package com.sonrlabs.test.sonr.signal;

import android.util.Log;

import com.sonrlabs.test.sonr.AudioProcessorQueue;

abstract class SignalConstructor
      implements AudioSupportConstants {

   private static final String TAG = "SignalConstructor";
   private static final int MIN_MATCHES = 2;
   private int signalMaxSum = 0;
   private final int[] movingbuf = new int[MOVING_SIZE];
   private final int[] movingsum = new int[TRANSMISSION_LENGTH];
   private final int[] signals = new int[SAMPLES_PER_BUFFER];

   /**
    * The use case for this method is {@link #constructSignal(short[], int[])}.
    * That method is invoked by both by {@link ListenerAudioSupport} and also by
    * {@link TransmissionFinder}
    * <p>
    * The former had a test to determine whether or not the construction should
    * proceed, the latter did not. Otherwise the code was identical. To handle
    * this I added the abstract method.
    * <p>
    * The distinction seems slightly suspicious. More likely either both cases
    * should be running the same test (<code> signal != 0</code> ) or neither
    * should.
    * 
    * 
    * @param signal the current signal value.
    * 
    * @return whether or not signal construction should proceeed.
    */
   abstract protected boolean checkSignal(int signal);

   void processSignalIfMatch()
         throws SpuriousSignalException {
      for (int i = 0; i < signals.length; i++) {
         int baseSignal = signals[i];
         if (baseSignal != 0 && baseSignal != BOUNDARY) {
            int matchCount = 1;
            for (int j = i + 1; j < signals.length; j++) {
               if (baseSignal == signals[j]) {
                  ++matchCount;
               }
            }
            if (matchCount >= MIN_MATCHES) {
               AudioProcessorQueue.processAction(baseSignal);
               String msg = "Detected signal " 
                     + Integer.toBinaryString(baseSignal) + " [0x" + Integer.toHexString(baseSignal) + "] with "
                     + matchCount + " matches";
               Log.d(TAG, msg);
               return;
            }
         }
      }
   }

   int countBoundSignals() {
      int matchCount = 0;
      for (int value : signals) {
         if (value == BOUNDARY) {
            ++matchCount;
         }
      }
      return matchCount;
   }

   boolean isPhaseChange(int movingSumIndex) {
      int sum1 = movingsum[movingSumIndex];
      int sum2 = movingsum[movingSumIndex + 1];
      return Math.abs(sum1 - sum2) > signalMaxSum;
   }

   int findSample(int startpos, short[] samples, int numsampleloc, int[] sampleStartIndices) {
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

   void computeSignalMax(short[] samples, int startpos) {
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

   void constructSignal(short[] samples, int[] sampleStartIndices) {
      for (int signalIndex = 0; signalIndex < SAMPLES_PER_BUFFER; signalIndex++) {
         if (checkSignal(sampleStartIndices[signalIndex])) {
            int index = 0;
            movingsum[0] = 0;
            for (int i = 0; i < MOVING_SIZE; i++) {
               short value = samples[i + sampleStartIndices[signalIndex]];
               movingbuf[i] = value;
               movingsum[0] += value;
            }
            for (int i = MOVING_SIZE; i < TRANSMISSION_LENGTH; i++) {
               movingsum[i] = movingsum[i - 1] - movingbuf[index];
               short value = samples[i + sampleStartIndices[signalIndex]];
               movingsum[i] += value;
               movingbuf[index] = value;
               index++;
               if (index == MOVING_SIZE) {
                  index = 0;
               }
            }

            constructSignal(signalIndex);
         }
      }
   }

   private void constructSignal(int signalIndex) {
      /* we start out with a phase shift and 0 signal */
      boolean inphase = true, switchphase = true;
      signals[signalIndex] = 0;

      for (int i = FRAMES_PER_BIT + 1, bitnum = 0; i < TRANSMISSION_LENGTH; i++) {
         if (switchphase && isPhaseChange(i - 1)) {
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
}
