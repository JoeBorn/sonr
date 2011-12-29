package com.sonrlabs.test.sonr;

import org.acra.ErrorReporter;

final class AudioProcessor {
   
   private final AudioProcessorSupport support = new AudioProcessorSupport();
   
   void nextSample(ISampleBuffer sampleBuffer) {
      try {
         support.nextSample(sampleBuffer.getNumberOfSamples(), sampleBuffer.getArray());
      } catch (RuntimeException e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      } finally {
         sampleBuffer.release();
      }
   }
}
