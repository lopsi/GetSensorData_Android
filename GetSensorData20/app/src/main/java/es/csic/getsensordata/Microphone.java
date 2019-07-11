package es.csic.getsensordata;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.util.Log;

public class Microphone {
	private int audiosamplerate=44100; //16000;   // 8000, 16000,   44.1kHz
	private int bufferSize = audiosamplerate/10;  // en 0.1 s se llena medio buffer
	private int audioReadRate=audiosamplerate/10;  // aviso a los 0.1 s  
	short[] buffer;
	private AudioRecord recorder;
	Context context;
	private OnRecordPositionUpdateListener mRecordListener;
	Boolean leer_audio=true;
	
	// ========================
	Microphone(OnRecordPositionUpdateListener mRecordListener)
	{
		this.mRecordListener=mRecordListener;
		buffer = new short[bufferSize];
		inicialize();
	}
	
	//=====================
	public short[] getbuffer()
	{
	short[] buffer_aux= new short[bufferSize];  // creo un buffer/array nuevo
	buffer_aux=buffer.clone();   //copio los contenidos del buffer actual al nuevo
	return buffer_aux;  // mando una copia del buffer
	}
	//======================
	private void inicialize()
	{
		int minBufferSize = AudioRecord.getMinBufferSize(audiosamplerate,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		if (bufferSize>minBufferSize )
		{  	Log.i("Audio","minBufferSize: "+minBufferSize); }
		try{
			recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, audiosamplerate,
					AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT, bufferSize*2);
			recorder.setPositionNotificationPeriod(audioReadRate);
			Log.i("Audio","Setting Audio Record Listener");
			recorder.setRecordPositionUpdateListener(mRecordListener);
			
		} catch(Exception E){
			Log.i("Audio","No puedo leer, no bien configurado MIC");
		}

		Log.i("Audio","+1.5End Start Audio Recording inside Thread");
	}
	
	//===========read==============
	public void start_read_audio_Thread()
	{
	final AudioRecord recorder_in_thread=recorder;  // copio referencia a objeto y lo pongo "final" (no copio el objeto)


	Thread readingThread = new Thread() {
		@Override
		public void run() {
			

		recorder_in_thread.startRecording();
		leer_audio=true;
		while (leer_audio)
		{
			recorder_in_thread.read(buffer, 0, bufferSize);  // bloquea mientras lee o llena el buffer (por eso en nuevo thread)
			//Log.i("Audio","READ Audio Recording inside Thread");
			try {
			//Log.i("Audio","SLEEP Audio Recording inside Thread");
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} // end-while
		}
	};
	readingThread.setName("Hilo Leer Microfono");
	readingThread.start();  
	}
	
	//=============================
	public void stop_audio_thread()
	{
		Log.i("Audio","Audio Interrupted");
		leer_audio=false;
		if (recorder.getState()==AudioRecord.STATE_INITIALIZED  && recorder.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)
		{
			recorder.stop();
			//recorder.release();
		}
	}
	
	//==============================
	public void release()
	{
		Log.i("Audio","Audio Interrupted");
		if (recorder.getState()==AudioRecord.STATE_INITIALIZED && recorder.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING)
		{
			//recorder.stop();
			recorder.release();
		}
	}

}

