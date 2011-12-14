/***************************************************************************
 *
 * <rrl>
 * =========================================================================
 *                                  LEGEND
 *
 * Use, duplication, or disclosure by the Government is as set forth in the
 * Rights in technical data noncommercial items clause DFAR 252.227-7013 and
 * Rights in noncommercial computer software and noncommercial computer
 * software documentation clause DFAR 252.227-7014, with the exception of
 * third party software known as Sun Microsystems' Java Runtime Environment
 * (JRE), Quest Software's JClass, Oracle's JDBC, and JGoodies which are
 * separately governed under their commercial licenses.  Refer to the
 * license directory for information regarding the open source packages used
 * by this software.
 *
 * Copyright 2011 by BBN Technologies Corporation.
 * =========================================================================
 * </rrl>
 *
 **************************************************************************/

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
         buffers.add(new ReusableBuffer(this.bufferSize));
      }
      /* Return one of the new ones. */
      return buffers.get(nextIndex);
   }
   
   ISampleBuffer getBuffer(short[] source) {
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
         return availableBuffer;
      }
   }
   
   
   private static class ReusableBuffer
         implements ISampleBuffer {
      private boolean available = true;
      private final short[] array;
      
      ReusableBuffer(int bufferSize) {
         array = new short[bufferSize];
      }
      
      synchronized public void release() {
         available = true;
      }
      
      public short[] getArray() {
         return array;
      }

      synchronized private boolean check(short[] source) {
         if (available) {
            System.arraycopy(source, 0, array, 0, source.length);
            available = false;
            return true;
         }
         return false;
      }
      
   }
}
