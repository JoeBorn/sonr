/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

import com.sonrlabs.test.sonr.signal.AudioUtils;
import com.sonrlabs.test.sonr.signal.Factory;
import com.sonrlabs.test.sonr.signal.IDockDetector;
import com.sonrlabs.v96.sonr.R;

class MicSerialListener implements Runnable {
   
   static final String MIC_INPUT_UNAVAIL = "Mic input was unavailable to be read";
   static final String TAG = MicSerialListener.class.getSimpleName();
// static final long SIGNAL_SEARCH_TIME_MILLIS = 5 * 1150;
   static final long SIGNAL_SEARCH_TIME_MILLIS = 10 * 1150;

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
   private Context appContext;
   private boolean running;
   private boolean foundDock;
   private int bufferSize;
   private AudioRecord inStream;
   private SampleBufferPool bufferPool;
   private final Object searchLock = new Object();

   MicSerialListener(Context context) {
      this.appContext = context;
      init();
      if (inStream != null) {
         searchSignal();
      } else {
         SonrLog.d(TAG, "Failed to initialize AudioRecord");
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
               SonrLog.d(TAG, "AudioRecord.startRecording()");
               break;
            case AudioRecord.STATE_UNINITIALIZED:
               //TODO: if this happens, no point in continuing in the future
            default:
               break;
         }
      }
   }
   
   /**
    * Reads in from mic and dispatches an audio processor to process the data
    */
   @Override
   public void run() {
      android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
      if (inStream != null) {
         running = true;
         ISampleBuffer samples = bufferPool.getBuffer(bufferSize);
         
         try {
            while (running) {
               // Log.d("SONR audio processor", "NEW RECORDING");
               int count = inStream.read(samples.getArray(), 0, bufferSize);
               if (count > 0) {
                  samples.setCount(count);
                  AudioProcessorQueue.push(samples);
                  samples = bufferPool.getBuffer(bufferSize);
               }
            }
         } catch (RuntimeException e) {
            e.printStackTrace();
            //ErrorReporter.getInstance().handleException(e);
         }
         Log.d(TAG, "LISTENER ENDED");
      }
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

   private void searchSignal() {
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
               }
            }
            if (foundDock) {
               //FIXME: reuse PLAY_PAUSE message, so processing of parse server
               //       data does not need to change
               Context applicationContext = appContext.getApplicationContext();
               String playPause = appContext.getString(R.string.PLAY_PAUSE);
               SonrAppInformationLogger logger = new SonrAppInformationLogger();
               logger.uploadErrorAppInformationWithErrorString(applicationContext, playPause);
            }
            /*for(int i = 0; i < bufferSize; i++)
            {
               Log.d("MicSerialListener", Integer.toHexString(samples[i]));
            }*/
            
            if (problem) {
               String errorMsg;
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
               SonrLog.e(TAG, errorMsg);
              
               //SonrLog.e ErrorReporter.getInstance().putCustomData(TAG, errorMsg);
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
