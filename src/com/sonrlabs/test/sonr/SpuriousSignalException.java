/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

/**
 *  Thrown when the audio processor generates a spurious signal.
 */
class SpuriousSignalException
      extends Exception {

   private static final long serialVersionUID = 1;
   private final int signalCode;
   private final StringBuilder builder = new StringBuilder();
   
   SpuriousSignalException(int signalCode) {
      this.signalCode = signalCode;
   }
   
   String generateLog(int samplelocsize, short[] samples, int[] sampleStartIndices, int[] signals) {
      builder.append("Spurious signal ").append(signalCode);
      builder.append("Signals:");
      for (int i=0; i<samplelocsize; i++) {
         builder.append(i).append('=').append(signals[i]);
      }
      builder.append("\nIndices:");
      for (int i=0; i<sampleStartIndices.length; i++) {
         builder.append(i).append('=').append(sampleStartIndices[i]).append("->").append(samples[i]);
      }
      return builder.toString();
   }
}
