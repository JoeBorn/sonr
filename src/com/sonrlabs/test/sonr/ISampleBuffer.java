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
   
   public void setNumberOfSamples(int numberOfSamples);
   
   public short[] getArray();
}