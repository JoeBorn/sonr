/***************************************************************************
 * Copyright 2011 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import android.util.Log;

/**
 * Sampling support.
 * 
 * Two sets of methods for now, one used by {@link MicSerialListener} the other
 * used by {@link AudioProcessor}. They should share code at some point.
 * 
 */
final class SampleSupport {
   
   static final SampleSupport singleton = new SampleSupport();
   
   private static final String TAG = SampleSupport.class.getSimpleName();
   
   private static final short SERIAL_TRANSMITTER_BAUD = 2400;
   private static final int SAMPLE_RATE = 44100; // In Hz
   private static final int FRAMES_PER_BIT = SAMPLE_RATE / SERIAL_TRANSMITTER_BAUD;
   private static final int TRANSMISSION_LENGTH = FRAMES_PER_BIT * 8;
   private static final int BIT_OFFSET = FRAMES_PER_BIT * 2;
   private static final int PREAMBLE = 64 * FRAMES_PER_BIT;
   private static final int SAMPLE_LENGTH = PREAMBLE + 3 * (TRANSMISSION_LENGTH + BIT_OFFSET);
   /* allow phone's internal AGC to stabilize first */
   private static final int BEGIN_OFFSET = PREAMBLE - TRANSMISSION_LENGTH - BIT_OFFSET;
   private static final int END_OFFSET = TRANSMISSION_LENGTH + BIT_OFFSET;
   // beginning of a sample
   private static final int THRESHOLD = 4000;
   // transmissions in a single nsample
   private static final int MAX_TRANSMISSIONS = 10;

   
   // Not clear why these are static.
   private static boolean PreambleIsCutOff = false;
   private static int Preamble_Offset = 0;
   private static int SIGNAL_MAX_SUM = 0;
   
   private final int[] movingsum = new int[TRANSMISSION_LENGTH];
   private final int[] movingbuf = new int[9];
   private final int[][] trans_buf = new int[MAX_TRANSMISSIONS * 3][TRANSMISSION_LENGTH + BIT_OFFSET];
   private final int[] byteInDec = new int[MAX_TRANSMISSIONS * 3];
   private final int[][] sloc = new int[MAX_TRANSMISSIONS][3];

   
   private SampleSupport() {
   }
   
   /**
    * This is the entry point for {@link MicSerialListener}.
    */
   boolean autoGainControl(short[] samples, int numSamples) {

      boolean found = false;
      int startpos = SAMPLE_LENGTH;
      int arraypos = 0;
      int sampleloc[] = new int[3];
      while (startpos < numSamples - 1 && Math.abs(samples[startpos] - samples[startpos + 1]) < THRESHOLD) {
         startpos++;
      }

      if (startpos < numSamples - 1 && startpos >= SAMPLE_LENGTH && startpos < SAMPLE_LENGTH * 2) {
         startpos -= SAMPLE_LENGTH;
         while (Math.abs(samples[startpos] - samples[startpos + 1]) < THRESHOLD) {
            // && startpos < numSamples-1)
            startpos++;
         }
      }

      startpos += BEGIN_OFFSET;

      if (startpos < numSamples - (SAMPLE_LENGTH - BEGIN_OFFSET)) {
         Log.d(TAG, "Found a sample...");

         movingsum[0] = 0;
         for (int i = startpos; i < startpos + 9; i++) {
            movingbuf[i - startpos] = samples[i];
            movingsum[0] += samples[i];
         }
         SIGNAL_MAX_SUM = 0;
         for (int i = startpos + 9; i < startpos + PREAMBLE - BEGIN_OFFSET + 3 * (TRANSMISSION_LENGTH + BIT_OFFSET); i++) {
            movingsum[1] = movingsum[0] - movingbuf[arraypos];
            movingsum[1] += samples[i];
            movingbuf[arraypos] = samples[i];
            arraypos++;
            if (arraypos == 9) {
               arraypos = 0;
            }

            int temp = Math.abs(movingsum[0] - movingsum[1]);
            if (temp > SIGNAL_MAX_SUM) {
               SIGNAL_MAX_SUM = temp;
            }

            // test_buf[i - startpos - 9] = sample_buf1[i];
            // movingsum2[i - startpos - 9] = movingsum[0];
            movingsum[0] = movingsum[1];
         }

         SIGNAL_MAX_SUM /= 1.375;
         findSample(startpos, samples, sampleloc);

         int[] triple = new int[3];
         for (int n = 0; n < 3; n++) {
            if (sampleloc[n] != 0) {
               arraypos = 0;
               movingsum[0] = 0;
               for (int i = 0; i < 9; i++) {
                  movingbuf[i] = samples[i + sampleloc[n]];
                  movingsum[0] += samples[i + sampleloc[n]];
               }
               for (int i = 9; i < TRANSMISSION_LENGTH; i++) {
                  movingsum[i] = movingsum[i - 1] - movingbuf[arraypos];
                  movingsum[i] += samples[i + sampleloc[n]];
                  movingbuf[arraypos] = samples[i + sampleloc[n]];
                  arraypos++;
                  if (arraypos == 9) {
                     arraypos = 0;
                  }
               }

               boolean isinphase = true, switchphase = true;
               /* we start out with a phase shift */
               int bitnum = 0;

               for (int i = FRAMES_PER_BIT + 1; i < TRANSMISSION_LENGTH; i++) {
                  if (isPhase(movingsum[i - 1], movingsum[i], SIGNAL_MAX_SUM) && switchphase) {
                     isinphase = !isinphase;
                     switchphase = false; // already switched
                  }

                  if (i % FRAMES_PER_BIT == 0) {
                     if (!isinphase) {
                        triple[n] |= 0x1 << bitnum;
                     }
                     bitnum++;
                     /* reached a bit, can now switch again if phase shifts */
                     switchphase = true;
                  }
               }

               Log.d(TAG, "TRANSMISSION[" + n + "]: " + "0x" + Integer.toHexString(triple[n]));
               // LogFile.MakeLog("TRANSMISSION[" + n + "]: " + "0x"+
               // Integer.toHexString(byteInDec[n]));
            }
         }

         /* If at least two are 0x27, that's a match. */
         int matchCount = 0;
         for (int value : triple) {
            if (value == 0x27) {
               ++matchCount;
            }
         }
         found = matchCount >= 2;
      }

      return found;
   }
   
