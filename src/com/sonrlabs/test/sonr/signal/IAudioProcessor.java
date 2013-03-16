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
 *  Linkage to signal processing from the {@link com.sonrlabs.test.sonr.AudioProcessorQueue}.
 */
public interface IAudioProcessor {

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
    * grouping would seem to buy us nothing.
    * 
    * @param buffers the buffers of audio data, to be processed in sequence.
    */
   public abstract void nextSamples(List<ISampleBuffer> buffers);
}
