package com.sonrlabs.test.sonr;


/**
 *  Null-object pattern.
 */
class NullAudioProcessor
      implements IAudioProcessor {

   public void run() {
   }

   public boolean isBusy() {
      return false;
   }

   public boolean isWaiting() {
      return false;
   }

}
