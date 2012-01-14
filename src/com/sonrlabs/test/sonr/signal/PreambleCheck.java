/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;


/**
 * Look for signal start, taking into account the 'preamble'.
 * 
 * XXX What is this preamble stuff about?
 */
final class PreambleCheck
      implements AudioSupportConstants {
   /*
    * XXX Seems suspicious that these are fields, with values that persist
    * between calls
    */
   private boolean preambleIsCutOff;
   private int preambleOffset = 0;

   private int currentIndex;
   private int sampleCount;

   int countSamples(int count, short[] samples, int[] sampleStartIndicies) {
      currentIndex = 0;
      sampleCount = 0;
      if (preambleIsCutOff) {
         ++sampleCount;
         sampleStartIndicies[0] = preambleOffset;
         preambleIsCutOff = false;
         currentIndex += SAMPLE_LENGTH + END_OFFSET;
         // Log.d(TAG, "PREAMBLE CUT OFF BEGIN");
      } else {
         currentIndex = SAMPLE_LENGTH;
      }

      findPSKBegin(count, samples, sampleStartIndicies);
      return sampleCount;
   }

   private void findPSKBegin(int count, short[] samples, int[] sampleStartIndices) {
      while (currentIndex < count - 1) {
         if (Math.abs(samples[currentIndex] - samples[currentIndex + 1]) > THRESHOLD) {
            if (currentIndex >= SAMPLE_LENGTH && currentIndex < SAMPLE_LENGTH * 2 && sampleCount == 0) {
               currentIndex -= SAMPLE_LENGTH;
               while (Math.abs(samples[currentIndex] - samples[currentIndex + 1]) < THRESHOLD) {
                  currentIndex++;
               }
            }
            if (currentIndex + PREAMBLE >= count) {
               // Log.d("Audio Processor", "PREAMBLE CUT OFF");
               if (currentIndex + BEGIN_OFFSET <= count) {
                  preambleOffset = 0;
               } else {
                  preambleOffset = currentIndex + BEGIN_OFFSET - count;
               }
               preambleIsCutOff = true;
            } else if (sampleCount == 0) {
               /* preamble not cut off */
               ++sampleCount;
               sampleStartIndices[0] = currentIndex + BEGIN_OFFSET;
            }
            break;
         }
         currentIndex++;
      }
   }
}