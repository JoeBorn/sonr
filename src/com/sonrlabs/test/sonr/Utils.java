package com.sonrlabs.test.sonr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Utils {

   private static final ExecutorService executor = Executors.newFixedThreadPool(4);

   static boolean isPhase(int sum1, int sum2, int max) {
      return Math.abs(sum1 - sum2) > max;
   }

   static void runTask(Runnable task) {
      Utils.executor.execute(task);
   }

}
