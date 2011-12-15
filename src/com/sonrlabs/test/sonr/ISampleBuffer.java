package com.sonrlabs.test.sonr;

/**
 *  Reusable sample buffers.
 */
interface ISampleBuffer {
   
   /**
    * Make the buffer available for reuse.
    */
   public void release();
   
   /**
    * @return the underlying array.
    */
   public short[] getArray();
}