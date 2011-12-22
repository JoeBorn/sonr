package com.sonrlabs.test.sonr;

import org.acra.ErrorReporter;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

public class MicSerialListener
      implements Runnable, AudioConstants {
   private static final String TAG = "MicSerialListener";

   private static final long SIGNAL_SEARCH_TIME_MILLIS = 1150; // 1.15 seconds

   private boolean running;
   private boolean foundDock;
   private final int bufferSize;
   private final AudioRecord inStream;
   private final SampleBufferPool bufferPool;
   private final SampleSupport sampleSupport = new SampleSupport();
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
   
   SampleSupport getSampleSupport() {
      return sampleSupport;
   }

   @Override
   /*
    * Reads in from mic and dispatches an audio rocessor to process the data
    */
   public void run() {
      running = true;
      ISampleBuffer samples = bufferPool.getBuffer(bufferSize, this);
      try {
         while (running) {
            // Log.d("SONR audio processor", "NEW RECORDING");
            int numSamples = inStream.read(samples.getArray(), 0, bufferSize);
            if (numSamples > 0) {
               /* if there are samples and not waiting */
               samples.setNumberOfSamples(numSamples);
               AudioProcessorQueue.singleton.push(samples);
               samples = bufferPool.getBuffer(bufferSize, this);
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
      ISampleBuffer buffer = bufferPool.getBuffer(bufferSize, this);
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
