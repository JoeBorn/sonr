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
    */
   static final byte BOUNDARY = 0x27;
   
   /*is this the right name for this? what buffer has 3 samples? */
   static final int SAMPLES_PER_BUFFER = 3;
   static final short SERIAL_TRANSMITTER_BAUD = 2400;
   static final int SAMPLE_RATE = 44100; // In Hz
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
   static final int THRESHOLD = 4000;
}
