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
   
   private ISampleBuffer sampleBuffer;
   private short[] sample_buf;
   private int numSamples;
   private boolean inUse;
   
   private AudioProcessor() {
   }
   
   private void init(ISampleBuffer samples) {
      inUse = true;
      sampleBuffer = samples;
      numSamples = samples.getNumberOfSamples();
      sample_buf = samples.getArray();
   }

   private void go() {
      try {
         /* Log.d(TAG, "AUDIO PROCESSOR BEGIN"); */
         SampleSupport.singleton.nextSample(numSamples, sample_buf);
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
      
      nextProcessor.init(samples);
      nextProcessor.go();
   }


}
