/***********************************************************************
 ** Copyright (C) 2012 LP-Research
 ** All rights reserved.
 ** Contact: LP-Research (klaus@lp-research.com)
 **
 ** Redistribution and use in source and binary forms, with 
 ** or without modification, are permitted provided that the 
 ** following conditions are met:
 **
 ** Redistributions of source code must retain the above copyright 
 ** notice, this list of conditions and the following disclaimer.
 ** Redistributions in binary form must reproduce the above copyright 
 ** notice, this list of conditions and the following disclaimer in 
 ** the documentation and/or other materials provided with the 
 ** distribution.
 **
 ** THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 ** "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 ** LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 ** FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 ** HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 ** SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 ** LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 ** DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 ** THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 ** (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 ** OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ***********************************************************************/

package es.csic.getsensordata;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

/**
 *  Thread class to retrieve data from LPMS-B (and eventually control configuration of LPMS-B) 
 *  based on the code available in: http://www.lp-research.com/support/
 *
 *  To get access to the LPMS IMU, it is necessary to:
 *  1) Get the Bluetooth adapter:
 *  	    mAdapter = BluetoothAdapter.getDefaultAdapter();
 *  2) Create the object (according to the desired communication)
 *  	    mLpmsB = new LPMSB_IMU(mAdapter,MacAddress,handlerMTi); //Bluetooth ID MacAddress (in our case "00:06:66:45:D2:83")
 *  3) Connect a socket to the Bluetooth RFID reader device
 mLpmsB.connect(true, true, true, false, false,true);
 *  4) Start reading the LPMS IMU (put the reader in Measurement mode):
 mLpmsB.startreading();
 Now the UI principal thread receives messages in the handlerLPMS. The messages are:
 Connected:
 "MensajeType"->"Connect"
 "ReaderName"->"LPMS_IMU"
 "Connected"->true
 or
 Data received:
 "MensajeType"->"IMU_Data"
 "ReaderName"->"LPMS_IMU"
 "Accelerations_x"-> float (m/s^2)
 "Accelerations_y"-> float (m/s^2)
 "Accelerations_z"-> float (m/s^2)
 "TurnRates_x" -> float (rad/s)
 "TurnRates_y" -> float (rad/s)
 "TurnRates_z" -> float (rad/s)
 "MagneticFields_x" -> float (mT)
 "MagneticFields_y" -> float (mT)
 "MagneticFields_z" -> float (mT)
 "Euler_Roll" -> float (&#xfffd;)
 "Euler_Pitch" -> float (&#xfffd;)
 "Euler_Yaw" -> float (&#xfffd;)
 "Temperature" -> float (0.0f) not used for the momment
 "Pressure" -> Atmospheric Pressure (mbar)
 "Counter" -> short (package number)
 "Time" -> long (System time in ns)
 "timeStamp" -> IMU time (s)

 *  5) When you do not need the LPMS IMU anymore stop and disconnect it:
 mLpmsB.stopreading();    // llamada en onPause();
 mLpmsB.disconnect();    // llamada en onDestroy();

 */
public class LPMSB_IMU extends Thread {
	// Log tag
	final String TAG = "LpmsB";

	// Standard Bluetooth serial protocol UUID
	final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");	

	// LpBus identifiers
	final int PACKET_ADDRESS0 = 0;
	final int PACKET_ADDRESS1 = 1;
	final int PACKET_FUNCTION0 = 2;
	final int PACKET_FUNCTION1 = 3;
	final int PACKET_RAW_DATA = 4;
	final int PACKET_LRC_CHECK0 = 5;
	final int PACKET_LRC_CHECK1 = 6;
	final int PACKET_END = 7;
	final int PACKET_LENGTH0 = 8;
	final int PACKET_LENGTH1 = 9;

	// LPMS-B function registers (most important ones only, currently only LPMS_GET_SENSOR_DATA is used)
	final int LPMS_ACK = 0;
	final int LPMS_NACK = 1;
	final int LPMS_GET_CONFIG = 4;	
	final int LPMS_GET_STATUS = 5;	
	final int LPMS_GOTO_COMMAND_MODE = 6;	
	final int LPMS_GOTO_STREAM_MODE = 7;	
	final int LPMS_GOTO_SLEEP_MODE = 8;	
	final int LPMS_GET_SENSOR_DATA = 9;
	final int LPMS_SET_TRANSMIT_DATA = 10;	
	final int LPMS_SET_STREAM_FREQ=11;
	final int LPMS_SET_GYR_RANGE=25;
	final int LPMS_SET_ACC_RANGE=31;
	final int LPMS_SET_MAG_RANGE=33;

