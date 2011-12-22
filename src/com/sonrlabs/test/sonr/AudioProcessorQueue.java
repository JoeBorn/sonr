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
      extends Thread
      implements AudioConstants {
   
   static final AudioProcessorQueue singleton = new AudioProcessorQueue(20);
   
   /*
    * These buffers are only used in AudioProcessor, not here. They're created
    * here because we only want a single copy, not a copy per AudioProcessor
    */
   final int[][] sloc = new int[MAX_TRANSMISSIONS][3];
   final int[][] trans_buf = new int[MAX_TRANSMISSIONS * 3][TRANSMISSION_LENGTH + BIT_OFFSET];
   final int[] byteInDec = new int[MAX_TRANSMISSIONS * 3];

   private IUserActionHandler actionHandler;
   
   private final Queue<ISampleBuffer> queuedBuffers = new LinkedList<ISampleBuffer>();
   private final int capacity;
   private final Object lock = "queue-lock";
   
   private AudioProcessorQueue(int capacity) {
      this.capacity = capacity;
      Utils.runTask(this);
   }
   
   void setUserActionHandler(IUserActionHandler handler) {
      actionHandler = handler;
   }
   
   void processAction(int actionCode) {
      if (actionHandler != null) {
         actionHandler.processAction(actionCode);
      }
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

   @Override
   public void run() {
      List<ISampleBuffer> pending = new ArrayList<ISampleBuffer>();
      while (true) {
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
             AudioProcessor.runAudioProcessor(buffer);
         }
         pending.clear();
      }
   }
}
