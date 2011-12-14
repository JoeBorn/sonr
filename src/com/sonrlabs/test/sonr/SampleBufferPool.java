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

   SampleBufferPool(int bufferSize, int poolSize) {
      this.bufferSize = bufferSize;
      this.buffers = new ArrayList<ReusableBuffer>(poolSize);
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
            /* Make a new one */
            availableBuffer = new ReusableBuffer(bufferSize);
            availableBuffer.check(source);
            buffers.add(availableBuffer);
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