	// State machine states. Currently no states are supported
	final int STATE_IDLE = 0;

	// Class members
	int rxState = PACKET_END;
	byte[] rxBuffer = new byte[512];
	byte[] txBuffer = new byte[512];
	byte[] rawTxData = new byte[1024];
	byte[] rawRxBuffer = new byte[1024];	
	int currentAddress;
	int currentFunction;
	int currentLength;
	int rxIndex = 0;
	byte b = 0;
	int lrcCheck;
	boolean isConnected = false;
	boolean imposible_conectarme=false;
	int nBytes;
	int timeout;
	boolean waitForAck;
	boolean waitForData;
	int state;
	byte inBytes[] = new byte[2];    
	InputStream mInStream;
	OutputStream mOutStream;
	BluetoothSocket mSocket;
	BluetoothAdapter mAdapter;
	String mAddress;
	BluetoothDevice mDevice;
	LpmsBData mLpmsBData = new LpmsBData();
	private BufferedOutputStream mBuffer;
	boolean isGetGyroscope = true;
	boolean isGetAcceleration = true;
	boolean isGetMagnetometer = true;
	boolean isGetQuaternion = true;
	boolean isGetEulerAngler = true;
	boolean isGetPressure = true;
	boolean isGetTemperature = true;
	boolean isGetLPMSTimeStamp = false;
	long time0,receivedMeassurements=0;
	private Handler handlerLPMSIMU;
	private String mName="LPMS_IMU";
	private Thread thread_connect=null;
	public Thread thread_read=null;
	private long receivedBytes=0;
	String MAC_address_string;
	boolean en_thread_read=false;
	private UUID uuid;
	private Boolean uuid_obtained=false;

//----------------------------Constructores--------------------------------
	/**
	 * Initializes the LPMS object
	 * 
	 * @param adapter Bluetooth adapter
	 */
	LPMSB_IMU(BluetoothAdapter adapter) {
		mAdapter = adapter;
		mBuffer = null;
		handlerLPMSIMU=null;
	}

	/**
	 *  Initializes the LPMS object with BluetoothAdapter adapter and saves the info in the buffer specified in the buffer item
	 * @param adapter Bluetooth adapter
	 * @param buffer  Data writing buffer
	 */
	LPMSB_IMU(BluetoothAdapter adapter, BufferedOutputStream buffer) {
		mAdapter = adapter;
		mBuffer = buffer;
		handlerLPMSIMU=null;
	}

	/**
	 *  Initializes the LPMS object sending the received information as messages to the specified handler
	 * @param adapter Bluetooth adapter
	 * @param handler Handler to receive the messages
	 */
	public LPMSB_IMU(BluetoothAdapter adapter, String MacAddress, Handler handler) {
		mAdapter = adapter;
		mBuffer = null;
		handlerLPMSIMU=handler;
		MAC_address_string=MacAddress;
	}
	/**
	 * Initializes the LPMS object sending the received information as messages to the specified handler and saves in the specified file
	 * 
	 * @param adapter Bluetooth adapter
	 * @param handler Handler to receive the messages
	 * @param buffer  Data writing buffer
	 */
	public LPMSB_IMU(BluetoothAdapter adapter, Handler handler, BufferedOutputStream buffer) {
		mAdapter = adapter;
		mBuffer = buffer;
		handlerLPMSIMU=handler;
	}
	
	//--------------------connect---------------------------------------
	public void connect(boolean isGetLPMSTimeStamp,boolean isGetGyroscope,boolean isGetAcceleration,boolean isGetMagnetometer,
			boolean isGetQuaternion,boolean isGetEulerAngler,boolean isGetPressure,	boolean isGetTemperature) {

		this.isGetLPMSTimeStamp = isGetLPMSTimeStamp;
		this.isGetGyroscope = isGetGyroscope;
		this.isGetAcceleration = isGetAcceleration;
		this.isGetMagnetometer = isGetMagnetometer;
		this.isGetQuaternion = isGetQuaternion;
		this.isGetEulerAngler = isGetEulerAngler;
		this.isGetPressure = isGetPressure;
		this.isGetTemperature = isGetTemperature;

		// Starts new Connection thread
		thread_connect = new Thread(new ConnectThread());
		if (thread_connect.isAlive()==false)
		{
		thread_connect.start();
		}
	}

