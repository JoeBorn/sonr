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
 * 
 * This class provides support for audio processing functionality that's
 * common to both the initial connection to the dock and the processing of
 * signals once that connection is established.
 *
 * signal format,
 * [ 64 bit preamble ] + [ 3 copies of data ], where data is,
 * [1 start bit ] + [ 8 data bits ] + [ 1 stop bit ] + [ 1 filler bit ]
 * bitrate: 2400Hz
 * carrier: 9600Hz
 * 
 * <p>
 *  Sonr uses differential phase shift keying for modulation and HDLC Framing.
 *  
 * @see <a href="http://en.wikipedia.org/wiki/Phase-shift_keying#Differential_phase-shift_keying_ .28DPSK.29">Phase Shift Keying</a>
 * @see <a href="http://en.wikipedia.org/wiki/High-Level_Data_Link_Control">HDLC Framing</a>
 */
abstract class SignalConstructor
      implements AudioSupportConstants {
   private static final String TAG = SignalConstructor.class.getSimpleName();

   private static final int PHASE_CHECK_WINDOW = (SAMPLES_PER_BIT / 2);
   private static final int SUMBUF_SAMPLES = PHASE_CHECK_WINDOW * 2;
   private short[] sumbuf = new short[SUMBUF_SAMPLES];
   private static final int STATE_INIT      = 0;
   private static final int STATE_PREAMBLE0 = 1;
   private static final int STATE_PREAMBLE1 = 2;
   private static final int STATE_PREAMBLE2 = 3;
   private static final int STATE_PREAMBLE3 = 4;
   private static final int STATE_START_BIT = 5;
   private static final int STATE_DATA      = 6;
   private static final int STATE_STOP_BIT  = 7;
   private static final int STATE_SKIP_BIT  = 8;
   private int state = STATE_INIT; //parser state machine
   private int stage = 0; //phase shift window sample stage register
   private int bit_samples = 0; //tracks number of samples processed for given bit
   private int bits = 0; //tracks number of bits process for given data byte
   private int prev_bit = 0; //toggles upon phase shifting
   private int[] bytes = new int[TRANSMISSIONS_PER_BUFFER]; //signal data bytes buffer
   private int cur_byte = 0; //current signal data byte
   private int sum; //running sample delta SUM
   private int threshold; //phase shifting detection threshold
   private int max, min; //audio sample maximum and minimum amplitude
   private int match; //running match counter
   private short prev_sample = 0; //previous sample value
   private int peak_out = 0; //peak output of SUM
   private int index = 0; //bit index, to compensate division: samplerate/bitrate
   private int offset = 0; //tracks offset from 1st threshold to peak position

   private static final int MAX_BUFFERED_SIGNALS = 4;
   private int[] nextSignals = new int[MAX_BUFFERED_SIGNALS];
   private int signalHEAD = 0;
   private int signalTAIL = 0;

   /*
   private boolean lpf_init = false;
   private double lpf_reg;
   private static final double lpf_alpha = ((1.0/44100) / ((1.0/44100) + (1.0 / 2400)));
   */
   private void putSignal(int sig) {
      int tail = signalTAIL;

      tail++;
      if (tail >= MAX_BUFFERED_SIGNALS) tail = 0;
      if (tail == signalHEAD) {
         SonrLog.w(TAG, "signal buffer overflow, signal lost.");
      } else {
         nextSignals[signalTAIL] = sig;
         signalTAIL = tail;
      }
   }

   private void validateSignal() {
      int sig = -1;
      if ((bytes[0] == bytes[1]) || (bytes[0] == bytes[2])) sig = bytes[0];
      else if (bytes[1] == bytes[2]) sig = bytes[1];
      if (sig != -1 ) putSignal(sig);
      else {
         SonrLog.w(TAG, String.format("invalid signal: %x %x %x", bytes[0], bytes[1], bytes[2]));
      }
   }

   /* LPF not helping that much
   private int lpfilter(int val) {

      if (lpf_init == false) {
         lpf_init = true;
         lpf_reg = val;
      } else {
         lpf_reg = lpf_reg + lpf_alpha * (val - lpf_reg);
      }
      //SonrLog.d(TAG, String.format("filter: %f %d/%d", lpf_reg, val, (int)lpf_reg));
      return (int)lpf_reg;
   }
   */

   //take care of the remainder from 44100 / 2400
   private int samplesPerBit (int bit) {
      int num = SAMPLES_PER_BIT;
      switch (bit % 8) {
         case 1 :
         case 3 :
         case 6 :
            num++;
         default : break;
      }
      return num;
   }

   int getSignal() {
      int sig = -1;
      if (signalHEAD != signalTAIL) {
         sig = nextSignals[signalHEAD];
         signalHEAD++;
         if (signalHEAD >= MAX_BUFFERED_SIGNALS) signalHEAD = 0;
      }
      return sig;
   }

   void parseSignal(short[] samples, int count) {
      int ii = 0;

      if (count < SUMBUF_SAMPLES) {
         SonrLog.w(TAG, String.format("unsupported: input buffer too small: %d", count));
         return;
      }

      //prefill phase shift detection window buffer
      if (state == STATE_INIT) {
         int jj;

         for (ii = 0; ii < SUMBUF_SAMPLES; ii++) {
            sumbuf[ii] = samples[ii];
         }

         sum = 0;
         for (jj = 0; jj < PHASE_CHECK_WINDOW; jj++) {
            sum += Math.abs(samples[jj + PHASE_CHECK_WINDOW] - samples[jj]);
         }
         stage = 0;

         SonrLog.d(TAG, String.format("signal parser initialized: %d", count));
         state = STATE_PREAMBLE0;
         max = min = match = 0;
         prev_sample = samples[ii];
      }
   
      while (ii < count) {
         int out;

         //running each sample through phase detection.
         short tmp1 = sumbuf[stage];
         short tmp2 = sumbuf[(stage + PHASE_CHECK_WINDOW) % SUMBUF_SAMPLES];

         sumbuf[stage] = samples[ii];
         sum += Math.abs(sumbuf[stage] - tmp2) - Math.abs(tmp2 - tmp1);

         stage++;
         if (stage >= SUMBUF_SAMPLES) stage = 0;

         //out = lpfilter(sum);
         out = sum;

         //SonrLog.d(TAG, String.format("filter out[%04d]: %d", ii, out));
         switch (state) {
            case STATE_PREAMBLE0 : //always starts from silence
               if (samples[ii] > 512) match = 0;
               else match++;

               // less than 2mili-seconds, but guaranteed silence
               if (match > SAMPLES_PER_BIT * 4) {
                  //SonrLog.d(TAG, "waiting for preamble");
                  state = STATE_PREAMBLE1;
                  max = min = match = 0;
               }
               break;

            case STATE_PREAMBLE1 : //process preamble
               if (Math.abs(samples[ii] - prev_sample) > PREAMBLE_DELTA && out < 100000) {
                  match++;
                  if (samples[ii] > max) max = samples[ii];
                  else if (samples[ii] < min) min = samples[ii];
               }

               if (match > SAMPLES_PER_BIT * 48) { //64 bit preamble - headroom for rampup
                  state = STATE_PREAMBLE2;
                  match = 0;
                  threshold = (PHASE_CHECK_WINDOW * (max - min)) / 4;
                  //SonrLog.d(TAG, String.format("threshold set: %d %d", out, threshold));
               }
               break;

            case STATE_PREAMBLE2 : //waiting for start bit
               if (out < threshold) break;

               state = STATE_PREAMBLE3;
               bit_samples = 0;
               peak_out = out;
               offset = 0;
               break;

            case STATE_PREAMBLE3 : //searching for peak and locate the very first start bit
               offset++;
               if (offset > SAMPLES_PER_BIT) {
                  SonrLog.w(TAG, "invalid preamble, restart");
                  state = STATE_PREAMBLE0;
               }
               if (out > threshold) {
                  if (out > peak_out) {
                     peak_out = out;
                     bit_samples = offset;
                  }
                  break;
               }

               //SonrLog.d(TAG, String.format("offset: %d", offset));
               index = 0;
               bits = 0;
               prev_bit = 1;
               cur_byte = 0;
               bytes[0] = 0;

               state = STATE_DATA;
               //SonrLog.d(TAG, String.format("start bit: %d/%d", out, threshold));
               break;

            case STATE_START_BIT :
               if (++bit_samples < samplesPerBit(index)) break;
               bit_samples = 0;
               index++;

               state = STATE_DATA;
               break;

            case STATE_DATA :
               if (++bit_samples < samplesPerBit(index)) break;
               bit_samples = 0;
               index++;

               if (out > threshold) {
                  prev_bit ^= 1;
               }
               bytes[cur_byte] <<= 1;
               bytes[cur_byte] |= prev_bit;

               bits++;
               if (bits < 8) break;
               //SonrLog.d(TAG, String.format("DATA[%d]: %x %d", cur_byte, bytes[cur_byte], threshold));

               cur_byte++;
               if (cur_byte < TRANSMISSIONS_PER_BUFFER) {
                  bits = 0;
                  prev_bit = 1;
                  bytes[cur_byte] = 0;
                  state = STATE_STOP_BIT;
               } else {
                  validateSignal();
                  state = STATE_PREAMBLE0;
               }
               break;

            case STATE_STOP_BIT :
               if (++bit_samples < samplesPerBit(index)) break;
               bit_samples = 0;
               index++;

               state = STATE_SKIP_BIT;
               break;

            case STATE_SKIP_BIT :
               if (++bit_samples < samplesPerBit(index)) break;
               bit_samples = 0;
               index++;

               state = STATE_START_BIT;
               break;
         }

         prev_sample = samples[ii];
         ii++;
      } //end of while (ii < count)
   }
}
