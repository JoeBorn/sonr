package com.sonrlabs.test.sonr;


import org.acra.ErrorReporter;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

public class MicSerialListener
      implements Runnable, AudioConstants {
   private static final String TAG = "MicSerialListener";

   private static final long CHECK_TIME = 1150; // 1.15 seconds
   
   /* Not final!  This can be set in AudioProcessor!  Yow!!*/
   static int SIGNAL_MAX_SUM = 0;

   // Serial input catcher
   private AudioRecord inStream;

   /*
    * size of inStream's buffer, set by AudioRecord.getMinBufferSize() in
    * constructor
    */
   private int bufferSize;

   /*
    * condition for run() loop - constructor sets it to true, stopRunning() sets
    * to false
    */
   private boolean running;

   private IUserActionHandler actionHandler;

   private int numSamples;

   /*
    * Right now this is only used by this class. Previously it was shared
    * with AudioProcessor, but that introduced false-positive artifacts. For now
    * we copy into a pooled buffer and pass that to AudioProcessor. This
    * can lead to false negatives (missed clicks) due to incomplete samples.
    */
   private short sample_buf[];
   
   /*
    * These buffers are only used in AudioProcessor, not here. They're created
    * here because we only want a single copy, not a copy per AudioProcessor
    */
   final int[][] sloc = new int[MAX_TRANSMISSIONS][3];
   final int[][] trans_buf = new int[MAX_TRANSMISSIONS * 3][TRANSMISSION_LENGTH + BIT_OFFSET];
   final int[] byteInDec = new int[MAX_TRANSMISSIONS * 3];
   
   
   /* These buffers are assigned values but never accessed! Disable them for now */
//   private int[] test_buf = new int[SAMPLE_LENGTH];
//   private int[] movingsum2 = new int[PREAMBLE + 3 * (TRANSMISSION_LENGTH + BIT_OFFSET) + 1];
   
   /*
    * These two are only set during AutoGainControl, which should only be
    * happening during initial configuration after Dock connections. They are
    * shared with the AudioProcessor.
    */
   final int[] movingsum = new int[TRANSMISSION_LENGTH];
   final int[] movingbuf = new int[9];
   
   private int sampleloc[] = new int[3];
   private boolean found_dock = false;
   private SampleBufferPool bufferPool;
   private final Object searchLock = new Object();

   MicSerialListener(AudioRecord theaudiorecord, int buffsize, IUserActionHandler theByteReceiver) {
      try {
         if (inStream == null) {
            /* screen turned sideways, dont re-initialize to null */
            actionHandler = theByteReceiver;

            Log.d(TAG, "STARTED");

            inStream = theaudiorecord;
            bufferSize = buffsize;
            if (inStream != null) {
               bufferPool = new SampleBufferPool(bufferSize, 2);
               sample_buf = new short[bufferSize];
               // set up recorder thread
               inStream.startRecording();
               
               /*
                * This variant looks for a signal in a separate task. Not
                * working reliably yet.
                */
//               Runnable searcher = new Runnable() {
//                  public void run() {
//                     searchSignal();
//                  }
//               };
//               Utils.runTask(searcher);
               
               /*
                * This variant looks for a signal synchronously, blocking the
                * caller's thread. Safer in general but could cause some early
                * clicks to be missed.
                */
               searchSignal();
            } else {
               // LogFile.MakeLog("Failed to initialize AudioRecord");
               Log.d(TAG, "Failed to initialize AudioRecord");
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }

   @Override
   /*
    * Reads in from mic and dispatches an audio rocessor to process the data
    */
   public void run() {
      running = true;
      try {
         while (running) {
            // Log.d("SONR audio processor", "NEW RECORDING");
            numSamples = inStream.read(sample_buf, 0, bufferSize);
            if (numSamples > 0) {
               /* if there are samples and not waiting */
               startNextProcessorThread();
            }
            try {
               Thread.sleep(100);
            } catch (InterruptedException e) {
               // wake up early, no big deal.
            }
         }
      } catch (RuntimeException e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
      Log.d("MicSerialListener", "LISTENER ENDED");
      // LogFile.MakeLog("LISTENER ended");
   }

   void processAction(int actionCode) {
      actionHandler.processAction(actionCode);
   }

   boolean foundDock() {
      return found_dock;
   }

   boolean isAlive() {
      return running;
   }
   
   void stopRunning() {
      running = false;
      synchronized (searchLock) {
         if (inStream != null) {
            inStream.release();
         }
         inStream = null;
      }
      Log.d(TAG, "STOPPED");
   }

   private void searchSignal() {
      try {
         long startTime = SystemClock.elapsedRealtime();
         long endTime = startTime + CHECK_TIME;
         boolean problem = false;
         synchronized (searchLock) {
            while (inStream != null && !found_dock && SystemClock.elapsedRealtime() <= endTime) {
               numSamples = inStream.read(sample_buf, 0, bufferSize);
               if (numSamples > 0) {
                  found_dock = autoGainControl();
               } else {
                  problem = true;
               }
            }
         }
         if (problem) {
            Log.d("MicSerialListener", "Mic input was unavailable to be read");
            ErrorReporter.getInstance().putCustomData("MicSerialListener", "Mic input was unavailable to be read");
         }
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }

   private void startNextProcessorThread() {
      if (running) {
         ISampleBuffer samples = bufferPool.getBuffer(sample_buf, numSamples, this);
         AudioProcessorQueue.singleton.push(samples);
//         AudioProcessor myaudioprocessor = new AudioProcessor(samples);
//         Utils.runTask(myaudioprocessor);
      }
   }

   private boolean autoGainControl() {
      boolean found = false;
      int startpos = SAMPLE_LENGTH;
      int arraypos = 0;

      while (startpos < numSamples - 1 && Math.abs(sample_buf[startpos] - sample_buf[startpos + 1]) < THRESHOLD) {
         startpos++;
      }

      if (startpos < numSamples - 1 && startpos >= SAMPLE_LENGTH && startpos < SAMPLE_LENGTH * 2) {
         startpos -= SAMPLE_LENGTH;
         while (Math.abs(sample_buf[startpos] - sample_buf[startpos + 1]) < THRESHOLD) {
            // && startpos < numSamples-1)
            startpos++;
         }
      }

      startpos += BEGIN_OFFSET;

      if (startpos < numSamples - (SAMPLE_LENGTH - BEGIN_OFFSET)) {
         Log.d(TAG, "Found a sample...");

         movingsum[0] = 0;
         for (int i = startpos; i < startpos + 9; i++) {
            movingbuf[i - startpos] = sample_buf[i];
            movingsum[0] += sample_buf[i];
         }
         SIGNAL_MAX_SUM = 0;
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

//            test_buf[i - startpos - 9] = sample_buf1[i];
//            movingsum2[i - startpos - 9] = movingsum[0];
            movingsum[0] = movingsum[1];
         }

         SIGNAL_MAX_SUM /= 1.375;
         findSample(startpos);

         int[] triple = new int[3];
         for (int n = 0; n < 3; n++) {
            if (sampleloc[n] != 0) {
               arraypos = 0;
               movingsum[0] = 0;
               for (int i = 0; i < 9; i++) {
                  movingbuf[i] = sample_buf[i + sampleloc[n]];
                  movingsum[0] += sample_buf[i + sampleloc[n]];
               }
               for (int i = 9; i < TRANSMISSION_LENGTH; i++) {
                  movingsum[i] = movingsum[i - 1] - movingbuf[arraypos];
                  movingsum[i] += sample_buf[i + sampleloc[n]];
                  movingbuf[arraypos] = sample_buf[i + sampleloc[n]];
                  arraypos++;
                  if (arraypos == 9) {
                     arraypos = 0;
                  }
               }

               boolean isinphase = true, switchphase = true;
               /* we start out with a phase shift */
               int bitnum = 0;

               for (int i = FRAMES_PER_BIT + 1; i < TRANSMISSION_LENGTH; i++) {
                  if (Utils.isPhase(movingsum[i - 1], movingsum[i], SIGNAL_MAX_SUM) && switchphase) {
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

   private void findSample(int startpos) {
      int arraypos = 0;
      int numsampleloc = 0;
      movingsum[0] = 0;
      for (int i = startpos; i < startpos + 9; i++) {
         movingbuf[i - startpos] = sample_buf[i];
         movingsum[0] += sample_buf[i];
      }

      for (int i = startpos + 9; i < startpos + SAMPLE_LENGTH - BIT_OFFSET; i++) {
         movingsum[1] = movingsum[0] - movingbuf[arraypos];
         movingsum[1] += sample_buf[i];
         movingbuf[arraypos] = sample_buf[i];
         arraypos++;
         if (arraypos == 9) {
            arraypos = 0;
         }

         if (Utils.isPhase(movingsum[0], movingsum[1], SIGNAL_MAX_SUM)) {
            sampleloc[numsampleloc++] = i - 5;
            // next transmission
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1; 
            sampleloc[numsampleloc++] = i;
            // next transmission
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1;
            sampleloc[numsampleloc++] = i;
            return;
         }

         movingsum[0] = movingsum[1];
      }
   }
}