	//-------------Class to connect to LPMS-B-----------------------------
	public class ConnectThread implements Runnable {
		public void run() {
			Log.i(TAG, "[LpmsBConnectThread] Checking Bluetooth Adapter");
			if (mAdapter == null) {
				Log.e(TAG, "[LpmsBConnectThread] Didn't find Bluetooth adapter");
				return;
			}
			

			mAddress = MAC_address_string;
			Log.i(TAG, "[LpmsBConnectThread] Getting device with address " + mAddress);
			try {
				mDevice = mAdapter.getRemoteDevice(mAddress);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "[LpmsBConnectThread] Invalid Bluetooth address", e);
				return;
			}

			mSocket = null;
			Log.i(TAG, "[LpmsBConnectThread] Creating socket");
			try {
				// Forma como lo hace Francisco, y funciona bien en la Tablet Xoom2:
				//mSocket = mDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);

				// De esta otra forma, que es como lo hace Antonio va igualmente bien con la tableta Xoom2 pero no bien con S3
				//				if ( mDevice.getBondState()==BluetoothDevice.BOND_BONDED )
				//				{
				//					ParcelUuid[] uuids=mDevice.getUuids();
				//					if (uuids.length>0)
				//					{
				//						uuid=uuids[0].getUuid();
				//						Log.i(TAG,"Object RFIDReader Created. Got UUID");
				//						uuid_obtained=true;
				//					}
				//				}
				//				mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
				
				
				// Segun un foro: http://stackoverflow.com/questions/12274210/android-bluetooth-spp-with-galaxy-s3
				// Hay un bug de Android con el Galaxy S3 que dificulta la conexion de Bluetooth.
				// Lo resuelven asi:
				//device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
				//Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
				//socket = (BluetoothSocket)m.invoke(device, Integer.valueOf(1)); 
				
				Method m = mDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
				mSocket = (BluetoothSocket)m.invoke(mDevice, Integer.valueOf(1)); 

				
			} catch (Exception e) {
				Log.e(TAG, "[LpmsBConnectThread] Socket create() failed", e);
				return;
			}

			Log.i(TAG, "[LpmsBConnectThread] Trying to connect..");	
			Integer intentos;

			try {
				mAdapter.cancelDiscovery();  // Lo recomiendan antes de llamar a "connect"
				mSocket.connect();  // Blocking function
				isConnected=true;  // indica al hilo de lectura "threadread" que ya esta el socket conectado
			} catch (IOException e) {
				//Log.e(TAG, "[LpmsBConnectThread] Couldn't connect to device", e);// Saca el mensaje en texto rojo en "LogCat"
				Log.e(TAG, "[LpmsBConnectThread] Couldn't connect to device. ");
				//System.out.println("RFIDM220Reader ERROR: Socket NOT connected");
				imposible_conectarme=true;
				return;
			}



