/***************************************************************************
 * Copyright 2011 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

/**
 * Just what the name says.
 */
public interface AudioConstants {

   public static final short SERIAL_TRANSMITTER_BAUD = 2400;
   public static final int SAMPLE_RATE = 44100; // In Hz
   public static final int FRAMES_PER_BIT = SAMPLE_RATE / SERIAL_TRANSMITTER_BAUD;
   public static final int TRANSMISSION_LENGTH = FRAMES_PER_BIT * 8;
   public static final int BIT_OFFSET = FRAMES_PER_BIT * 2;
   public static final int PREAMBLE = 64 * FRAMES_PER_BIT;
   public static final int SAMPLE_LENGTH = PREAMBLE + 3 * (TRANSMISSION_LENGTH + BIT_OFFSET);
   public static final int AVE_LEN = 9;
   /* allow phone's internal AGC to stabilize first */
   public static final int BEGIN_OFFSET = PREAMBLE - TRANSMISSION_LENGTH - BIT_OFFSET;
   public static final int END_OFFSET = TRANSMISSION_LENGTH + BIT_OFFSET;
   // beginning of a sample
   public static final int THRESHOLD = 4000;
   // transmissions in a single nsample
   public static final int MAX_TRANSMISSIONS = 10;

}
