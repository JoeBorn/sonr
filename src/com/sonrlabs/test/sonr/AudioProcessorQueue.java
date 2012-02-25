/***************************************************************************
 * Copyright 2011 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.sonrlabs.test.sonr.signal.Factory;
import com.sonrlabs.test.sonr.signal.IAudioProcessor;

/**
 *  Queue requests, process them in sequence in a task.
 */
public final class AudioProcessorQueue extends Thread {
   
   private static final String TAG = "AudioProcessorQueue";
   private static final AudioProcessorQueue singleton = new AudioProcessorQueue();
   
   static void push(ISampleBuffer buffer) {
      singleton.offer(buffer);
   }
   
   static void setUserActionHandler(UserActionHandler handler) {
      singleton.actionHandler = handler;
   }
   
   public static void processAction(int actionCode) {
      singleton.handleAction(actionCode);
   }
   
   private final IAudioProcessor processor = Factory.createAudioProcessor();
   private UserActionHandler actionHandler;
   
   private final Queue<ISampleBuffer> queuedBuffers = new LinkedList<ISampleBuffer>();
   private final Object lock = "queue-lock";
   
   private AudioProcessorQueue() {
      super(TAG);
      setDaemon(true);
      start();
   }
   
   private void handleAction(int actionCode) {
      if (Thread.currentThread() != this) {
         SonrLog.e(TAG, "Actions must be handled in the " + TAG+ " thread");
         return;
      }
      if (actionHandler != null) {
         actionHandler.processAction(actionCode);
      }
   }
   
   private void offer(ISampleBuffer buffer) {
      synchronized (lock) {
         queuedBuffers.add(buffer);
         lock.notify();
      }
   }

   @Override
   public void run() {
      List<ISampleBuffer> pending = new ArrayList<ISampleBuffer>();
      // Loop forever, suppress IDEA warning:
      //noinspection InfiniteLoopStatement
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
         processor.nextSamples(pending);
         pending.clear();
      }
   }
}
