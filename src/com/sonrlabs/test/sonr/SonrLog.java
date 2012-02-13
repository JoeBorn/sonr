package com.sonrlabs.test.sonr;

import android.util.Log;

/**
 * This class exists to divert the log, filter TAGs, etc.
 *  TODO: add support for release, debug modes
 */
public class SonrLog {

   public static void d(String tag, String message) {
      if (verifyTag(tag)) {
         Log.d(tag, message);
      }
   }
   
   public static void e(String tag, String message) {
      if (verifyTag(tag)) {
         Log.e(tag, message);
      }
   }
   
   public static void i(String tag, String message) {
      if (verifyTag(tag)) {
         Log.i(tag, message);
      }
   }
   
   public static void v(String tag, String message) {
      if (verifyTag(tag)) {
         Log.v(tag, message);
      }
   }
 
   public static void w(String tag, String message) {
      if (verifyTag(tag)) {
         Log.w(tag, message);
      }
   }
 
   static boolean verifyTag(String tag) {
      return true; //(ToggleSONR.class.getSimpleName().equals(tag));
   }
   
}
