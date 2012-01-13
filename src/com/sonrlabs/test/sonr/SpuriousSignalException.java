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
   
   SpuriousSignalException(int signalCode) {
      this.signalCode = signalCode;
   }

   int getSignalCode() {
      return signalCode;
   }
}
