
package com.smartphonebeuro.sonr.v0;


import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

public class MicSerialListener extends Thread
{
	private static final String TAG = "SONR";
	
	public static final short SERIAL_TRANSMITTER_BAUD = 2400;
	public static final int SAMPLE_RATE = 44100;		//In Hz
	public static final int FRAMES_PER_BIT	 = SAMPLE_RATE/SERIAL_TRANSMITTER_BAUD;
	public static final int TRANSMISSION_LENGTH = (int) (FRAMES_PER_BIT) * 8;
	public static final int AVE_LEN = 9;
	public static final int BIT_OFFSET = MicSerialListener.FRAMES_PER_BIT*2;
	
	public static final int MAX_TRANSMISSIONS = 100;	//no more than 100 transmissions in a single sample


	private AudioRecord inStream;		//Serial input catcher
	private int bufferSize;				//size of inStream's buffer, set by AudioRecord.getMinBufferSize() in constructor
	private boolean running;			//condition for run() loop - constructor sets it to true, stopDriver() sets to false
	private ByteReceiver myByteReceiver;		//Received bytes are sent to myByteReciver.reciveByte(byte recivedByte)
	
	private int numSamples;
	private short sample_buf1[], sample_buf2[];		//switch out the buffers
	//we want to declare this data here so that we don't keep making new arrays and run out of memory
	private int[][] trans_buf = new int[MAX_TRANSMISSIONS][TRANSMISSION_LENGTH+MicSerialListener.AVE_LEN];
	private int[] movingsum = new int[MicSerialListener.TRANSMISSION_LENGTH+MicSerialListener.AVE_LEN];
	private int[] movingbuf = new int[9];
	
	public boolean switchbuffer = false;
	private boolean thread = false;
	
	private AudioProcessor myaudioprocessor = null;
	
	private static int SIGNAL_MAX_SUM = 0;
	
	private long start_check = 0;
	private static long CHECK_TIME = 1500;	//1.5 seconds
	
	private SONR sonrctx;
	
	public MicSerialListener(SONR ctx, AudioRecord theaudiorecord, int buffsize, ByteReceiver theByteReceiver) {
		if(inStream == null) {		//screen turned sideways, dont re-initialize to null
			myByteReceiver = theByteReceiver;
			sonrctx = ctx;
			
			Log.d(TAG, "STARTED");
			
			inStream = theaudiorecord;
			bufferSize = buffsize;
			
			if(inStream != null) {
				sample_buf1 = new short[bufferSize];
				sample_buf2 = new short[bufferSize];
			
				//set up thread
				running = true;
				this.setDaemon(true);
				inStream.startRecording();
				searchSignal();
			} else {
				theByteReceiver.receiveByte(-101);
				Log.d(TAG, "Failed to initialize AdioRecord");
			}
		}
	}
	
	public void searchSignal() {
		start_check = SystemClock.elapsedRealtime();
		boolean found = false;
		while((SystemClock.elapsedRealtime() - start_check < CHECK_TIME) && !found) {
			numSamples = inStream.read(sample_buf1, 0, bufferSize);
			synchronized(this) {
			found = AutoGainControl();
			}
		}
	}
	
	
	public void run() {		//thread reads in from mic and dispatches an audio processor to process the data
		while(running) {
			try{
				if(switchbuffer && !thread) {		//this is so that the buffer doesn't get overwritten while audio processor is working
					numSamples = inStream.read(sample_buf2, 0, bufferSize);
				} else if(!switchbuffer && !thread) {
					numSamples = inStream.read(sample_buf1, 0, bufferSize);
				}
				
				if(myaudioprocessor != null && myaudioprocessor.isAlive() && myaudioprocessor.IsWaiting()) {		//if a signal got cut off and audioprocessor is waiting
					thread = true;
					myaudioprocessor.buffer_notify();
					myaudioprocessor.interrupt();
				}

				if(numSamples > 0) {
					if(myaudioprocessor == null || !myaudioprocessor.isAlive()) {	//if not busy
						synchronized(this) {	//make sure no context switch to mess up switchbuffer
							if(switchbuffer)
								switchbuffer = false;
							else
								switchbuffer = true;
						}
						if(switchbuffer) {
							myaudioprocessor = new AudioProcessor(myByteReceiver, numSamples, sample_buf1, sample_buf2, trans_buf, movingsum, movingbuf);
						} else {
							myaudioprocessor = new AudioProcessor(myByteReceiver, numSamples, sample_buf2, sample_buf1, trans_buf, movingsum, movingbuf);
						}
						
						myaudioprocessor.start();
						thread = false;
					}
				}
			} catch(Exception e) {
				e.getStackTrace();
			}
		}
	}
	
