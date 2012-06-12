/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
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
         buffers.add(new ReusableBuffer());
      }
      /* Return one of the new ones. */
      return buffers.get(nextIndex);
   }
   
   ISampleBuffer getBuffer(int size) {
      synchronized (buffers) {
         ReusableBuffer availableBuffer = null;
         for (ReusableBuffer buffer : buffers) {
            if (buffer.check()) {
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
            availableBuffer.check();
            SonrLog.i(getClass().getName(), "Increased buffer pool size to " + buffers.size());
         }
         availableBuffer.setCount(size);
         return availableBuffer;
      }
   }
   
   
   private class ReusableBuffer
         implements ISampleBuffer {
      private boolean available = true;
      private int count;
      private final short[] array;
      
      ReusableBuffer() {
         array = new short[bufferSize];
      }
      
      @Override
      synchronized public void release() {
         available = true;
      }
      
      @Override
      public short[] getArray() {
         return array;
      }

      @Override
      public int getCount() {
         return count;
      }
      
      @Override
      public void setCount(int count) {
         this.count = count;
      }

      synchronized private boolean check() {
         if (available) {
            available = false;
            return true;
         } else {
            return false;
         }
      }
      
   }
}
