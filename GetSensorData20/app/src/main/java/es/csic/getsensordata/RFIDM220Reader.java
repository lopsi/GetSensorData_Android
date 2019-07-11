package es.csic.getsensordata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

/* ----------------Example of use:------------------
1) First Create an "RFIDM220Reader" object:
RFIDM220Reader mRFIDM220Reader=new RFIDM220Reader(handlerRFID,Bluetooth_MAC);
where:
 - "handlerRFID" is a "Handler" object that processes (e.g. to update your UI with RFID data) the data sent by the object in a message. 
   You have to create this code: for.example:
   Handler handlerRFID=new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle data=msg.getData();
			String mensajetype=data.getString("MensajeType");
			String readername=data.getString("ReaderName");
			int RSS_A=data.getInt("RSS_A");
			int RSS_B=data.getInt("RSS_B");
			long TagID=data.getLong("TagID");
			// Do something with this data (e.g. update your UI)
			}
	}
 - "Bluetooth_MAC": is the MAC address of the RFID reader
2) Connect a socket to the Bluetooth RFID reader device
mRFIDM220Reader.connect();
3) Start reading the RFID reader (put the reader in Measurement mode):
mRFIDM220Reader.startreading();
4) Now all the processing of data is done in the handlerRFID (in your activity)
5) When you do not need the RFID reader anymore stop and disconnect it:
mRFIDM220Reader.stopreading();
mRFIDM220Reader.disconnect();
 
 */

public class RFIDM220Reader extends Thread{
	
	// Standard Bluetooth serial protocol UUID
	final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");	

	private static String TAG="RFIDM220Reader";
	private BluetoothAdapter bluetooth;  
	private BluetoothDevice RFID_BluetoothDevice;
	private BluetoothSocket mSocket;
	private InputStream mInStream;
	private OutputStream mOutStream;
	private Thread ConnectThread;
	private Thread ReadingThread;
	private Handler handlerRFID;
	private String RFID_bluetoothName;
	private UUID uuid;
	private Boolean uuid_obtained=false;
	private Boolean en_ReadingRFID_Thread=false;
	Boolean socket_connected=false;
	private Boolean imposible_conectarme=false;


	//----------------- Constructor---------------------
	RFIDM220Reader(Handler handlerRFID,String RFIDBluetooth_MAC){
		this.handlerRFID= handlerRFID;
		bluetooth = BluetoothAdapter.getDefaultAdapter();
		if (bluetooth.isEnabled()==true)
		{
			RFID_BluetoothDevice =	bluetooth.getRemoteDevice(RFIDBluetooth_MAC);  
			RFID_bluetoothName = RFID_BluetoothDevice.getName();
			if ( RFID_BluetoothDevice.getBondState()==BluetoothDevice.BOND_BONDED )
			{
				ParcelUuid[] uuids=RFID_BluetoothDevice.getUuids();
				if (uuids.length>0)
				{
					uuid=uuids[0].getUuid();
					Log.i(TAG,"Object RFIDReader Created. Got UUID");
					uuid_obtained=true;
				}
			}
		}
		else
		{
			System.out.println("RFIDM220Reader Error: Bluetooth not activated");
		}
	}


