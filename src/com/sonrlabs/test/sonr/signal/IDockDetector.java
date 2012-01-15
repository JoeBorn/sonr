/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr.signal;

/**
 * Synchronizes the {@link com.sonrlabs.test.sonr.MicSerialListener} with the dock.
 */

public interface IDockDetector {
   /**
    * @param samples a buffer of audio data read from the microphone.
    * @param count the count of valid data in the buffer.
    * 
    * @return true iff the given audio data is sufficient to sync with the dock.
    */
   public boolean findDock(short[] samples, int count);
}