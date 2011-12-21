package com.sonrlabs.test.sonr;

import java.util.ArrayList;
import java.util.List;

/**
 *  Pool of reusable buffers for sample arrays
 */
class SampleBufferPool {
   private final int bufferSize;
   private final List<ReusableBuffer> buffers;
   private final int incrementSize;

   SampleBufferPool(int bufferSize, int poolSize) {
      this.bufferSize = bufferSize;
      this.incrementSize = poolSize;
      this.buffers = new ArrayList<ReusableBuffer>(poolSize);
      incrementPool();
   }

   private ReusableBuffer incrementPool() {
      int nextIndex = buffers.size();
      for (int i=0; i<incrementSize; i++) {
         buffers.add(new ReusableBuffer());
      }
      /* Return one of the new ones. */
      return buffers.get(nextIndex);
   }
   
   ISampleBuffer getBuffer(short[] source, int numberOfSamples, MicSerialListener listener) {
      synchronized (buffers) {
         ReusableBuffer availableBuffer = null;
         for (ReusableBuffer buffer : buffers) {
            if (buffer.check(source)) {
               availableBuffer = buffer;
               break;
            }
         }
         if (availableBuffer == null) {
            /*
             * Ran out, make a new one. Really shouldn't happen. Get the first
             * newly added one, use the first newly added one.
             */
            availableBuffer = incrementPool();
            availableBuffer.check(source);
            android.util.Log.i(getClass().getName(), "Increased buffer pool size to " + buffers.size());
         }
         availableBuffer.numberOfSamples = numberOfSamples;
         availableBuffer.listener = listener;
         return availableBuffer;
      }
   }
   
   
   private class ReusableBuffer
         implements ISampleBuffer {
      private boolean available = true;
      private int numberOfSamples;
      private MicSerialListener listener;
      private final short[] array;
      
      ReusableBuffer() {
         array = new short[bufferSize];
      }
      
      synchronized public void release() {
         available = true;
      }
      
      public short[] getArray() {
         return array;
      }

      public int getNumberOfSamples() {
         return numberOfSamples;
      }

      public MicSerialListener getListener() {
         return listener;
      }

      synchronized private boolean check(short[] source) {
         if (available) {
            System.arraycopy(source, 0, array, 0, bufferSize);
            available = false;
            return true;
         }
         return false;
      }
      
   }
}
