package com.sonrlabs.test.sonr;

import org.acra.ErrorReporter;

/**
 * Process one reusable sample buffer.
 * 
 * This is effectively a singleton as it's only instantiated once by the
 * {@link AudioProcessorQueue} singleton.
 */
final class AudioProcessor {
   
   private final AudioProcessorSupport support = new AudioProcessorSupport();
   
   void nextSample(ISampleBuffer sampleBuffer) {
      try {
         support.nextSample(sampleBuffer.getCount(), sampleBuffer.getArray());
      } catch (RuntimeException e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      } finally {
         sampleBuffer.release();
      }
   }
}
