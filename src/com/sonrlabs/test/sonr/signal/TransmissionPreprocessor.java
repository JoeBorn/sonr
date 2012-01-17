/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;

import com.sonrlabs.test.sonr.ISampleBuffer;


/**
 * Look for signal start, taking into account the 'preamble',
 * a section of signal designed to provide the time for the
 * phone's internal hardware gain control to get adjusted to the signal amplitude.
 * <p>
 * Transmission Structure: [Preamble] [Data] [Copy of data] [Second copy of data]
 * <pre>
 * Data: start bit (phase shift), bit 0, bit 1, ... bit 8, end bit (phase shift)
 * Bit: 18 samples (44,100 Hz sample rate/2400 bps data rate=18 samples per bit)
 * Preamble:  64 bits = 1152 samples
 * </pre>
 */
final class TransmissionPreprocessor
      implements AudioSupportConstants {
   
   private final TransmissionFinder finder = new TransmissionFinder();
   private final int[] sampleStartIndices = new int[SAMPLES_PER_BUFFER];

   /*
    * XXX Seems suspicious that these two persist between calls
    */
   private boolean preambleIsCutOff;
   private int preambleOffset = 0;

   void nextSample(ISampleBuffer buffer) {
      int numfoundsamples = countSamples(buffer);
      if (numfoundsamples > 0) {
         finder.nextSample(buffer, numfoundsamples, sampleStartIndices);
      }
   }
   
   private int countSamples(ISampleBuffer buffer) {
      int currentIndex = 0;
      int sampleCount = 0;
      if (preambleIsCutOff) {
         ++sampleCount;
         sampleStartIndices[0] = preambleOffset;
         preambleIsCutOff = false;
         currentIndex += SAMPLE_LENGTH + END_OFFSET;
         // Log.d(TAG, "PREAMBLE CUT OFF BEGIN");
      } else {
         currentIndex = SAMPLE_LENGTH;
      }
      return findPSKBegin(buffer, currentIndex, sampleCount);
   }

   private int findPSKBegin(ISampleBuffer buffer, int currentIndex, int sampleCount) {
      int count = buffer.getCount();
      short[] samples = buffer.getArray();
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
      return sampleCount;
   }
}