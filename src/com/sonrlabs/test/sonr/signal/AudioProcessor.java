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

import java.util.List;

import com.sonrlabs.test.sonr.ISampleBuffer;

/**
 * Process an ordered collection of reusable sample buffers.
 * <p>
 * This is effectively a singleton as it's only instantiated once by the
 * {@link com.sonrlabs.test.sonr.AudioProcessorQueue} singleton.
 */
final class AudioProcessor
      implements AudioSupportConstants, IAudioProcessor {
   
   private final TransmissionPreprocessor preprocessor = new TransmissionPreprocessor();
   
   @Override
   public void nextSamples(List<ISampleBuffer> buffers) {
      for (ISampleBuffer buffer : buffers) {
         try {
            preprocessor.nextSample(buffer);
         } catch (RuntimeException e) {
            e.printStackTrace();
            //ErrorReporter.getInstance().handleException(e);
         } finally {
            buffer.release();
         }
      }
   }
}


