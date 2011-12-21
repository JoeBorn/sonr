package com.sonrlabs.test.sonr;

/**
 *  Reusable sample buffers.
 */
interface ISampleBuffer {
   
   /**
    * Make the buffer available for reuse.
    */
   public void release();
   
   public int getNumberOfSamples();
   
   public MicSerialListener getListener();
   
   /**
    * @return the underlying array.
    */
   public short[] getArray();
}