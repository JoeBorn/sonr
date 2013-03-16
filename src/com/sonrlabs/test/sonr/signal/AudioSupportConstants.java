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

interface AudioSupportConstants {
   /**
    * This is the "heartbeat" code the app looks for to distinguish the dock from a headphone.
    * The dock emits this twice a second.
    */
   static final byte BOUNDARY = 0x19;
   
   static final int TRANSMISSIONS_PER_BUFFER = 3;
   static final short SERIAL_TRANSMITTER_BAUD = 2400;
   static final int SAMPLE_RATE = 44100; // In Hz
   static final int SAMPLES_PER_BIT = SAMPLE_RATE / SERIAL_TRANSMITTER_BAUD;
   static final int PREAMBLE_DELTA = 1000; //delta between two contiguous samples
}
