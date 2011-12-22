package com.sonrlabs.test.sonr;

import java.util.ArrayList;
import java.util.List;

import org.acra.ErrorReporter;

import android.util.Log;

class AudioProcessor
      implements AudioConstants {

   // private static final String TAG = "SONR audio processor";
   
   private static final int PoolSize = 4;
   private static List<AudioProcessor> Pool = new ArrayList<AudioProcessor>(PoolSize);
   
   static {
      for (int i=0; i<PoolSize; i++) {
         Pool.add(new AudioProcessor());
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
   
   private ISampleBuffer sampleBuffer;
   private short[] sample_buf;
   private int[][] sampleloc = AudioProcessorQueue.singleton.sloc;
   private int numSamples;
   private boolean inUse;
   private SampleSupport sampleSupport;
   
   private AudioProcessor() {
   }
   
   private void init(ISampleBuffer samples) {
      inUse = true;
      this.sampleBuffer = samples;
      numSamples = samples.getNumberOfSamples();
      sample_buf = samples.getArray();
      sampleSupport = samples.getListener().getSampleSupport();
   }

   private void go() {
      try {
         /* Log.d(TAG, "AUDIO PROCESSOR BEGIN"); */
         sampleSupport.nextSample(numSamples, sampleloc, sample_buf);
         /* Log.d(TAG, "AUDIO PROCESSOR END"); */
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      } finally {
         sampleBuffer.release();
         inUse = false;
      }
   }


}
