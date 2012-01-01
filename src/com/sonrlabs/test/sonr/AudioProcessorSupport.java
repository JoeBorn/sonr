/***************************************************************************
 * Copyright 2011 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

/**
 * Messy audio signal processing used by {@link AudioProcessor} to detect user
 * signals from the remote.
 * 
 * This is effectively a singleton since it's only instantiated once per
 * {@link AudioProcessor}, and that in turn is effectively a singleton.
 * 
 */
final class AudioProcessorSupport
      implements AudioSupportConstants {

   private boolean preambleIsCutOff = false;
   private int preambleOffset = 0;
   private int signalMaxSum = 0;
   
   private final int[] movingsum = new int[TRANSMISSION_LENGTH];
   private final int[] movingbuf = new int[MOVING_SIZE];
   private final int[][] trans_buf = new int[MAX_TRANSMISSIONS * SAMPLES_PER_BUFFER][TRANSMISSION_LENGTH + BIT_OFFSET];
   private final int[] byteInDec = new int[MAX_TRANSMISSIONS * SAMPLES_PER_BUFFER];
   private final int[][] sloc = new int[MAX_TRANSMISSIONS][SAMPLES_PER_BUFFER];
   
   /**
    * This is the entry point for {@link AudioProcessor}. It looks for a
    * user-initiated signal (from the remote control) and processes it if it
    * finds one.
    * 
    * FIXME: This will occasionally 'detect' a signal when there wasn't one.
    * 
    * TODO: Find out when and why that happens,
    */
   void nextSample(int count, short[] sample_buf) {
      int sampleLocSize = findSample(count, sample_buf);
      if (sampleLocSize > 0) {
         processSample(sampleLocSize, count, sample_buf);
      }
   }

   private int findSample(int count, short[] sample_buf) {
      int currentIndex = 0;
      int arraypos;
      int numfoundsamples = 0;

      if (preambleIsCutOff) {
         sloc[numfoundsamples++][0] = preambleOffset;
         preambleIsCutOff = false;
         currentIndex += SAMPLE_LENGTH + END_OFFSET;
         // Log.d(TAG, "PREAMBLE CUT OFF BEGIN");
      } else {
         currentIndex = SAMPLE_LENGTH;
      }

      while (currentIndex < count - 1) {
         /* 1. find where the PSK signals begin */
         if (Math.abs(sample_buf[currentIndex] - sample_buf[currentIndex + 1]) > THRESHOLD) {
            if (currentIndex >= SAMPLE_LENGTH && currentIndex < SAMPLE_LENGTH * 2 && numfoundsamples == 0) {
               currentIndex -= SAMPLE_LENGTH;
               while (Math.abs(sample_buf[currentIndex] - sample_buf[currentIndex + 1]) < THRESHOLD) {
                  currentIndex++;
               }
            }
            if (currentIndex + PREAMBLE >= count) {
               // Log.d(TAG, "PREAMBLE CUT OFF");
               if (currentIndex + BEGIN_OFFSET <= count) {
                  preambleOffset = 0;
               } else {
                  preambleOffset = currentIndex + BEGIN_OFFSET - count;
               }
               preambleIsCutOff = true;
               break;
            } else {
               /* preamble not cut off */
               sloc[numfoundsamples++][0] = currentIndex + BEGIN_OFFSET;
               if (numfoundsamples >= MAX_TRANSMISSIONS) {
                  break;
               }
               currentIndex += SAMPLE_LENGTH + END_OFFSET;
            }
         }
         currentIndex++;
      }

      if (numfoundsamples > 0) {
         autoGainControl(sample_buf);
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
         for (int i = sloc[n][0]; i < sloc[n][0] + MOVING_SIZE; i++) {
            movingbuf[i - sloc[n][0]] = sample_buf[i];
            movingsum[0] += sample_buf[i];
         }
         for (int i = sloc[n][0] + MOVING_SIZE; i < sloc[n][0] + SAMPLE_LENGTH - BIT_OFFSET; i++) {
            movingsum[1] = movingsum[0] - movingbuf[arraypos];
            movingsum[1] += sample_buf[i];
            movingbuf[arraypos] = sample_buf[i];
            arraypos++;
            if (arraypos == MOVING_SIZE) {
               arraypos = 0;
            }

            if (isPhase(movingsum[0], movingsum[1], signalMaxSum)) {
               sloc[numsampleloc / SAMPLES_PER_BUFFER][numsampleloc % SAMPLES_PER_BUFFER] = i - 5;

               samplelocsize = ++numsampleloc;
               if (numsampleloc >= MAX_TRANSMISSIONS * SAMPLES_PER_BUFFER) {
                  return samplelocsize;
               }
               /* next transmission */
               i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
               sloc[numsampleloc / SAMPLES_PER_BUFFER][numsampleloc % SAMPLES_PER_BUFFER] = i;
               ++numsampleloc;
               /* next transmission */
               i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
               sloc[numsampleloc / SAMPLES_PER_BUFFER][numsampleloc % SAMPLES_PER_BUFFER] = i;
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
                * searching for signals arraypos = 0; for(int t = i; t < i + MAGIC_9;
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

   private void processSample(int sampleLocSize, int count, short[] sample_buf) {
      /* copy transmission down because the buffer could get overwritten */
      for (int j = 0; j < sampleLocSize; j++) {
         for (int i = 0; i < TRANSMISSION_LENGTH; i++) {
            if (sloc[j / SAMPLES_PER_BUFFER][j % SAMPLES_PER_BUFFER] + i < count) {
               trans_buf[j][i] = sample_buf[sloc[j / SAMPLES_PER_BUFFER][j % SAMPLES_PER_BUFFER] + i];
            } else {
               /*
                * Deleted an else clause here that was originally waiting on
                * aother buffer in a way that wasn't thread-safe. Unclear how
                * to make this work reliably.
                */
            }
         }
      }
      // if (count2 != 0) {
      // Log.d(TAG, "CUT OFF");

      for (int s = 0; s < sampleLocSize; s++) {
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

         // if(byteInDec[s] != BOUND || samplelocsize < SAMPLE_CHUNK_COUNT) {
         // Log.d(TAG, "--------------");
         // }
      }

      if (sampleLocSize > 1) {
         /* 2 or more */
         for (int i = 0; i < sampleLocSize; i += SAMPLES_PER_BUFFER) {
            /*
             * receive byte using best two out of three.
             */
            int byte0 = byteInDec[i];
            int byte1 = byteInDec[i + 1];
            int byte2 = byteInDec[i + 2];
            if ((byte0 == byte1 || byte0 == byte2) && byte0 != BOUNDARY) {
               AudioProcessorQueue.singleton.processAction(byte0);
            } else if (byte1 == byte2 && byte1 != BOUNDARY) {
               AudioProcessorQueue.singleton.processAction(byte1);
            }
         }
      }
   }

   private void autoGainControl(short[] sample_buf) {
      signalMaxSum = 0;
      int arraypos = 0;
      int startpos = sloc[0][0];
      movingsum[0] = 0;
      for (int i = startpos; i < startpos + MOVING_SIZE; i++) {
         movingbuf[i - startpos] = sample_buf[i];
         movingsum[0] += sample_buf[i];
      }
      for (int i = startpos + MOVING_SIZE; i < startpos + PREAMBLE - BEGIN_OFFSET + SAMPLES_PER_BUFFER * (TRANSMISSION_LENGTH + BIT_OFFSET); i++) {
         movingsum[1] = movingsum[0] - movingbuf[arraypos];
         movingsum[1] += sample_buf[i];
         movingbuf[arraypos] = sample_buf[i];
         arraypos++;
         if (arraypos == MOVING_SIZE) {
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
