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

/**
 *  Public access to instance construction in this package.
 */
public class Factory {
   
   public static IDockDetector createDockDetector() {
      return new DockDetector();
   }
   
   public static IAudioProcessor createAudioProcessor() {
      return new AudioProcessor();
   }
}
