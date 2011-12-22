package com.sonrlabs.test.sonr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Utils {

   private static final ExecutorService executor = Executors.newFixedThreadPool(4);

   static void runTask(Runnable task) {
      Utils.executor.execute(task);
   }

}
