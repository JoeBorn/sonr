package com.sonrlabs.test.sonr;

import org.acra.ErrorReporter;

final class AudioProcessor {
   private static final AudioProcessor singleton = new AudioProcessor();
   
   private AudioProcessor() {
   }
   
   private void go(ISampleBuffer sampleBuffer) {
      try {
         SampleSupport.singleton.nextSample(sampleBuffer.getNumberOfSamples(), sampleBuffer.getArray());
      } catch (RuntimeException e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      } finally {
         sampleBuffer.release();
      }
   }

   static void runAudioProcessor(ISampleBuffer buffer) {
      singleton.go(buffer);
   }
}
