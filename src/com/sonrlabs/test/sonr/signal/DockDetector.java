/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 * Questions/Comments: joe@sonrlabs.com
 * 
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/
package com.sonrlabs.test.sonr.signal;



import com.sonrlabs.test.sonr.SonrLog;

/**
 * Sync with the dock.
 * 
 */
class DockDetector
      extends SignalConstructor
      implements IDockDetector {
   
   private static final String TAG = DockDetector.class.getSimpleName();
   
   @Override
   public boolean findDock(short[] samples, int count) {

      int startpos = SAMPLE_LENGTH;
      int sampleStartIndices[] = new int[SAMPLES_PER_BUFFER];
      while (startpos < count - 1 && Math.abs(samples[startpos] - samples[startpos + 1]) < THRESHOLD) {
         //TODO: Log every sample, this should be commented out before production
         //Log.d("DockDetector", Integer.toHexString(samples[startpos]));
         startpos++;
      }

      if (startpos < count - 1 && startpos >= SAMPLE_LENGTH && startpos < SAMPLE_LENGTH * (SAMPLES_PER_BUFFER-1)) {
         startpos -= SAMPLE_LENGTH;
         while (Math.abs(samples[startpos] - samples[startpos + 1]) < THRESHOLD) {
            // && startpos < numSamples-1)
            startpos++;
         }
      }

      startpos += BEGIN_OFFSET;

      if (startpos < count - (SAMPLE_LENGTH - BEGIN_OFFSET)) {
         SonrLog.d(TAG, "Found a sample...");
         computeSignalMax(samples, startpos);
         findSample(startpos, samples, 0, sampleStartIndices);

         constructSignal(samples, sampleStartIndices);

         /* If at least two are BOUND, that's a match. */
         return countBoundSignals() >= 2;
      }

      return false;
   }
}
