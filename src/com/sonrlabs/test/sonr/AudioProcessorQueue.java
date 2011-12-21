/***************************************************************************
 * Copyright 2011 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 *  Queue requests, process them in sequence in a task.
 */
final class AudioProcessorQueue
      implements Runnable {
   
   static final AudioProcessorQueue singleton = new AudioProcessorQueue(20);
   private final Queue<ISampleBuffer> queuedBuffers = new LinkedList<ISampleBuffer>();
   private final int capacity;
   private final Object lock = "queue-lock";
   private boolean stop;
   
   private AudioProcessorQueue(int capacity) {
      this.capacity = capacity;
      Utils.runTask(this);
   }
   
   void stop() {
      this.stop = true;
   }
   
   boolean push(ISampleBuffer buffer) {
      synchronized (lock) {
         if (queuedBuffers.size() == capacity) {
            android.util.Log.w(getClass().getName(), "Queue capacity exceeded");
            return false;
         } else {
            queuedBuffers.add(buffer);
            lock.notify();
            return true;
         }
      }
   }

   public void run() {
      List<ISampleBuffer> pending = new ArrayList<ISampleBuffer>();
      while (!stop) {
         synchronized (lock) {
            while (queuedBuffers.isEmpty()) {
               try {
                  lock.wait();
               } catch (InterruptedException e) {
                  // keep waiting
               }
            }
            pending.addAll(queuedBuffers);
            queuedBuffers.clear();
         }
         for (ISampleBuffer buffer : pending) {
            // TODO: Pooll these
            new AudioProcessor(buffer).run();
         }
         pending.clear();
      }
   }
}