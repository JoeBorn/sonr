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


import com.sonrlabs.test.sonr.ISampleBuffer;

final class TransmissionFinder
      extends SignalConstructor {
   
   void nextSample(ISampleBuffer buffer, int sampleCount, int[] sampleStartIndices) {
      short[] samples = buffer.getArray();
      int startpos = sampleStartIndices[0];
      computeSignalMax(samples, startpos);
      int samplelocsize = 0;
      for (int n = 0; n < sampleCount; n++) {
         samplelocsize = findSample(startpos, samples, samplelocsize, sampleStartIndices);
      }
      if (samplelocsize >= 2) {
         processSample(samples, sampleStartIndices);
      }
   }

   private void processSample(short[] samples, int[] sampleStartIndices) {
      constructSignal(samples, sampleStartIndices);
      processSignalIfMatch();
   }
}