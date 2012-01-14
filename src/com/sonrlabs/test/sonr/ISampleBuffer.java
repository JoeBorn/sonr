package com.sonrlabs.test.sonr;

/**
 *  Reusable sample buffers.
 */
public interface ISampleBuffer {
   
   /**
    * Make the buffer available for reuse.
    */
   public void release();
   
   public int getCount();
   
   public void setCount(int count);
   
   public short[] getArray();
}