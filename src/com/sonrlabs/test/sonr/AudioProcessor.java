package com.sonrlabs.test.sonr;

import java.util.List;

import org.acra.ErrorReporter;

/**
 * Process an ordered collecton of reusable sample buffers.
 * 
 * This is effectively a singleton as it's only instantiated once by the
 * {@link AudioProcessorQueue} singleton.
 * 
 * There are almost certainly one or more bugs buried in this complex code that
 * generate signals when the user didn't do anything on the remote or the dock.
 * The excessive complexity can easily hide such bugs, so any simplifications
 * are worth doing, even if they don't fix any specific issue. The simpler the
 * code, the easier it is to debug and maintain.
 * 
 * The dock itself might also have firmware issues that cause the same symptom,
 * but it's unlikely to be the only cause.
 * 
 * TODO Simplify this code
 * 
 * FIXME find the source(s) of the spurious signal bug.
 */
final class AudioProcessor
      implements AudioSupportConstants {
   
   /**
    * XXX What is this preamble about?
    * 
    * XXX Seems suspicious that these are fields, with values that persist between
    * calls why aren't they local to {@link #findSample()}?
    */
   private boolean preambleIsCutOff = false;
   private int preambleOffset = 0;
   
   private int signalMaxSum = 0;
   
   private final int[] movingsum = new int[TRANSMISSION_LENGTH];
   private final int[] movingbuf = new int[MOVING_SIZE];
   private final int[][] trans_buf = new int[SAMPLES_PER_BUFFER][TRANSMISSION_LENGTH + BIT_OFFSET];
   private final int[] byteInDec = new int[SAMPLES_PER_BUFFER];
   private final int[] sloc = new int[SAMPLES_PER_BUFFER];

   
   /**
    * Process the next set of buffers. For now do this one at a time.
    * 
    * In principle we could group up to N together where N is some maximum, by
    * increasing the size of the some arrays by a factor of N and making sloc
    * two-dimensional, with N as the first dimension. This is how the code used
    * to look.
    * 
    * Grouping adds complexity but was (maybe) an advantage in the old model
    * where each run was a new thread. Creating and starting threads is a
    * non-trivial expense, and grouping decreases the number of threads.
    * 
    * With the queue model we have one fixed thread, so the added complexity of
    * groupong would seem to buy us nothing.
    */
   void nextSamples(List<ISampleBuffer> buffers) {
      for (ISampleBuffer buffer : buffers) {
         try {
            nextSample(buffer);
         } catch (RuntimeException e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
         } finally {
            buffer.release();
         }
      }
   }
   
   private void nextSample(ISampleBuffer buffer) {
      int count = buffer.getCount();
      short[] data = buffer.getArray();
      int sampleLocSize = findSample(count, data);
      if (sampleLocSize > 0) {
         processSample(sampleLocSize, count, data);
      }
   }

   private int findSample(int count, short[] sample_buf) {
      int currentIndex = 0;
      int arraypos;
      int numfoundsamples = 0;
      if (preambleIsCutOff) {
         ++numfoundsamples;
         sloc[0] = preambleOffset;
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
//               Log.d("Audio Processor", "PREAMBLE CUT OFF");
               if (currentIndex + BEGIN_OFFSET <= count) {
                  preambleOffset = 0;
               } else {
                  preambleOffset = currentIndex + BEGIN_OFFSET - count;
               }
               preambleIsCutOff = true;
               break;
            } else if (numfoundsamples > 0) {
               break;
            } else {
               /* preamble not cut off */
               ++numfoundsamples;
               sloc[0] = currentIndex + BEGIN_OFFSET;
               if (numfoundsamples > 0) {
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
         for (int i = sloc[0]; i < sloc[0] + MOVING_SIZE; i++) {
            movingbuf[i - sloc[0]] = sample_buf[i];
            movingsum[0] += sample_buf[i];
         }
         for (int i = sloc[0] + MOVING_SIZE; i < sloc[0] + SAMPLE_LENGTH - BIT_OFFSET; i++) {
            movingsum[1] = movingsum[0] - movingbuf[arraypos];
            movingsum[1] += sample_buf[i];
            movingbuf[arraypos] = sample_buf[i];
            arraypos++;
            if (arraypos == MOVING_SIZE) {
               arraypos = 0;
            }

            if (isPhase(movingsum[0], movingsum[1], signalMaxSum)) {
               sloc[numsampleloc % SAMPLES_PER_BUFFER] = i - 5;

               samplelocsize = ++numsampleloc;
               if (numsampleloc >=  SAMPLES_PER_BUFFER) {
                  return samplelocsize;
               }
               /* next transmission */
               i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
               sloc[numsampleloc % SAMPLES_PER_BUFFER] = i;
               ++numsampleloc;
               /* next transmission */
               i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
               sloc[numsampleloc % SAMPLES_PER_BUFFER] = i;
               samplelocsize = ++numsampleloc;

               /*
                * finished with this signal, go back to search through next
                * signal
                */
               break;
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
            int slocValue = sloc[j % SAMPLES_PER_BUFFER] + i;
            trans_buf[j][i] = sample_buf[slocValue];
         }
      }
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
      }

      if (sampleLocSize > 1) {
         /*
         * receive byte using best two out of three.
         *
         * Note the we're depending on SAMPLES_PER_BUFFER being 3 here.
         */
         int byte0 = byteInDec[0];
         int byte1 = byteInDec[1];
         int byte2 = byteInDec[2];
         if ((byte0 == byte1 || byte0 == byte2) && byte0 != BOUNDARY && byte0 != 0) {
            AudioProcessorQueue.singleton.processAction(byte0);
         } else if (byte1 == byte2 && byte1 != BOUNDARY && byte1 != 0) {
            AudioProcessorQueue.singleton.processAction(byte1);
         }
      }
   }

   private void autoGainControl(short[] sample_buf) {
      signalMaxSum = 0;
      int arraypos = 0;
      int startpos = sloc[0];
      movingsum[0] = 0;
      for (int i = startpos; i < startpos + MOVING_SIZE; i++) {
         movingbuf[i - startpos] = sample_buf[i];
         movingsum[0] += sample_buf[i];
      }
      int endpos = startpos + PREAMBLE - BEGIN_OFFSET + SAMPLES_PER_BUFFER * (TRANSMISSION_LENGTH + BIT_OFFSET);
      for (int i = startpos + MOVING_SIZE; i < endpos; i++) {
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
