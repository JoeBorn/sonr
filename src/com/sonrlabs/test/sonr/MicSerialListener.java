package com.sonrlabs.test.sonr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

import com.sonrlabs.test.sonr.signal.AudioUtils;
import com.sonrlabs.test.sonr.signal.Factory;
import com.sonrlabs.test.sonr.signal.IDockDetector;

class MicSerialListener
      implements Runnable {
   
   private static final String MIC_INPUT_UNAVAIL = "Mic input was unavailable to be read";
   private static final String TAG = "MicSerialListener";
//   private static final long SIGNAL_SEARCH_TIME_MILLIS = 5 * 1150;
   private static final long SIGNAL_SEARCH_TIME_MILLIS = 10 * 1150;

   /*
    * Just one reusable thread since we only start a new one after killing the
    * existing one.
    */
   private static final ExecutorService executor = Executors.newFixedThreadPool(1);
   
   /*
    * One instance shared by each listener, only one of which is active at any
    * given time.  Is this safe?
    */
   private static final IDockDetector dockDetector = Factory.createDockDetector();

   private boolean running;
   private boolean foundDock;
   private int bufferSize;
   private AudioRecord inStream;
   private SampleBufferPool bufferPool;
   private Object searchLock = new Object();

   MicSerialListener() {
      Log.d(TAG, "STARTED");
      
      init();
      
      if (inStream != null) {
         searchSignal();
      } else {
         // LogFile.MakeLog("Failed to initialize AudioRecord");
         Log.d(TAG, "Failed to initialize AudioRecord");
      }
   }

   private void init() {
      inStream = AudioUtils.findAudioRecord(TAG);
      bufferSize = AudioUtils.getAudioBufferSize();
      
      bufferPool = new SampleBufferPool(bufferSize, 2);
      if (inStream != null) {
         // set up recorder thread
         switch (inStream.getState()) {
            case AudioRecord.STATE_INITIALIZED:
               inStream.startRecording();
               break;
            case AudioRecord.STATE_UNINITIALIZED:
               //TODO: if this happens, no point in continuing in the future
            default:
               break;
         }
      }
   }
   
   @Override
   /*
    * Reads in from mic and dispatches an audio rocessor to process the data
    */
   public void run() {
//      synchronized (searchLock) {
         if (inStream != null) {
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
               //ErrorReporter.getInstance().handleException(e);
            }
            Log.d(TAG, "LISTENER ENDED");
         }
//      }
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
            
            switch (inStream.getRecordingState()) {
               case AudioRecord.RECORDSTATE_RECORDING:
                  inStream.stop();
                  break;
               case AudioRecord.RECORDSTATE_STOPPED:
               default:
                  break;
            }
            
            inStream.release();
            inStream = null;
            
            foundDock = false;
         }
      }
      Log.d(TAG, "STOPPED");
   }

   void searchSignal() {
      if (inStream == null) {
         if (!foundDock) {
            init();
         }
         if (inStream == null) {
            return;
         }
      }
      
      ISampleBuffer buffer = bufferPool.getBuffer(bufferSize);
      short[] samples = buffer.getArray();
      long endTime = SystemClock.elapsedRealtime() + SIGNAL_SEARCH_TIME_MILLIS;
      
      boolean problem = false;
      int errorCode = -1;
      
      try {
         synchronized (searchLock) {
            
            while (!foundDock && SystemClock.elapsedRealtime() <= endTime) {
               int count = inStream.read(samples, 0, bufferSize);
               if (count > 0) {
                  foundDock = dockDetector.findDock(samples, count);
               } else {
                  problem = true;
                  errorCode = count;
                  //ErrorReporter.getInstance().putCustomData(TAG, MIC_INPUT_UNAVAIL);
               }
               
            }
            
            if (problem) {
               String errorMsg = null;
               switch (errorCode) {
                  case AudioRecord.ERROR_INVALID_OPERATION:
                     errorMsg = "AudioRecord: the object wasn't properly initialized";
                     break;
                  case AudioRecord.ERROR_BAD_VALUE:
                     errorMsg = "AudioRecord: the parameters don't resolve to valid data and indexes.";
                     break;
                  default:
                     errorMsg = MIC_INPUT_UNAVAIL;
                     break;
               }
               Log.e(TAG, errorMsg);
               //ErrorReporter.getInstance().putCustomData(TAG, errorMsg);
            }
            
         }
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      } finally {
         buffer.release();
      }
   }

   static void startNewListener(MicSerialListener listener) {
      executor.execute(listener);
   }
}
