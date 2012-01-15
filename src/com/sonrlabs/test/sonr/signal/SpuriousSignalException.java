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
   private final String message;
   
   public SpuriousSignalException(int signalCode) {
      this.signalCode = signalCode;
      this.message = "Spurious signal " + Integer.toBinaryString(signalCode) + " [0x" + Integer.toHexString(signalCode) + "]";
   }

   @Override
   public String getMessage() {
      return message;
   }

   public int getSignalCode() {
      return signalCode;
   }
   
}
