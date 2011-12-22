package com.sonrlabs.test.sonr;

import java.util.ArrayList;
import java.util.List;

import org.acra.ErrorReporter;

import android.util.Log;

class AudioProcessor {

   // private static final String TAG = "SONR audio processor";
   
   private static final int PoolSize = 4;
   private static List<AudioProcessor> Pool = new ArrayList<AudioProcessor>(PoolSize);
   
   static {
      for (int i=0; i<PoolSize; i++) {
         Pool.add(new AudioProcessor());
      }
   }
   
   private boolean inUse;
   
   private AudioProcessor() {
   }
   
   private void go(ISampleBuffer sampleBuffer) {
      inUse = true;
      try {
         /* Log.d(TAG, "AUDIO PROCESSOR BEGIN"); */
         SampleSupport.singleton.nextSample(sampleBuffer.getNumberOfSamples(), sampleBuffer.getArray());
         /* Log.d(TAG, "AUDIO PROCESSOR END"); */
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      } finally {
         sampleBuffer.release();
         inUse = false;
      }
   }

   static void runAudioProcessor(ISampleBuffer samples) {
      AudioProcessor nextProcessor = null;
      for (AudioProcessor processor : Pool) {
         if (!processor.inUse) {
            nextProcessor = processor;
            break;
         }
      }
      if (nextProcessor == null) {
         // pool ran out
         nextProcessor = new AudioProcessor();
         Pool.add(nextProcessor);
         Log.i("AudioProcessor", "Pool size is " + Pool.size());
      }
      
      nextProcessor.go(samples);
   }


}
