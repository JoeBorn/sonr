package com.sonrlabs.test.sonr;

import java.util.Collection;

import org.acra.ErrorReporter;

final class AudioProcessor {
   private final SampleSupport sampleSupport;

   AudioProcessor(SampleSupport sampleSupport) {
      this.sampleSupport = sampleSupport;
   }

   void processSamples(Collection<ISampleBuffer> sampleBuffers, IUserActionHandler actionHandler) {
      for (ISampleBuffer sampleBuffer : sampleBuffers) {
         try {
            sampleSupport.nextSample(sampleBuffer, actionHandler);
         } catch (RuntimeException e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
         } finally {
            sampleBuffer.release();
         }
      }
   }
}
