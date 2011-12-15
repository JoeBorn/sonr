package com.sonrlabs.test.sonr;

/**
 *  API for audio processor
 */
interface IAudioProcessor
      extends Runnable {
   boolean isBusy();
   boolean isWaiting();
}