			if (isConnected)
			{
				Log.i(TAG, "[LpmsBConnectThread] Connected!");		

				try {
					mInStream = mSocket.getInputStream();
					mOutStream = mSocket.getOutputStream();
				} catch (IOException e) {
					Log.e(TAG, "[LpmsBConnectThread] Streams not created", e);
					return;
				}		

				configura();  // Una vez conectado, configura sensor LPMS poniendolo en modo comando

				if(mBuffer!=null){
					try {
						time0=System.currentTimeMillis();
						mBuffer.write(("Time (ms): " + time0 + "\nData counter, System Time (ns), -Acceleration[3] (g) , TurnRate[3] (rad/s), MagneticField[3] (mT), IMU time (s)\n").getBytes());
					} catch (IOException e) {
						Log.e(TAG, "[LpmsBConnectThread] Failed file writing", e);		
					}
				}
				if(handlerLPMSIMU!=null){
					Message mensaje= new Message();
					Bundle data = new Bundle();
					data.putString("MensajeType","Connect");
					data.putString("ReaderName",mName);
					data.putBoolean("Connected",true);
					mensaje.setData(data);
					handlerLPMSIMU.sendMessage(mensaje);
				}
				Log.i(TAG, "[LpmsBConnectThread] Concetado Socket Bluetooth");
			}
			
		}
	}

	
	//---------------configura----------------------------------
	void configura(){

		// Asumo que ya hay conexion establecida
		int config=0x00000000;  // 32 bits  (el bit 0 es el de la derecha, y el 31 el de la izquierda)
		
		// Configurar LPMS
		Log.i(TAG, "Enviar comando: LPMS_GOTO_COMMAND_MODE");
		sendData(0, LPMS_GOTO_COMMAND_MODE, 0);  // modo comando
		try {
			Thread.sleep(400);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		config=0x000007D0;   // Poner rango Gyro: 2000�/s (4 bytes:   7D0 hex = 2000 dec)
		convertIntToTxbytes(config, 0, rawTxData);
		sendData(0, LPMS_SET_GYR_RANGE, 4);
		
		config=0x00000004;   // Poner rango Acc: 4g (4 bytes:   4 hex = 4 dec)
		convertIntToTxbytes(config, 0, rawTxData);
		sendData(0, LPMS_SET_ACC_RANGE, 4);
		
		config=0x000000FA;   // Poner rango Mag: 250uT (4 bytes:   FA hex = 250 dec)
		convertIntToTxbytes(config, 0, rawTxData);
		sendData(0, LPMS_SET_MAG_RANGE, 4);
		
		config=0x00000064;   // Poner Sampling Freq: 100 Hz (4 bytes:   64hex = 100 dec)
		convertIntToTxbytes(config, 0, rawTxData);
		sendData(0, LPMS_SET_MAG_RANGE, 4);
		
		Log.i(TAG, "Eviar comando: LPMS_SET_TRANSMIT_DATA");
		config=0x00000000;  // 32 bits  (el bit 0 es el de la derecha, y el 31 el de la izquierda)
		if (isGetPressure)  // pressure bit 9
		{ config= config | 0x00000200; }
		if (isGetMagnetometer)  // Mag bit 10
		{ config= config | 0x00000400; }
		if (isGetAcceleration)  // Acc bit 11
		{ config= config | 0x00000800; }
		if (isGetGyroscope)  // Gyro bit 12
		{ config= config | 0x00001000; }
		if (isGetTemperature)  // Temp bit 13
		{ config= config | 0x00002000; }
		if (isGetEulerAngler)  // Euler bit 17
		{ config= config | 0x00020000; }
		if (isGetQuaternion)  // Quat bit 18
		{ config= config | 0x00040000; }
		convertIntToTxbytes(config, 0, rawTxData);
		sendData(0, LPMS_SET_TRANSMIT_DATA, 4);

		try {
			Thread.sleep(400);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


	}
	//---------------------startreading------------------------------
	void startreading(){
		thread_read = new Thread(new ClientReadThread());
		if (thread_read.isAlive()==false)
		{
		// Starts new reader thread
		thread_read.start();
		}
	}
	//---------------------ReadThread------------------------------
	//  Class to continuously read data from LPMS-B
	public class ClientReadThread implements Runnable {

		public void run() {
            //  He quitado la llamada a este hilo ya que no parece muy util !!!!!!!!!!!!!!!!!!!!
			// Starts state machine thread
			// 	Thread t = new Thread(new ClientStateThread());	
			// 	t.start();	
			en_thread_read=true;

			//----- esperar a que se conecte-------
			while (isConnected==false  && en_thread_read  && imposible_conectarme==false)
			{ 
				Log.i(TAG,"=============Loop LPMS=====================");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} // Esperar a que el socket (en "ConnectThred") se conecte de verdad
			if (isConnected)
			{
				//----- poner en modo medida continua------
				Log.i(TAG, "Enviar comando: LPMS_GOTO_STREAM_MODE");
				sendData(0, LPMS_GOTO_STREAM_MODE, 0);  // modo stream

				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				//----leer sin parar---------------*********
				while (en_thread_read) {
					try {
						nBytes = mInStream.read(rawRxBuffer);

					} catch (Exception e) {
						break;
					}
					receivedBytes+=nBytes;
					//Log.i("LPMS-B","Num bytes leidos: "+nBytes);
					// Parses received LpBus data
					parse();  //---PARSE-----
				}
			}
		}
	}	

	
	//-------------------StopReading--------------------------
	public void stopreading(){
		en_thread_read=false;  // para indicarle que salga del loop en el "thread_read"
		if (isConnected)  // si conectado
		{
		try {
				if (thread_read.isAlive())
				{
					thread_read.interrupt();
				}
				Log.i(TAG,"StopReading: ReadingThread interrupted");
			} catch (Exception e) {};
			
			Log.i(TAG, "Enviar comando: LPMS_GOTO_SLEEP_MODE");
			sendData(0, LPMS_GOTO_SLEEP_MODE, 0);  // modo SLEEP
			//sendData(0, LPMS_GOTO_COMMAND_MODE, 0);  // modo COMMAND
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else
		{
			System.out.println("LPMS IMU. Info: No stopping done since it was not connected nor reading");
		}
	}
	
	//-------------Disconnect-----------------------------
	public void disconnect(){
		if (isConnected)  // si conectado
		{
			try {
				if (thread_connect.isAlive())
				{
				thread_connect.interrupt();
				}
				Log.i(TAG,"[LpmsBThread]disconnect: ConnectThread interrupted");
			} catch (Exception e) {};

			try {
				Log.i(TAG,"[LpmsBThread]INI:Socket closed on disconnetct method");
				if (mSocket.isConnected())
				{
					mSocket.close();
					isConnected = false;
				}
				Log.i(TAG,"[LpmsBThread]END:Socket closed on disconnetct method");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("LPMSB IMU. Info: No disconnection done since it was not connected");
		}
	}
	
	//---------------------------------------
	public boolean isReading(){
		if(thread_read==null)
			return false;
		else
			return thread_read.isAlive();
	}
	//---------------------------------------
	public long readedBytes(){
		return receivedBytes;
	}

	//-------------------------No la uso--------------	
	/**
	 *  State machine thread class
	 * 
	 */
	private class ClientStateThread implements Runnable {
		public void run() {
			try {				
				while (true) {
					if (waitForAck == false && waitForData == false) {
						switch (state) {
						case STATE_IDLE:
							break;
						}
					} else if (timeout > 100) {
						Log.e(TAG, "[LpmsBThread] Receive timeout");
						timeout = 0;
						state = STATE_IDLE;
						waitForAck = false;
						waitForData = false;
					} else {
						Thread.sleep(10);
						++timeout;
					} 
				}
			} catch (Exception e) {
				Log.d(TAG, "[LpmsBThread] Connection interrupted");
				isConnected = false;			
			}
		}
	}
	
	//---------------------------parse (state machine)-------------------------------------
	/**
	 *  Parses LpBus raw data
	 */
	private void parse() {
		int lrcReceived = 0;

		for (int i=0; i<nBytes; i++) {
			b = rawRxBuffer[i];

			switch (rxState) {
			case PACKET_END:
				if (b == 0x3a) {
					rxState = PACKET_ADDRESS0;
				}
				break;

			case PACKET_ADDRESS0:
				inBytes[0] = b;
				rxState = PACKET_ADDRESS1;
				break;

			case PACKET_ADDRESS1:
				inBytes[1] = b;				
				currentAddress = convertRxbytesToInt16(0, inBytes);
				rxState = PACKET_FUNCTION0;
				// Log.d(TAG, "[LpmsBThread] LpBus received address: " + Integer.toString(currentAddress));
				break;

			case PACKET_FUNCTION0:
				inBytes[0] = b;
				rxState = PACKET_FUNCTION1;				
				break;

			case PACKET_FUNCTION1:
				inBytes[1] = b;				
				currentFunction = convertRxbytesToInt16(0, inBytes);			
				rxState = PACKET_LENGTH0;	
				// Log.d(TAG, "[LpmsBThread] LpBus received function: " + Integer.toString(currentFunction));				
				break;

			case PACKET_LENGTH0:
				inBytes[0] = b;
				rxState = PACKET_LENGTH1;
				break;

			case PACKET_LENGTH1:
				inBytes[1] = b;				
				currentLength = convertRxbytesToInt16(0, inBytes);	
				rxState = PACKET_RAW_DATA;
				rxIndex = 0;
				//Log.i(TAG, "[LpmsBThread] LpBus received length: " + Integer.toString(currentLength));
				break;

			case PACKET_RAW_DATA:
				if (rxIndex == currentLength) {
					lrcCheck = (currentAddress & 0xffff) + (currentFunction & 0xffff) + (currentLength & 0xffff);

					for (int j=0; j<currentLength; j++) {
						lrcCheck += (int) rxBuffer[j] & 0xff;
					}		

					inBytes[0] = b;		
					rxState = PACKET_LRC_CHECK1;								
				} else {	
					rxBuffer[rxIndex] = b;
					++rxIndex;
					// Log.d(TAG, "[LpmsBThread] LpBus received byte: " + Byte.toString(b));
				}
				break;

			case PACKET_LRC_CHECK1:
				inBytes[1] = b;

				lrcReceived = convertRxbytesToInt16(0, inBytes);
				lrcCheck = lrcCheck & 0xffff;

				// Log.d(TAG, "[LpmsBThread] LpBus lrcReceived: " + Integer.toString(lrcReceived));
				// Log.d(TAG, "[LpmsBThread] LpBus lrcCheck: " + Integer.toString(lrcCheck));

				if (lrcReceived == lrcCheck) {
					// Log.d(TAG, "[LpmsBThread] Successfully received data packet");	
					parseFunction();   // -----------PARSE-----------
				} else {
					Log.e(TAG, "[LpmsBThread] Check-sum ERROR. State: " + Integer.toString(state) + ". Function: " + Integer.toString(currentFunction));
				}

				rxState = PACKET_END;
				break;

			default:
				rxState = PACKET_END;
				break;
			}
		}
	}

	//--------------------------------------------------------------------------
	/**
	 *  Parses LpBus function
	 */
	private void parseFunction() {	
		switch (currentFunction) {
		case LPMS_ACK:
			Log.d(TAG, "[LpmsBThread] Received ACK");
			break;

		case LPMS_NACK:
			Log.d(TAG, "[LpmsBThread] Received NACK");
			break;

		case LPMS_GET_CONFIG:
			break;

		case LPMS_GET_STATUS:
			break;

		case LPMS_GOTO_COMMAND_MODE:
			break;

		case LPMS_GOTO_STREAM_MODE:
			break;

		case LPMS_GOTO_SLEEP_MODE:
			break;

			// If new sensor data is received parse the data and send/save it
		case LPMS_GET_SENSOR_DATA:
			receivedMeassurements++;
			parseSensorData();   //-------------PARSE--------------
			if(mBuffer!=null){
				try {
					mBuffer.write((mLpmsBData.count + ", " + System.nanoTime() +", " + mLpmsBData.acc[0] + ", " + mLpmsBData.acc[1] + ", " + mLpmsBData.acc[2] + ", " + mLpmsBData.gyr[0] + ", " + mLpmsBData.gyr[1] + ", " + mLpmsBData.gyr[2] + ", " + mLpmsBData.mag[0] + ", " + mLpmsBData.mag[1] + ", " + mLpmsBData.mag[2] + ",0 , " + mLpmsBData.timeStamp + "\n").getBytes());
				} catch (IOException e) {
					Log.e(TAG, "[LpmsBThread] Failed file writing", e);
				}
			}
			if (handlerLPMSIMU!=null){
				Message menssaje= new Message();
				Bundle data = new Bundle();
				data.putString("MensajeType","IMU_Data");
				data.putString("ReaderName",mName);
				data.putFloat("timeStamp", mLpmsBData.timeStamp);
				data.putFloat("Accelerations_x", mLpmsBData.acc[0]);
				data.putFloat("Accelerations_y", mLpmsBData.acc[1]);
				data.putFloat("Accelerations_z", mLpmsBData.acc[2]);
				data.putFloat("TurnRates_x", mLpmsBData.gyr[0]);
				data.putFloat("TurnRates_y", mLpmsBData.gyr[1]);
				data.putFloat("TurnRates_z", mLpmsBData.gyr[2]);
				data.putFloat("MagneticFields_x", mLpmsBData.mag[0]);
				data.putFloat("MagneticFields_y", mLpmsBData.mag[1]);
				data.putFloat("MagneticFields_z", mLpmsBData.mag[2]);
				data.putFloat("Euler_Roll", mLpmsBData.euler[0]);
				data.putFloat("Euler_Pitch", mLpmsBData.euler[1]);
				data.putFloat("Euler_Yaw", mLpmsBData.euler[2]);
				data.putFloat("quaternions1", mLpmsBData.quat[0]);
				data.putFloat("quaternions2", mLpmsBData.quat[1]);
				data.putFloat("quaternions3", mLpmsBData.quat[2]);
				data.putFloat("quaternions4", mLpmsBData.quat[3]);
				data.putFloat("Temperature", mLpmsBData.temp);
				data.putFloat("Pressure", mLpmsBData.pres);
				data.putLong("Counter", mLpmsBData.count);
				data.putLong("Time", System.nanoTime());
				menssaje.setData(data);
				handlerLPMSIMU.sendMessage(menssaje);
			}
			break;

		case LPMS_SET_TRANSMIT_DATA:
			break;
		}

		waitForAck = false;
		waitForData = false;
	}
	
	//----------------------------------------------------------------------
	/**
	 *  Parses received sensor data (received with function value LPMS_GET_SENSOR_DATA)
	 */
	void parseSensorData() {
		int o = 0;
		float r2d = 57.2958f; 	// multiply by 57.2958f; for degrees

		if (isGetLPMSTimeStamp == true) {
			mLpmsBData.timeStamp = convertRxbytesToFloat(o, rxBuffer); o += 4;
		}

		if (isGetGyroscope == true) {  // in rad/s
			mLpmsBData.gyr[0] = convertRxbytesToFloat(o, rxBuffer); o += 4;
			mLpmsBData.gyr[1] = convertRxbytesToFloat(o, rxBuffer); o += 4;
			mLpmsBData.gyr[2] = convertRxbytesToFloat(o, rxBuffer); o += 4;
			// Log.d(TAG, "[LpmsBThread] Gyro X=" + mLpmsBData.gyr[0] + " Y=" + mLpmsBData.gyr[1] + " Z=" + mLpmsBData.gyr[2]);
		}

		if (isGetAcceleration == true) {	// in -g  (Lo transformo en m/s^2)
			mLpmsBData.acc[0]	= convertRxbytesToFloat(o, rxBuffer)*(-9.81f); o += 4;
			mLpmsBData.acc[1]	= convertRxbytesToFloat(o, rxBuffer)*(-9.81f); o += 4;
			mLpmsBData.acc[2]	= convertRxbytesToFloat(o, rxBuffer)*(-9.81f); o += 4;
		}

		if (isGetMagnetometer == true) {
			mLpmsBData.mag[0]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
			mLpmsBData.mag[1]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
			mLpmsBData.mag[2]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
		}

		// Aqui podr�an recogerse los datos de "angular velocity"

		if (isGetQuaternion == true) {
			mLpmsBData.quat[0]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
			mLpmsBData.quat[1]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
			mLpmsBData.quat[2]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
			mLpmsBData.quat[3]	= convertRxbytesToFloat(o, rxBuffer); o += 4;
		}

		if (isGetEulerAngler == true) {  // en grados
			mLpmsBData.euler[0] = convertRxbytesToFloat(o, rxBuffer)*r2d; o += 4;
			mLpmsBData.euler[1] = convertRxbytesToFloat(o, rxBuffer)*(-r2d); o += 4;
			mLpmsBData.euler[2] = convertRxbytesToFloat(o, rxBuffer)*r2d; o += 4;
		}

		// Aqui podr�an recogerse los datos de "Linear Acceleration"

		if (isGetPressure == true) {   //lo paso de kPa a mbar
			mLpmsBData.pres = convertRxbytesToFloat(o, rxBuffer)*10f; o += 4;
		}
		// Aqui podr�an recogerse los datos de "Heave motion"
		
		if (isGetTemperature == true) {   //�C centigrade   ---NO sabia en que orden manda los datos de la temperatura (no viene en el manual de LPMS), pero se deduce "debuggeando" que vienen aqui
			mLpmsBData.temp = convertRxbytesToFloat(o, rxBuffer); o += 4;
		}

		mLpmsBData.count=receivedMeassurements;
	}




	//----------------------sendData----------------------------------
	/**
	 *  Sends data to sensor	
	 * @param address Which sensor to send data
	 * @param function	specify the package type
	 * @param length	amount of data in the rawTxData buffer
	 */
	void sendData(int address, int function, int length) {
		int txLrcCheck;

		txBuffer[0] = 0x3a;
		convertInt16ToTxbytes(address, 1, txBuffer);
		convertInt16ToTxbytes(function, 3, txBuffer);
		convertInt16ToTxbytes(length, 5, txBuffer);

		for (int i=0; i < length; ++i) {
			txBuffer[7+i] = rawTxData[i];
		}

		txLrcCheck = address;
		txLrcCheck += function;
		txLrcCheck += length;

		for (int i=0; i < length; i++) {
			txLrcCheck += (int) rawTxData[i];
		}

		convertInt16ToTxbytes(txLrcCheck, 7 + length, txBuffer);
		txBuffer[9 + length] = 0x0d;
		txBuffer[10 + length] = 0x0a;

		for (int i=0; i < 11 + length; i++) {
			Log.d(TAG, "[LpmsBThread] Sending: " + Byte.toString(txBuffer[i]));		
		}

		try {
			Log.d(TAG, "[LpmsBThread] Sending data");
			mOutStream.write(txBuffer, 0, length+11);
		} catch (Exception e) {
			Log.d(TAG, "[LpmsBThread] Error while sending data");
		}
	}
	//-----------------------------------------------------------
	/**
	 *  Sends ACK to sensor
	 */
	void sendAck() {
		sendData(0, LPMS_ACK, 0);
	}

	/**
	 *  Sends NACK to sensor
	 */
	void sendNack() {
		sendData(0, LPMS_NACK, 0);
	}
	//-----------------------------------------------------------------
	/**
	 *  Converts received 32-bit word to float values
	 * @param offset 	specify the index in the buffer
	 * @param buffer	data to convert, starting in the specified offset
	 * @return	received float
	 */
	float convertRxbytesToFloat(int offset, byte buffer[]) {
		byte[] t = new byte[4];

		for (int i=0; i<4; i++) {
			t[3-i] = buffer[i+offset];
		}

		return Float.intBitsToFloat(ByteBuffer.wrap(t).getInt(0)); 
	}

	/**
	 *  Converts received 32-bit word to int value
	 * @param offset 	specify the index in the buffer
	 * @param buffer	data to convert, starting in the specified offset
	 * @return	received int
	 */
	int convertRxbytesToInt(int offset, byte buffer[]) {
		int v;
		byte[] t = new byte[4];

		for (int i=0; i<4; i++) {
			t[3-i] = buffer[i+offset];
		}

		v = ByteBuffer.wrap(t).getInt(0);

		return v; 
	}

	/**
	 *  Converts received 16-bit word to int value
	 * @param offset 	specify the index in the buffer
	 * @param buffer	data to convert, starting in the specified offset
	 * @return	received 16-bit int
	 */
	int convertRxbytesToInt16(int offset, byte buffer[]) {
		int v;
		byte[] t = new byte[2];

		for (int i=0; i<2; ++i) {
			t[1-i] = buffer[i+offset];
		}

		v = (int) ByteBuffer.wrap(t).getShort(0) & 0xffff;

		return v; 
	}	

	/**
	 *  Converts 32-bit int value to output bytes
	 * @param v			int to convert
	 * @param offset 	specify the index in the buffer
	 * @param buffer	output data, starting in the specified offset
	 */
	void convertIntToTxbytes(int v, int offset, byte buffer[]) {
		byte[] t = ByteBuffer.allocate(4).putInt(v).array();

		for (int i=0; i<4; i++) {
			buffer[3-i+offset] = t[i];
		}
	}

	/**
	 *  Converts 16-bit int value to output bytes
	 * @param v			16 bits int to convert
	 * @param offset 	specify the index in the buffer
	 * @param buffer	output data, starting in the specified offset
	 */
	void convertInt16ToTxbytes(int v, int offset, byte buffer[]) {
		byte[] t = ByteBuffer.allocate(2).putShort((short) v).array();

		for (int i=0; i<2; i++) {
			buffer[1-i+offset] = t[i];
		}
	}	

	/**
	 *  Converts 32-bit float value to output bytes
	 * @param v			32 bits float to convert
	 * @param offset 	specify the index in the buffer
	 * @param buffer	output data, starting in the specified offset
	 */
	void convertFloatToTxbytes(float f, int offset, byte buffer[]) {
		int v = Float.floatToIntBits(f);
		byte[] t = ByteBuffer.allocate(4).putInt(v).array();

		for (int i=0; i<4; i++) {
			buffer[3-i+offset] = t[i];
		}
	}


}



//=============================================================
// Class to contain orientation data retrieved from LPMS-B
//============================================================

class LpmsBData {
	public float timeStamp; 
	public float[] gyr; // Gyroscope data in rad / s
	public float[] acc;// Accelerometer data in m/s^2
	public float[] mag;// Magnetometer data in uT
	public float[] quat;// Orientation quaternion
	public float[] euler;	// Euler angles in degrees
	public float pres; 
	public float temp;
	public long count; 


	// Initializes new object
	public LpmsBData() {
		gyr = new float[3];
		acc = new float[3];
		mag = new float[3];	
		quat = new float[4];
		euler = new float[3];
		count = 0;
		timeStamp = 0.0f;
		pres=0.0f;
		temp=0.0f;
	}

	// Copy constructor
	public LpmsBData(LpmsBData d) {
		gyr = new float[3];
		acc = new float[3];
		mag = new float[3];	
		quat = new float[4];
		euler = new float[3];

		for (int i=0; i<3; i++) gyr[i] = d.gyr[i];
		for (int i=0; i<3; i++) acc[i] = d.acc[i];
		for (int i=0; i<3; i++) mag[i] = d.mag[i];
		for (int i=0; i<4; i++) quat[i] = d.quat[i];
		for (int i=0; i<3; i++) euler[i] = d.euler[i];
		pres=d.pres;
		temp=d.temp;
		count=d.count;
	}	
}




