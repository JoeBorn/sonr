package com.sonrlabs.test.sonr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.acra.ErrorReporter;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

public class MicSerialListener
      implements Runnable {
   
   /* Log id */
   private static final String TAG = "MicSerialListener";
   
   /* Initial size of the buffer pool  */
   private static final int BUFFER_POOL_SIZE = 4;
   
   /* Max time to wait when offering a sample buffer to the queue. */
   private static final int MAX_QUEUE_WAIT_TIME_MILLIS = 200;

   /*  End Dock search after 1.15 seconds */
   private static final long SIGNAL_SEARCH_TIME_MILLIS = 1150;
   
   /* Just one reusable thread. */
   private static final ExecutorService executor = Executors.newFixedThreadPool(1);

   static void  startNewListener(MicSerialListener listener) {
      executor.execute(listener);
   }

   private boolean running;
   private boolean foundDock;
   private final int bufferSize;
   private final AudioRecord inStream;
   private final SampleBufferPool bufferPool;
   private final Object searchLock = new Object();
   private final SampleSupport sampleSupport;

   MicSerialListener(AudioRecord record, int buffsize, SampleSupport sampleSupport) {
      this.sampleSupport = sampleSupport;
      Log.d(TAG, "STARTED");
      inStream = record;
      bufferSize = buffsize;
      bufferPool = new SampleBufferPool(bufferSize, BUFFER_POOL_SIZE);
      if (inStream != null) {
         try {
            // set up recorder thread
            inStream.startRecording();
            searchSignal();
         } catch (RuntimeException e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
         }
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
            int numSamples = inStream.read(samples.getArray(), 0, bufferSize);
            if (numSamples > 0) {
               /* if there are samples and not waiting */
               samples.setNumberOfSamples(numSamples);
               if (AudioProcessorQueue.addSamples(samples, MAX_QUEUE_WAIT_TIME_MILLIS)) {
                  samples = bufferPool.getBuffer(bufferSize);
               } else {
                  // Queue full!  Drop this one.
                  Log.e(TAG, "Dropping samples, queue full");
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

   private void searchSignal() {
      ISampleBuffer buffer = bufferPool.getBuffer(bufferSize);
      short[] samples = buffer.getArray();
      try {
         long startTime = SystemClock.elapsedRealtime();
         long endTime = startTime + SIGNAL_SEARCH_TIME_MILLIS;
         boolean problem = false;
         synchronized (searchLock) {
            while (inStream != null && !foundDock && SystemClock.elapsedRealtime() <= endTime) {
               int numSamples = inStream.read(samples, 0, bufferSize);
               if (numSamples > 0) {
                  foundDock = sampleSupport.autoGainControl(samples, numSamples);
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
      } finally {
         buffer.release();
      }
   }
}
