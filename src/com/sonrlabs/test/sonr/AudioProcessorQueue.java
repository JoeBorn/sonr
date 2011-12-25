/***************************************************************************
 * Copyright 2011 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.content.Context;
import android.media.AudioManager;

/**
 *  Queue requests, process them in sequence in a task.
 *  Strict static singleton thread.
 */
final class AudioProcessorQueue
      extends Thread {
   
   private static final AudioProcessorQueue singleton = new AudioProcessorQueue(20);

   private UserActionHandler actionHandler;
   private SampleSupport sampleSupport;
   private AudioProcessor processor = new AudioProcessor(sampleSupport);
   private final Queue<ISampleBuffer> queuedBuffers = new LinkedList<ISampleBuffer>();
   private final int capacity;
   private final Object lock = "queue-lock";
   
   private AudioProcessorQueue(int capacity) {
      super("AudioProcessorQueue");
      setDaemon(true);
      this.capacity = capacity;
      start();
   }
   
   private boolean push(ISampleBuffer buffer) {
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
         processor.processSamples(pending, actionHandler);
         pending.clear();
      }
   }

   static void init(AudioManager theAudioManager, Context ctx, SampleSupport sampleSupport) {
       singleton.actionHandler = new UserActionHandler(theAudioManager,ctx);
       singleton.sampleSupport = sampleSupport;
       singleton.processor = new AudioProcessor(sampleSupport);
   }

   static void addSamples(ISampleBuffer samples) {
      singleton.push(samples);
   }
}