	//----------------Connect-----------------
	public void connect(){
		if (uuid_obtained)
		{
			try {
				// Forma como lo hago yo, y que funciona perfectamente en el S3
			//mSocket = RFID_BluetoothDevice.createRfcommSocketToServiceRecord(uuid);
			mSocket = RFID_BluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
				
				// Forma como lo hace Francisco para el LPMS-B:
			//	mSocket = RFID_BluetoothDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
				
				// Segun un foro: http://stackoverflow.com/questions/12274210/android-bluetooth-spp-with-galaxy-s3
				// Hay un bug de Android con el Galaxy S3 que dificulta la conexion de Bluetooth.
				// Lo resuelven asi:
				//device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
				//Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
				//socket = (BluetoothSocket)m.invoke(device, Integer.valueOf(1)); 
				
			//	Method m = RFID_BluetoothDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
			//	mSocket = (BluetoothSocket)m.invoke(RFID_BluetoothDevice, Integer.valueOf(1));
				
				System.out.println("RFIDM220Reader OK: Socket created");
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			ConnectThread = new Thread(new Runnable() {
				public void run() 
				{
					try {
						// Block until server connection accepted.
						bluetooth.cancelDiscovery();  // Lo recomiendan antes de llamar a "connect"
						mSocket.connect();  // Blocking function
						socket_connected=true;
						System.out.println("RFIDM220Reader OK: Socket connected");
						// Enviar notificacion al Handler encargado de pintar en UI
						Message mensaje= new Message();
						Bundle data = new Bundle();
						data.putString("MensajeType","Connect");
						data.putString("ReaderName",RFID_bluetoothName);
						data.putBoolean("Connected",true);
						mensaje.setData(data);
						handlerRFID.sendMessage(mensaje);
					} catch (IOException e) {
						//System.out.println("RFIDM220Reader ERROR: Socket NOT connected");
						//Log.e(TAG, "ERROR: Socket NOT connected", e);  // Saca el mensaje en texto rojo en "LogCat"
						Log.e(TAG, "ERROR: Socket NOT connected");
						imposible_conectarme=true;
					}
				}
			});
			if (ConnectThread.isAlive()==false)
			{
				ConnectThread.setName("Hilo ConnectThread - RFIDM220Reader");
				ConnectThread.start();
			}
		}
		else {
			System.out.println("RFIDM220Reader Error: No UUID obtained");
		}
	}


	//------------StartReading---------------------
	public void startreading(){
		if (uuid_obtained)
		{
			ReadingThread = new Thread(new Runnable() {
				public void run() 
				{
					en_ReadingRFID_Thread=true;
					try {
						// Put RFID reader in measurement mode
						// Configurar lector RFID
						while (socket_connected==false  && en_ReadingRFID_Thread==true  && imposible_conectarme==false)
						{ 
							Log.i(TAG,"=============Loop====================="+RFID_bluetoothName);
							Thread.sleep(1000);
						} // Esperar a que el socket (en "ConnectThred") se conecte de verdad
						Log.i(TAG,"=============Start Reading====================="+RFID_bluetoothName);				

						if (socket_connected)
						{
							// Asignar los Streams de entrada y salida:
							try {
								mInStream = mSocket.getInputStream();
								mOutStream = mSocket.getOutputStream();
							} catch (IOException e) {
								Log.e("MIMU22BT", "Streams not created", e);
								return;
							}

							Log.i(TAG,"Config M220 reader"+RFID_bluetoothName);
							SendDataSocket("M,0\r");
							SendDataSocket("M,0\r");
							SendDataSocket("G,LOCATE,4\r");
							SendDataSocket("S,2\r");
							//	SendDataSocket("YT,20\r");
							Log.i(TAG,"Put RFID reader in measurement mode"+RFID_bluetoothName);
							SendDataSocket("M,433\r");
							SendDataSocket("M,433\r");

							// Listen the Incoming DATA
							Log.i(TAG,"Start ListenforMessages"+RFID_bluetoothName);
							listenForDataRFID();   // Blocking or very long (infinite) process
							Log.i(TAG,"End ListenforMessages"+RFID_bluetoothName);
						}
					} catch (Exception e) {
						Log.e(TAG, "listenForDataRFID Exception", e);
					}
				}
			});  // end-thread

			if (ReadingThread.isAlive()==false)
			{
				ConnectThread.setName("Hilo ReadingThread - RFIDM220Reader");
				ReadingThread.start();
			}
		}
	}

	//----------------SendDatatoSocket----------------
	private void SendDataSocket(String comando_str)
	{
		byte[] buffer=comando_str.getBytes();
		try {

			mOutStream.write(buffer);
			Log.i(TAG, "Data "+ comando_str+" sent to socket.");
		} catch (IOException e) {
			Log.i(TAG, "Data Send failed.", e);
		}
	}

	//------------------------listenForDataRFID-----------------------------------	
	private void listenForDataRFID()
	{
		byte mibyte;
		int byteLeido;
		Boolean sincronizado=false;
		String linea="";
		try {

			while (en_ReadingRFID_Thread) 
			{
				byteLeido = mInStream.read();  // Leer 1 solo byte ( Blocking function )
				mibyte=(byte) byteLeido;
				if (sincronizado==false && mibyte==13 )
				{ // Resetear la linea y empezar a recoger lineas
					linea="";
					sincronizado=true;
					Log.i("ListenRFID","Linea sincronizada");
				}
				else
				{
					if (sincronizado==true)
					{
						if (mibyte!=13)
						{ // Estoy en linea, la continuo rellenando
							linea=linea+(char)mibyte;	
						}
						else
						{ // encontrará el final de linea => parsear
							//Log.i("ListenRFID","Linea leida: "+linea);
							Bundle data=parsear_linea_RFID(linea);   // .......PARSEAR......data contiene los datos RSS_A, RSS_B y TagID...
							int RSS_A=data.getInt("RSS_A");
							int RSS_B=data.getInt("RSS_B");
							long TagID=data.getLong("TagID");

							// Enviar datos RFID a Handler en hilo UI para que los pinte
							Log.i("ListenRFID","Tag ID: "+TagID+" RSS_A: "+RSS_A+" RSS_B: "+RSS_B);
							Message mensaje= new Message();
							data.putString("MensajeType","RFID_Data");
							data.putString("ReaderName",RFID_bluetoothName);
							mensaje.setData(data);
							handlerRFID.sendMessage(mensaje);
							// reseteo linea para coger la siguiente
							linea="";  
						}
					}
				}
			}  // end-while
			Log.i("ListenRFID","Salgo de LOOP listenForMessages");
		} catch (IOException e) {
			Log.i(TAG, "Message received failed.", e);
		}
	}

	//-----------------------------parsear_linea_RFID---------------------
	private Bundle parsear_linea_RFID(String linea)   //,long ID, int RSS_A, int RSS_B)
	{
		Boolean linea_erronea=false;
		Bundle bundle = new Bundle();

		// Procesamos la línea
		int posH=linea.indexOf("H,");
		int posG=linea.indexOf(",GLOCATE,");
		int posP=linea.toUpperCase().indexOf(",P"); // a veces viene P y otras p
		int posA=linea.indexOf(",A");
		int posB=linea.indexOf(",B");

		// Varias comprobaciones
		if( linea.length()<23 ||  posH!=0  || posH==-1 || posG==-1 || posP==-1 || (posA==-1 && posB==-1))
		{
			linea_erronea=true;  // continue: "Pass control to next iteration of for or while loop"
		}

		if (linea_erronea==false)
		{
			// Extraemos el tag ID
			long tagID=0;
			try{
			   tagID=Long.parseLong(linea.substring(posH+2, posH+10));
			} catch (Exception e) {};

			int SS1=-1; // -1 indica que no hay lectura RSS válida
			int SS2=-1;
			if(posA!=-1) // si valor A -> hay numero de 2 o 3 cifras (entre 40 y 120 dB),(si 3 cifras el primero es un 1; ciento y pico)
			{
				if ( (linea.charAt(posA+2))=='1'   ) // leo numero de 3 cifras
				{
					try{
						SS1=Integer.parseInt(linea.substring(posA+2, posA+5)); //SS1
				       } catch (Exception e) {};
				} else                         // leo numero de 2 cifras
				{
					try {
					SS1=Integer.parseInt(linea.substring(posA+2, posA+4)); //SS1
					} catch (Exception e) {};
				}
			}
			else
			{	SS1=-1;
			}

			if(posB!=-1)
			{
				if ( (linea.charAt(posB+2))=='1'   ) // leo numero de 3 cifras
				{
					try{
					SS2=Integer.parseInt(linea.substring(posB+2, posB+5)); //SS2
					} catch (Exception e) {};
				} else                         // leo numero de 2 cifras
				{
					try {
					SS2=Integer.parseInt(linea.substring(posB+2, posB+4)); //SS2
					} catch (Exception e) {};
				}
			}
			else
			{	SS2=-1;
			}

			// última comprobación
			if (SS1 >0 || SS2 >0)
			{
				bundle.putInt("RSS_A", SS1);
				bundle.putInt("RSS_B", SS2);
				bundle.putLong("TagID", tagID);
			}

		}
		return bundle;
	}

	//-------------------StopReading--------------------------
	public void stopreading(){
		en_ReadingRFID_Thread=false;  // para indicarle que salga del loop en el thread "ReadingRFID"
		if (socket_connected)
		{
			try {
				if (ReadingThread.isAlive())
				{
				ReadingThread.interrupt();
				}
				Log.i(TAG,"StopReading: ReadingThread interrupted");
			} catch (Exception e) {};
			
			Log.i(TAG,"StopReading: Send M,0");
			SendDataSocket("M\r");
			SendDataSocket("M,0\r");
		}
		else
		{
			System.out.println("RFIDM220Reader. Info: No stopping done since it was not connected nor reading");
		}
	}

	//-------------Disconnect-----------------------------
	public void disconnect(){
		if (socket_connected)
		{
			try {
				if (ConnectThread.isAlive())
				{
				ConnectThread.interrupt();
				}
				Log.i(TAG,"disconnect: ConnectThread interrupted");
			} catch (Exception e) {};

			try {
				Log.i(TAG,"INI:Socket closed on disconnetct method");
				if (mSocket.isConnected())
				{
					mSocket.close();
				}
				Log.i(TAG,"END:Socket closed on disconnetct method");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("RFIDM220Reader. Info: No disconnection done since it was not connected");
		}

	}
	//---------
}
