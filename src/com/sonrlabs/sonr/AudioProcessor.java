package com.sonrlabs.sonr;

import com.sonrlabs.sonr.MicSerialListener.ByteReceiver;

import android.util.Log;

public class AudioProcessor extends Thread {
	
	private static final String TAG = "SONR audio processor";
	
	private ByteReceiver myByteReceiver;
	
	short[] sample_buf, sample_buf2;
	private int[][] trans_buf;
	private int numSamples;
	private int[] sampleloc = new int[MicSerialListener.MAX_TRANSMISSIONS];
	private int samplelocsize = 0;
	private int buffer;
	private static final int BUFFER_AVAILABLE = 1;
	//notify that another buffer is available
	public void buffer_notify() { buffer++; }
	
	private boolean iswaiting = false;
	public boolean IsWaiting() {return iswaiting;}
	
	int[] movingsum;
	int[] movingbuf;
	
	AudioProcessor(ByteReceiver tempByteReceiver, int numsamples, 
			short[] thesamples, short[] thesamples2, int[][] thetrans_buf, int[] movsum, int[] movbuf) {
		//pass all of these values by reference so that memory is only allocated once in MicSerialListener
		myByteReceiver = tempByteReceiver;
		numSamples = numsamples;
		sample_buf = thesamples;
		sample_buf2 = thesamples2;
		trans_buf = thetrans_buf;
		buffer = 0;
		movingbuf = movbuf;
		movingsum = movsum;
	}

	@Override
	public void run() {
		processSample();
	}
	
	@Override
	public void destroy() {

	}

	
	private void processSample() {

		synchronized(this) {
			findSample();
			if(samplelocsize > 0) {
				//copy transmission down because the buffer could get overwritten
				int count2 = 0;
				try{
				for(int j = 0; j < samplelocsize; j++) {
					for(int i = 0; i < MicSerialListener.TRANSMISSION_LENGTH+MicSerialListener.AVE_LEN; i++) {
						if(sampleloc[j] + i < numSamples) {
							trans_buf[j][i] = sample_buf[sampleloc[j] + i];
						} else if(buffer >= BUFFER_AVAILABLE && sampleloc[j] + i >= numSamples) {		//circular "queue"
							trans_buf[j][i] = sample_buf2[count2++];
						} else {	//no extra buffer, wait
							try{
								iswaiting = true;
								Thread.sleep(200);
							}catch(Exception e){
								Log.e("SONR", e.getStackTrace().toString());
							}
							iswaiting = false;
							i--;	//redo
						}
					}
				}
				} catch(Exception e){
					Log.e("SONR", e.getStackTrace().toString());
				}
			} else return;		//nothing found
		}


		for(int s = 0; s < samplelocsize; s++) {
			int arraypos = 0;
			movingsum[0] = 0;
			for(int i = 0; i < 9; i++) {
				movingbuf[i] = trans_buf[s][i];
				movingsum[0] += trans_buf[s][i];
			}
			
			for(int i = 1; i < MicSerialListener.TRANSMISSION_LENGTH; i++) {
				movingsum[i] = movingsum[i-1] - movingbuf[arraypos];
				movingsum[i] += trans_buf[s][i+8];
				movingbuf[arraypos] = trans_buf[s][i+8];
				arraypos++;
				if(arraypos == 9) arraypos = 0;
			}
			
			
			int byteInDec = 0;
			
			boolean isinphase = true, switchphase = true;	//we start out with a phase shift
			int bitnum = 0;
			
			for(int i = MicSerialListener.FRAMES_PER_BIT+1; i < MicSerialListener.TRANSMISSION_LENGTH; i++) {
				if(MicSerialListener.isPhase(movingsum[i-1]) && switchphase) {
					isinphase = !isinphase;
					switchphase = false;		//already switched
				}
				
				if(i % MicSerialListener.FRAMES_PER_BIT == 0) {
					if(!isinphase)	//if a 1
						byteInDec |= (0x1 << bitnum);			//i/MicSerialListener.FRAMES_PER_BIT-1
					bitnum++;
					switchphase = true;			//reached a bit, can now switch
				}
			}
	
			
			Log.d(TAG, "TRANSMISSION: " + "0x"+ Integer.toHexString(byteInDec));
			
			if(byteInDec != 0x27)
				myByteReceiver.receiveByte(byteInDec); //RECEIVED THE BYTE!
		}
	}


	private void findSample() {
		movingsum[0] = 0;
		int arraypos = 0;
		int startpos = MicSerialListener.TRANSMISSION_LENGTH+MicSerialListener.BIT_OFFSET;
		for(int i = startpos; i < startpos + 9; i++) {
			movingbuf[i - startpos] = sample_buf[i];
			movingsum[0] += sample_buf[i];
		}
			
		for(int i = startpos + 9; i < numSamples; i++) {
			movingsum[1] = movingsum[0] - movingbuf[arraypos];
			movingsum[1] += sample_buf[i];
			movingbuf[arraypos] = sample_buf[i];
			arraypos++;
			if(arraypos == 9) arraypos = 0;
			
			if(MicSerialListener.isPhase(movingsum[1])) {
				if(i < startpos*2) {
					movingsum[0] = 0;
					arraypos = 0;
					int offset = i - MicSerialListener.TRANSMISSION_LENGTH - MicSerialListener.BIT_OFFSET;
					for(int b = offset; b < offset + 9; b++) {
						movingbuf[b - offset] = sample_buf[b];
						movingsum[0] += sample_buf[b];
					}
					for(int b = offset + 9; b < i; b++) {
						movingsum[1] = movingsum[0] - movingbuf[arraypos];
						movingsum[1] += sample_buf[b];
						movingbuf[arraypos] = sample_buf[b];
						arraypos++;
						if(arraypos == 9) arraypos = 0;
						if(MicSerialListener.isPhase(movingsum[1])) {
							i = b;
							break;
						}
					}
				}
				
				sampleloc[samplelocsize] = i-10;
				samplelocsize++;
				if(samplelocsize >= 100)
					return;
				
				for(int m = i-18; m < i + MicSerialListener.TRANSMISSION_LENGTH - 18; m++)
					trans_buf[0][m-i+18] = sample_buf[m];
				i += MicSerialListener.TRANSMISSION_LENGTH+MicSerialListener.BIT_OFFSET+9;
				
				movingsum[0] = 0;
				arraypos = 0;
				for(int t = i; t < i + 9; t++) {
					movingbuf[t - i] = sample_buf[t];
					movingsum[0] += sample_buf[t];
				}
				i += 8;
			} else
				movingsum[0] = movingsum[1];
		}
	}
}

