/***************************************************************************
 * Copyright 2011 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

/**
 * Messy audio signal processing used by {@link AudioProcessor} to detect user
 * signals from the remote.
 * 
 */
final class AudioProcessorSupport
      implements AudioSupportConstants {

   private boolean preambleIsCutOff = false;
   private int preambleOffset = 0;
   private int signalMaxSum = 0;
   
   private final int[] movingsum = new int[TRANSMISSION_LENGTH];
   private final int[] movingbuf = new int[9];
   private final int[][] trans_buf = new int[MAX_TRANSMISSIONS * 3][TRANSMISSION_LENGTH + BIT_OFFSET];
   private final int[] byteInDec = new int[MAX_TRANSMISSIONS * 3];
   private final int[][] sloc = new int[MAX_TRANSMISSIONS][3];
   
   /**
    * This is the entry point for {@link AudioProcessor}. It looks for a
    * user-initiated signal (from the remote control) and processes it if it
    * finds one.
    * 
    * FIXME: This will occasionally 'detect' a signal when there wasn't one.
    * 
    * TODO: Find out when and why that happens,
    */
   void nextSample(int numSamples, short[] sample_buf) {
      int sampleLocSize = processorFindSample(numSamples, sample_buf);
      if (sampleLocSize > 0) {
         processSample(sampleLocSize, numSamples, sample_buf);
      }
   }

   private int processorFindSample(int numSamples, short[] sample_buf) {
      int count = 0;
      int arraypos;
      int numfoundsamples = 0;

      if (preambleIsCutOff) {
         sloc[numfoundsamples++][0] = preambleOffset;
         preambleIsCutOff = false;
         count += SAMPLE_LENGTH + END_OFFSET;
         // Log.d(TAG, "PREAMBLE CUT OFF BEGIN");
      } else {
         count = SAMPLE_LENGTH;
      }

      while (count < numSamples - 1) {
         /* 1. find where the PSK signals begin */
         if (Math.abs(sample_buf[count] - sample_buf[count + 1]) > THRESHOLD) {
            if (count >= SAMPLE_LENGTH && count < SAMPLE_LENGTH * 2 && numfoundsamples == 0) {
               count -= SAMPLE_LENGTH;
               while (Math.abs(sample_buf[count] - sample_buf[count + 1]) < THRESHOLD) {
                  count++;
               }
            }
            if (count + PREAMBLE >= numSamples) {
               // Log.d(TAG, "PREAMBLE CUT OFF");
               if (count + BEGIN_OFFSET <= numSamples) {
                  preambleOffset = 0;
               } else {
                  preambleOffset = count + BEGIN_OFFSET - numSamples;
               }
               preambleIsCutOff = true;
               break;
            } else {
               /* preamble not cut off */
               sloc[numfoundsamples++][0] = count + BEGIN_OFFSET;
               if (numfoundsamples >= MAX_TRANSMISSIONS) {
                  break;
               }
               count += SAMPLE_LENGTH + END_OFFSET;
            }
         }
         count++;
      }

      if (numfoundsamples > 0) {
         processorAGC(sample_buf);
      }

      int numsampleloc = 0;
      int samplelocsize= 0;
      for (int n = 0; n < numfoundsamples; n++) {
         /*
          * 2. cycle through the found PSK locations and find the specific start
          * points of individual transmissions
          */
         arraypos = 0;
         movingsum[0] = 0;
         for (int i = sloc[n][0]; i < sloc[n][0] + 9; i++) {
            movingbuf[i - sloc[n][0]] = sample_buf[i];
            movingsum[0] += sample_buf[i];
         }
         for (int i = sloc[n][0] + 9; i < sloc[n][0] + SAMPLE_LENGTH - BIT_OFFSET; i++) {
            movingsum[1] = movingsum[0] - movingbuf[arraypos];
            movingsum[1] += sample_buf[i];
            movingbuf[arraypos] = sample_buf[i];
            arraypos++;
            if (arraypos == 9) {
               arraypos = 0;
            }

            if (isPhase(movingsum[0], movingsum[1], signalMaxSum)) {
               sloc[numsampleloc / 3][numsampleloc % 3] = i - 5;

               samplelocsize = ++numsampleloc;
               if (numsampleloc >= MAX_TRANSMISSIONS * 3) {
                  return samplelocsize;
               }
               /* next transmission */
               i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
               sloc[numsampleloc / 3][numsampleloc % 3] = i;
               ++numsampleloc;
               /* next transmission */
               i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
               sloc[numsampleloc / 3][numsampleloc % 3] = i;
               samplelocsize = ++numsampleloc;

               /*
                * finished with this signal, go back to search through next
                * signal
                */
               break;

               /*
                * i += MicSerialListener.TRANSMISSION_LENGTH +
                * MicSerialListener.BIT_OFFSET +
                * MicSerialListener.FRAMES_PER_BIT +
                * MicSerialListener.BEGIN_OFFSET;
                * 
                * movingsum[0] = 0; //re-set up the variables to continue
                * searching for signals arraypos = 0; for(int t = i; t < i + 9;
                * t++) { movingbuf[t - i] = sample_buf[t]; movingsum[0] +=
                * sample_buf[t]; } i += 4;
                */
            } else {
               movingsum[0] = movingsum[1];
            }
         }
      } // end loop through numsamplesfound
      return samplelocsize;
   }

   private void processSample(int sampleLocSize, int numSamples, short[] sample_buf) {
      /* copy transmission down because the buffer could get overwritten */
      for (int j = 0; j < sampleLocSize; j++) {
         for (int i = 0; i < TRANSMISSION_LENGTH; i++) {
            if (sloc[j / 3][j % 3] + i < numSamples) {
               trans_buf[j][i] = sample_buf[sloc[j / 3][j % 3] + i];
            }
         }
      }
      // if (count2 != 0) {
      // Log.d(TAG, "CUT OFF");

      for (int s = 0; s < sampleLocSize; s++) {
         int arraypos = 0;
         movingsum[0] = 0;
         for (int i = 0; i < 9; i++) {
            movingbuf[i] = trans_buf[s][i];
            movingsum[0] += trans_buf[s][i];
         }

         for (int i = 9; i < TRANSMISSION_LENGTH; i++) {
            movingsum[i] = movingsum[i - 1] - movingbuf[arraypos];
            movingsum[i] += trans_buf[s][i];
            movingbuf[arraypos] = trans_buf[s][i];
            arraypos++;
            if (arraypos == 9) {
               arraypos = 0;
            }
         }

         /* we start out with a phase shift */
         boolean isinphase = true, switchphase = true;
         int bitnum = 0;
         byteInDec[s] = 0;

         for (int i = FRAMES_PER_BIT + 1; i < TRANSMISSION_LENGTH; i++) {
            if (isPhase(movingsum[i - 1], movingsum[i], signalMaxSum) && switchphase) {
               isinphase = !isinphase;
               /* already switched */
               switchphase = false;
            }

            if (i % FRAMES_PER_BIT == 0) {
               if (!isinphase) {
                  /* i/MicSerialListener.FRAMES_PER_BIT-1 */
                  byteInDec[s] |= 0x1 << bitnum;
               }
               bitnum++;
               /* reached a bit, can now switch */
               switchphase = true;
            }
         }

         // Log.d(TAG, "TRANSMISSION[" + s + "]: " + "0x"+
         // Integer.toHexString(byteInDec[s]));

         // if(byteInDec[s] != 0x27 || samplelocsize < 3) {
         // Log.d(TAG, "--------------");
         // }
      }

      if (sampleLocSize > 1) {
         /* 2 or more */
         for (int i = 0; i < sampleLocSize; i += 3) {
            /*
             * receive byte using best two out of three.
             */
            if ((byteInDec[i] == byteInDec[i + 1] || byteInDec[i] == byteInDec[i + 2]) && byteInDec[i] != 0x27) {
               AudioProcessorQueue.singleton.processAction(byteInDec[i]);
            } else if (byteInDec[i + 1] == byteInDec[i + 2] && byteInDec[i + 1] != 0x27) {
               AudioProcessorQueue.singleton.processAction(byteInDec[i + 1]);
            }
         }
      }
   }

   private void processorAGC(short[] sample_buf) {
      signalMaxSum = 0;
      int arraypos = 0;
      int startpos = sloc[0][0];
      movingsum[0] = 0;
      for (int i = startpos; i < startpos + 9; i++) {
         movingbuf[i - startpos] = sample_buf[i];
         movingsum[0] += sample_buf[i];
      }
      for (int i = startpos + 9; i < startpos + PREAMBLE - BEGIN_OFFSET + 3 * (TRANSMISSION_LENGTH + BIT_OFFSET); i++) {
         movingsum[1] = movingsum[0] - movingbuf[arraypos];
         movingsum[1] += sample_buf[i];
         movingbuf[arraypos] = sample_buf[i];
         arraypos++;
         if (arraypos == 9) {
            arraypos = 0;
         }

         int temp = Math.abs(movingsum[0] - movingsum[1]);
         if (temp > signalMaxSum) {
            signalMaxSum = temp;
         }

         movingsum[0] = movingsum[1];
      }

      signalMaxSum /= 1.375;
   }

   private boolean isPhase(int sum1, int sum2, int max) {
      return Math.abs(sum1 - sum2) > max;
   }

}