	int sampleloc = 0;
	boolean AutoGainControl() {
		boolean found = false;
		
		int arraypos = 0;
		int startpos = MicSerialListener.TRANSMISSION_LENGTH+BIT_OFFSET;
		for(int i = startpos; i < startpos+9; i++) {
			movingbuf[i - startpos] = sample_buf1[i];
			movingsum[0] += sample_buf1[i];
		}
		SIGNAL_MAX_SUM = 0;
		for(int i = startpos + 9; i < numSamples; i++) {
			movingsum[1] = movingsum[0] - movingbuf[arraypos];
			movingsum[1] += sample_buf1[i];
			movingbuf[arraypos] = sample_buf1[i];
			arraypos++;
			if(arraypos == 9) arraypos = 0;
			
			//int dev = Math.abs(movingsum[1]-movingsum[0]);
			//if(dev > SIGNAL_MAX_SUM)
			if(Math.abs(movingsum[1]) > SIGNAL_MAX_SUM)
				SIGNAL_MAX_SUM = movingsum[1];

			movingsum[0] = movingsum[1];
		}
		
		SIGNAL_MAX_SUM/=2.5;
		findSample();
		
		if(sampleloc > 0) {
			arraypos = 0;
			movingsum[0] = 0;
			for(int i = 0; i < 9; i++) {
				movingbuf[i] = sample_buf1[i+sampleloc];
				movingsum[0] += sample_buf1[i+sampleloc];
			}
			trans_buf[0][0] = sample_buf1[sampleloc];
			for(int k = 1; k < MicSerialListener.TRANSMISSION_LENGTH; k++) {
				trans_buf[0][k] = sample_buf1[sampleloc+k];
				movingsum[k] = movingsum[k-1] - movingbuf[arraypos];
				movingsum[k] += sample_buf1[k+8+sampleloc];
				movingbuf[arraypos] = sample_buf1[k+8+sampleloc];
				arraypos++;
				if(arraypos == 9) arraypos = 0;
			}
			
			
			int byteInDec = 0;
			
			boolean isinphase = true, switchphase = true;	//we start out with a phase shift
			int bitnum = 0;
			
			for(int i = MicSerialListener.FRAMES_PER_BIT+1; i < MicSerialListener.TRANSMISSION_LENGTH; i++) {
				//if(isPhase(movingsum[i], movingsum[i-1]) && switchphase) {
				if(isPhase(movingsum[i-1]) && switchphase) {
					isinphase = !isinphase;
					switchphase = false;		//already switched
				}
				
				if(i % MicSerialListener.FRAMES_PER_BIT == 0) {
					if(!isinphase)	//if a 1
						byteInDec |= (0x1 << bitnum);
					bitnum++;
					switchphase = true;			//reached a bit, can now switch again if phase shifts
				}
			}
		
			
			Log.d(TAG, "TRANSMISSION: " + "0x"+ Integer.toHexString(byteInDec));
			
			if(byteInDec == 0x27) {
		        sonrctx.setDockFound();
		        found = true;
			}
		}
		return found;
	}
	
	private void findSample() {
		int arraypos = 0;
		int startpos = MicSerialListener.TRANSMISSION_LENGTH+MicSerialListener.BIT_OFFSET;
		for(int i = startpos; i < startpos + 9; i++) {
			movingbuf[i - startpos] = sample_buf1[i];
			movingsum[0] += sample_buf1[i];
		}
			
		for(int i = startpos + 9; i < numSamples; i++) {
			movingsum[1] = movingsum[0] - movingbuf[arraypos];
			movingsum[1] += sample_buf1[i];
			movingbuf[arraypos] = sample_buf1[i];
			arraypos++;
			if(arraypos == 9) arraypos = 0;
			
			if(MicSerialListener.isPhase(movingsum[1])) {
				sampleloc = i-10;
				return;
			}
			
			movingsum[0] = movingsum[1];
		}
	}

	public void onDestroy() {
		running = false;
		if(inStream != null)
			inStream.release();
		inStream = null;
		Log.d(TAG, "STOPPED");
	}

	public int round(double num) {
		if((int)(num + 0.5) > num) return (int)(num + 0.5);
		else return (int)num;
	}
 	
 	public interface ByteReceiver {
 		public void receiveByte(int receivedByte);
 	}
 	
 	
	public static boolean isPhase (int sum1) {
		if(Math.abs(sum1) > SIGNAL_MAX_SUM) return true;
		//if(Math.abs(sum1 - sum2) > SIGNAL_MAX_SUM) return true;
		else return false;
	}
}
