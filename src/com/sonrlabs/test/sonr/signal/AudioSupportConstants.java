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
    * Don't really know yet what this magic number is except that it's the size
    * of the movingbuf array.
    */
   static final int MOVING_SIZE = 9;
   
   /**
    * Don't know what this is either, looks like maybe a boundary marker of some
    * kind?
    * This is the "heartbeat" code the app looks for to distinguish the dock from a headphone.
    * The dock emits this twice a second.
    */
   static final byte BOUNDARY = 0x27;
   
   /*is this the right name for this? what buffer has 3 samples? 
    * each keypress signal is repeated three times by the dock microprocessor, ie 
    * 3 copies of data per transmission
    */
   static final int SAMPLES_PER_BUFFER = 3;
   static final short SERIAL_TRANSMITTER_BAUD = 2400;
   static final int SAMPLE_RATE = 44100; // In Hz
   /*are errors introduced by the fact that the real number is 2% more than the int? */
   static final int FRAMES_PER_BIT = SAMPLE_RATE / SERIAL_TRANSMITTER_BAUD;
   /*seems this is actually "byte length" */
   static final int TRANSMISSION_LENGTH = FRAMES_PER_BIT * 8;
   static final int BIT_OFFSET = FRAMES_PER_BIT * 2;
   /* allow phone's internal AGC to stabilize first */
   static final int PREAMBLE = 64 * FRAMES_PER_BIT;
   static final int SAMPLE_LENGTH = PREAMBLE + SAMPLES_PER_BUFFER * (TRANSMISSION_LENGTH + BIT_OFFSET);
   static final int BEGIN_OFFSET = PREAMBLE - TRANSMISSION_LENGTH - BIT_OFFSET;
   static final int END_OFFSET = TRANSMISSION_LENGTH + BIT_OFFSET;
   // beginning of a sample
   /* 500 to 4000 seem to work about the same on Photon Q */
   static final int THRESHOLD = 4000;
   /* divides signal amplitude to determine threshold jump for phase change */
   static final double AMPLITUDE_THRESHOLD = 1.5;
}
