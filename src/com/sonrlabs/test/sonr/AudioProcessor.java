package com.sonrlabs.test.sonr;

import java.util.List;

import org.acra.ErrorReporter;

/**
 * Process an ordered collecton of reusable sample buffers.
 * 
 * This is effectively a singleton as it's only instantiated once by the
 * {@link AudioProcessorQueue} singleton.
 * 
 * There are almost certainly one or more bugs buried in this complex code that
 * generate signals when the user didn't do anything on the remote or the dock.
 * The excessive complexity can easily hide such bugs, so any simplifications
 * are worth doing, even if they don't fix any specific issue. The simpler the
 * code, the easier it is to debug and maintain.
 * 
 * The dock itself might also have firmware issues that cause the same symptom,
 * but it's unlikely to be the only cause.
 * 
 * TODO Simplify this code
 * 
 * FIXME find the source(s) of the spurious signal bug.
 */
final class AudioProcessor {
   
   private final TransmissionFinder finder = new TransmissionFinder();
   private final PreambleCheck checker = new PreambleCheck();
   
   /* This funky structure is shared by the checker and the finder. */
   private final int[] sampleStartInidices = new int[AudioSupportConstants.SAMPLES_PER_BUFFER];
   
   void newConnection() {
      finder.newConnection();
   }

   /**
    * Process the next set of buffers. For now do this one at a time.
    * 
    * In principle we could group up to N together where N is some maximum, by
    * increasing the size of the some arrays by a factor of N and making sloc
    * two-dimensional, with N as the first dimension. This is how the code used
    * to look.
    * 
    * Grouping adds complexity but was (maybe) an advantage in the old model
    * where each run was a new thread. Creating and starting threads is a
    * non-trivial expense, and grouping decreases the number of threads.
    * 
    * With the queue model we have one fixed thread, so the added complexity of
    * groupong would seem to buy us nothing.
    */
   void nextSamples(List<ISampleBuffer> buffers) {
      for (ISampleBuffer buffer : buffers) {
         try {
            nextSample(buffer);
         } catch (RuntimeException e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
         } finally {
            buffer.release();
         }
      }
   }
   
   private void nextSample(ISampleBuffer buffer) {
      int count = buffer.getCount();
      short[] data = buffer.getArray();
      int numfoundsamples = checker.countSamples(count, data, sampleStartInidices);
      if (numfoundsamples > 0 && finder.findSamples(data, numfoundsamples, sampleStartInidices)) {
         finder.processSample(count, data, sampleStartInidices);
      }
   }
}