   private void findSample(int startpos, short[] samples, int[] sampleloc) {
      int arraypos = 0;
      int numsampleloc = 0;
      movingsum[0] = 0;
      for (int i = startpos; i < startpos + 9; i++) {
         movingbuf[i - startpos] = samples[i];
         movingsum[0] += samples[i];
      }

      for (int i = startpos + 9; i < startpos + SAMPLE_LENGTH - BIT_OFFSET; i++) {
         movingsum[1] = movingsum[0] - movingbuf[arraypos];
         movingsum[1] += samples[i];
         movingbuf[arraypos] = samples[i];
         arraypos++;
         if (arraypos == 9) {
            arraypos = 0;
         }

         if (isPhase(movingsum[0], movingsum[1], SIGNAL_MAX_SUM)) {
            sampleloc[numsampleloc++] = i - 5;
            // next transmission
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
            sampleloc[numsampleloc++] = i;
            // next transmission
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
            sampleloc[numsampleloc] = i;
            return;
         }

         movingsum[0] = movingsum[1];
      }
   }

   /**
    *  This is the entry point for {@link AudioProcessor}.
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

      if (PreambleIsCutOff) {
         sloc[numfoundsamples++][0] = Preamble_Offset;
         PreambleIsCutOff = false;
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
                  Preamble_Offset = 0;
               } else {
                  Preamble_Offset = count + BEGIN_OFFSET - numSamples;
               }
               PreambleIsCutOff = true;
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

            if (isPhase(movingsum[0], movingsum[1], SIGNAL_MAX_SUM)) {
               sloc[numsampleloc / 3][numsampleloc % 3] = i - 5;

               samplelocsize = ++numsampleloc;
               if (numsampleloc >= MAX_TRANSMISSIONS * 3) {
                  return samplelocsize;
               }
               /* next transmission */
               i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
               sloc[numsampleloc / 3][numsampleloc % 3] = i;
               samplelocsize = ++numsampleloc;
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
            if (isPhase(movingsum[i - 1], movingsum[i], SIGNAL_MAX_SUM) && switchphase) {
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
      SIGNAL_MAX_SUM = 0;
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
         if (temp > SIGNAL_MAX_SUM) {
            SIGNAL_MAX_SUM = temp;
         }

         movingsum[0] = movingsum[1];
      }

      SIGNAL_MAX_SUM /= 1.375;
   }

   private boolean isPhase(int sum1, int sum2, int max) {
      return Math.abs(sum1 - sum2) > max;
   }

}
