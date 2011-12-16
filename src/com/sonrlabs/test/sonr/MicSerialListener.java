package com.sonrlabs.test.sonr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.acra.ErrorReporter;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

public class MicSerialListener
      extends Thread {
   private static final String TAG = "MicSerialListener";

   public static final short SERIAL_TRANSMITTER_BAUD = 2400;
   public static final int SAMPLE_RATE = 44100; // In Hz
   public static final int FRAMES_PER_BIT = SAMPLE_RATE / SERIAL_TRANSMITTER_BAUD;
   public static final int TRANSMISSION_LENGTH = FRAMES_PER_BIT * 8;
   public static final int BIT_OFFSET = MicSerialListener.FRAMES_PER_BIT * 2;
   public static final int PREAMBLE = 64 * FRAMES_PER_BIT;
   public static final int SAMPLE_LENGTH = PREAMBLE + 3 * (TRANSMISSION_LENGTH + BIT_OFFSET);
   public static final int AVE_LEN = 9;
   public static final int BEGIN_OFFSET = PREAMBLE - TRANSMISSION_LENGTH - BIT_OFFSET; // allow
   /* phone's internal AGC to stabilize first */
   public static final int END_OFFSET = TRANSMISSION_LENGTH + BIT_OFFSET; // allow
   /* phone's internal AGC to stabilize first */

   // beginning of a sample
   public static final int THRESHOLD = 4000;

   // transmissions in a single nsample
   public static final int MAX_TRANSMISSIONS = 10; // no more than 10

   public static int SIGNAL_MAX_SUM = 0;

   private static long CHECK_TIME = 1150; // 1.15 seconds

   private static final ExecutorService executor = Executors.newFixedThreadPool(1);

   // Serial input catcher
   private AudioRecord inStream;

   /*
    * size of inStream's buffer, set by AudioRecord.getMinBufferSize() in
    * constructor
    */
   private int bufferSize;

   /*
    * condition for run() loop - constructor sets it to true, stopDriver() sets
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
   final int[][] sloc = new int[MicSerialListener.MAX_TRANSMISSIONS][3];
   final int[][] trans_buf = new int[MAX_TRANSMISSIONS * 3][TRANSMISSION_LENGTH + BIT_OFFSET];
   final int[] byteInDec = new int[MicSerialListener.MAX_TRANSMISSIONS * 3];
   
   
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
   private long start_check = 0;

   private SampleBufferPool bufferPool;

   MicSerialListener(AudioRecord theaudiorecord, int buffsize, IUserActionHandler theByteReceiver) {
      super(TAG);
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

               // set up thread
               running = true;
               this.setDaemon(true);
               inStream.startRecording();
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

   void searchSignal() {
      try {
         start_check = SystemClock.elapsedRealtime();
         boolean problem = false;
         while (SystemClock.elapsedRealtime() - start_check < CHECK_TIME && !found_dock) {
            numSamples = inStream.read(sample_buf, 0, bufferSize);
            if (numSamples > 0) {
               found_dock = autoGainControl();
            } else {
               problem = true;
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

   void stopRunning() {
      running = false;
      if (inStream != null) {
         inStream.release();
      }
      inStream = null;
      Log.d(TAG, "STOPPED");
   }

   private void startNextProcessorThread() {
      if (running) {
         ISampleBuffer samples;
         samples = bufferPool.getBuffer(sample_buf);
         AudioProcessor myaudioprocessor = new AudioProcessor(this, numSamples, samples);
         executor.execute(myaudioprocessor);
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

         int[] byteInDec = new int[3];
         for (int n = 0; n < 3; n++) {
            if (sampleloc[n] != 0) {
               arraypos = 0;
               movingsum[0] = 0;
               for (int i = 0; i < 9; i++) {
                  movingbuf[i] = sample_buf[i + sampleloc[n]];
                  movingsum[0] += sample_buf[i + sampleloc[n]];
               }
               for (int i = 9; i < MicSerialListener.TRANSMISSION_LENGTH; i++) {
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

               for (int i = MicSerialListener.FRAMES_PER_BIT + 1; i < MicSerialListener.TRANSMISSION_LENGTH; i++) {
                  if (isPhase(movingsum[i - 1], movingsum[i], SIGNAL_MAX_SUM) && switchphase) {
                     isinphase = !isinphase;
                     switchphase = false; // already switched
                  }

                  if (i % MicSerialListener.FRAMES_PER_BIT == 0) {
                     if (!isinphase) {
                        byteInDec[n] |= 0x1 << bitnum;
                     }
                     bitnum++;
                     switchphase = true; // reached a bit, can now switch
                     // again if phase shifts
                  }
               }

               Log.d(TAG, "TRANSMISSION[" + n + "]: " + "0x" + Integer.toHexString(byteInDec[n]));
               // LogFile.MakeLog("TRANSMISSION[" + n + "]: " + "0x"+
               // Integer.toHexString(byteInDec[n]));
            }
         }

         if (byteInDec[0] == 0x27 && byteInDec[1] == 0x27 || byteInDec[1] == 0x27 && byteInDec[2] == 0x27 || byteInDec[0] == 0x27
               && byteInDec[2] == 0x27) {
            found = true;
         }
      }// end if found a start position

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

      for (int i = startpos + 9; i < startpos + SAMPLE_LENGTH - MicSerialListener.BIT_OFFSET; i++) {
         movingsum[1] = movingsum[0] - movingbuf[arraypos];
         movingsum[1] += sample_buf[i];
         movingbuf[arraypos] = sample_buf[i];
         arraypos++;
         if (arraypos == 9) {
            arraypos = 0;
         }

         if (isPhase(movingsum[0], movingsum[1], SIGNAL_MAX_SUM)) {
            sampleloc[numsampleloc++] = i - 5;
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1; // next
            // transmission
            sampleloc[numsampleloc++] = i;
            i += TRANSMISSION_LENGTH + BIT_OFFSET + FRAMES_PER_BIT + 1; // next
            // transmission
            sampleloc[numsampleloc++] = i;
            return;
         }

         movingsum[0] = movingsum[1];
      }
   }

   static boolean isPhase(int sum1, int sum2, int max) {
      return Math.abs(sum1 - sum2) > max;
   }
}
