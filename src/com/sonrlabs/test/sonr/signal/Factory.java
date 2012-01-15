/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;

/**
 *  Public access to instance construction in this package.
 */
public class Factory {
   
   public static IDockDetector createDockDetector() {
      return new DockDetector();
   }
   
   public static IAudioProcessor createAudioProcessor() {
      return new AudioProcessor();
   }
}
