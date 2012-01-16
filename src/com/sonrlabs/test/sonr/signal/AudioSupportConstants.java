package com.sonrlabs.test.sonr.signal;

interface AudioSupportConstants {
   /**
    * Don't really know yet what this magic number is except that it's the size
    * of the movingbuf array.
    */
   public static final int MOVING_SIZE = 9;
   
   /**
    * Don't know what this is either, looks like maybe a boundary marker of some
    * kind?
    */
   public static final byte BOUNDARY = 0x27;
   
   public static final int SAMPLES_PER_BUFFER = 3;
   /*is this the right name for this? what buffer has 3 samples? */
   public static final short SERIAL_TRANSMITTER_BAUD = 2400;
   public static final int SAMPLE_RATE = 44100; // In Hz
   public static final int FRAMES_PER_BIT = SAMPLE_RATE / SERIAL_TRANSMITTER_BAUD;
   public static final int TRANSMISSION_LENGTH = FRAMES_PER_BIT * 8;
   /*seems this is actually "byte length" */
   public static final int BIT_OFFSET = FRAMES_PER_BIT * 2;
   public static final int PREAMBLE = 64 * FRAMES_PER_BIT;
   /* allow phone's internal AGC to stabilize first */
   public static final int SAMPLE_LENGTH = PREAMBLE + SAMPLES_PER_BUFFER * (TRANSMISSION_LENGTH + BIT_OFFSET);
   public static final int BEGIN_OFFSET = PREAMBLE - TRANSMISSION_LENGTH - BIT_OFFSET;
   public static final int END_OFFSET = TRANSMISSION_LENGTH + BIT_OFFSET;
   // beginning of a sample
   public static final int THRESHOLD = 4000;
}
