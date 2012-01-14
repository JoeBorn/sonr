package com.sonrlabs.test.sonr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.acra.ErrorReporter;

import com.sonrlabs.test.sonr.signal.DockDetector;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

class MicSerialListener
      implements Runnable {
   private static final String TAG = "MicSerialListener";

   /*
    * Just one reusable thread since we only start a new one after killing the
    * existing one.
    */
   private static final ExecutorService executor = Executors.newFixedThreadPool(1);
   
   /*
    * One instance shared by each listener, only one of which is active at any
    * given time.  Is this safe?
    */
   private static final DockDetector support = new DockDetector();

   static void  startNewListener(MicSerialListener listener) {
      executor.execute(listener);
   }
   private static final long SIGNAL_SEARCH_TIME_MILLIS = 1150; // 1.15 seconds

   private boolean running;
   private boolean foundDock;
   private final int bufferSize;
   private final AudioRecord inStream;
   private final SampleBufferPool bufferPool;
   private final Object searchLock = new Object();

   MicSerialListener(AudioRecord record, int buffsize) {
      Log.d(TAG, "STARTED");
      inStream = record;
      bufferSize = buffsize;
      bufferPool = new SampleBufferPool(bufferSize, 2);
      if (inStream != null) {
         try {
            // set up recorder thread
            inStream.startRecording();
         } catch (RuntimeException e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
         }
         searchSignal();
      } else {
         // LogFile.MakeLog("Failed to initialize AudioRecord");
         Log.d(TAG, "Failed to initialize AudioRecord");
      }
   }

   @Override
   /*
    * Reads in from mic and dispatches an audio rocessor to process the data
    */
   public void run() {
      running = true;
      ISampleBuffer samples = bufferPool.getBuffer(bufferSize);
      try {
         while (running) {
            // Log.d("SONR audio processor", "NEW RECORDING");
            int count = inStream.read(samples.getArray(), 0, bufferSize);
            if (count > 0) {
               /* if there are samples and not waiting */
               samples.setCount(count);
               if (AudioProcessorQueue.push(samples)) {
                  /*
                   * Grab a new buffer from the pool if the current buffer was
                   * successfully queued. Otherwise reuse it.
                   */
                  samples = bufferPool.getBuffer(bufferSize);
               }
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
   }

   boolean foundDock() {
      return foundDock;
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
      }
      Log.d(TAG, "STOPPED");
   }

   void searchSignal() {
      ISampleBuffer buffer = bufferPool.getBuffer(bufferSize);
      short[] samples = buffer.getArray();
      try {
         long startTime = SystemClock.elapsedRealtime();
         long endTime = startTime + SIGNAL_SEARCH_TIME_MILLIS;
         boolean problem = false;
         synchronized (searchLock) {
            while (inStream != null && !foundDock && SystemClock.elapsedRealtime() <= endTime) {
               int count = inStream.read(samples, 0, bufferSize);
               if (count > 0) {
                  foundDock = support.findDock(samples, count);
               } else {
                  problem = true;
               }
            }
         }
         if (problem) {
            Log.d("MicSerialListener", "Mic input was unavailable to be read");
            ErrorReporter.getInstance().putCustomData("MicSerialListener", "Mic input was unavailable to be read");
         }
      } catch (RuntimeException e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      } finally {
         buffer.release();
      }
   }
}
