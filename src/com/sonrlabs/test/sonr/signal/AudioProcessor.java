package com.sonrlabs.test.sonr.signal;

import java.util.List;

import com.sonrlabs.test.sonr.ISampleBuffer;

/**
 * Process an ordered collection of reusable sample buffers.
 * <p>
 * This is effectively a singleton as it's only instantiated once by the
 * {@link com.sonrlabs.test.sonr.AudioProcessorQueue} singleton.
 */
final class AudioProcessor
      implements AudioSupportConstants, IAudioProcessor {
   
   private final TransmissionPreprocessor preprocessor = new TransmissionPreprocessor();
   
   @Override
   public void nextSamples(List<ISampleBuffer> buffers) {
      for (ISampleBuffer buffer : buffers) {
         try {
            preprocessor.nextSample(buffer);
         } catch (RuntimeException e) {
            e.printStackTrace();
            //ErrorReporter.getInstance().handleException(e);
         } finally {
            buffer.release();
         }
      }
   }
}


