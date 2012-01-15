/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;

/**
 *  Thrown when the audio processor generates a spurious signal.
 */
public class SpuriousSignalException
      extends Exception {

   private static final long serialVersionUID = 1;
   private final int signalCode;
   
   public SpuriousSignalException(int signalCode) {
      this.signalCode = signalCode;
   }

   public int getSignalCode() {
      return signalCode;
   }
}
