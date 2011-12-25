/***************************************************************************
 * Copyright 2011 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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
   private final BlockingQueue<ISampleBuffer> queuedBuffers;
   private final Object lock = "queue-lock";
   
   private AudioProcessorQueue(int capacity) {
      super("AudioProcessorQueue");
      setDaemon(true);
      queuedBuffers = new ArrayBlockingQueue<ISampleBuffer>(capacity);
      start();
   }
   
   private boolean push(ISampleBuffer buffer, long timeoutMillis) {
      synchronized (lock) {
         boolean queued = false;
         try {
            queued = queuedBuffers.offer(buffer, timeoutMillis, TimeUnit.MILLISECONDS);
         } catch (InterruptedException e) {
            // treat this as a timeout
         }
         lock.notify();
         return queued;
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

   static boolean addSamples(ISampleBuffer samples, long timeoutMillis) {
      return singleton.push(samples, timeoutMillis);
   }
}
