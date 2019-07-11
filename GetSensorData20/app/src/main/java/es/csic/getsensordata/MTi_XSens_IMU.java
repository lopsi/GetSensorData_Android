/**
 * 
 */
package es.csic.getsensordata;

import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.PendingIntent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 
 * Class that handles the USB communication and parse of the data coming from an MTi IMU from XSens
 * The IMU must be previously configured to submit, Temperature, Acc, Gyr, Mag, Quaternions 
 * (all in IEEE single precision float) and a 2 bytes counter (in total 58 bytes of information).
 * It has been tested at 100 Hz without lost information.
 * Other configurations and support for other devices will be added in the future.
 * 
 *     For accessing the MTi it is necessary to:
 *     - Create a handler to manage the messages from the class
 * 		    private void createMTiHandler(){
				handlerMTi=new Handler() {
					
					@Override
					public void handleMessage(Message msg) {
						 * handle MTi data and connection messages
						Bundle data=msg.getData();
						String mensajetype=data.getString("MensajeType");
						String readername=data.getString("ReaderName");
						if ( mensajetype.equals("Connect") ){...
							dataIMU.acceleration[0]=data.getFloat("Accelerations_x");
							dataIMU.acceleration[1]=data.getFloat("Accelerations_y");
							dataIMU.acceleration[2]=data.getFloat("Accelerations_z");
							dataIMU.turnRate[0]=data.getFloat("TurnRates_x");
							dataIMU.turnRate[1]=data.getFloat("TurnRates_y");
							dataIMU.turnRate[2]=data.getFloat("TurnRates_z");
							dataIMU.magneticField[0]=data.getFloat("MagneticFields_x");
							dataIMU.magneticField[1]=data.getFloat("MagneticFields_y");
							dataIMU.magneticField[2]=data.getFloat("MagneticFields_z");
							dataIMU.temperature=data.getFloat("Temperature");
							dataIMU.counter=data.getShort("Counter");
							dataIMU.time=data.getLong("Time");
						}
					}
				};
			}
 *     - Create the UsbManager. 
 *     		manager = (UsbManager) getSystemService(Context.USB_SERVICE);
 *     - Create the request permission broadcast:
 *     		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
    		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    		registerReceiver(mUsbReceiver, filter);
    		
    		and the broadcaster:
			private static final String ACTION_USB_PERMISSION =
		    	"com.csic.xoom_usb.USB_PERMISSION";
		    	
			public final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
			    public void onReceive(Context context, Intent intent) {
			        String action = intent.getAction();
			        if (ACTION_USB_PERMISSION.equals(action)) {
			            synchronized (this) {
			                UsbDevice localdevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
		                    	//If the permission was granted
		                    }
		                    else{
		                    	//If the permission was denied 
		                    }
	                    }
                    }
                }
            };

	   - Create MTi element
	   		XSens =new MTi_XSens_IMU(handlerMTi,mPermissionIntent);
   	   - Get the Device (if permission is needed the program will call the broadcast receiver, a system call)
   	    	mUsbDevice=XSens.getDevice(manager);
   	   - Connect to the Device(if permission is needed, this step must be done after the broadcast receiver is called or it will not connect and return false)
   	   		XSens.connect(mUsbDevice); (return true if correctly connected)
   	   - Start the measurements
   	   		XSens.startMeassurements();
 *
 *
 * The Manifest must include:
 * <uses-feature android:name="android.hardware.usb.host" />
    before the uses-sdk and the min sdk must be at least 12 
 */
public class MTi_XSens_IMU  {   //extends Activity
	private static final String TAG = "MTi XSens";
	private Handler handlerMTiIMU;
	private UsbManager manager;
	private Thread ReadingThread;
	private boolean readingIMU=false;
	private String MTiIMU_Name="XSens MTi IMU";
	private UsbDevice device; 
	private UsbEndpoint out_ep,in_ep;
	private UsbInterface intf;
	private UsbDeviceConnection connection;
	private PendingIntent mPendingIntent;
	private int mVendorId=0x0403;
	private int mProductId=0x0D38B;
	private int MTiPackageSize=63;
	private int receivedPackages=0;
	private byte dataLength=(byte)(MTiPackageSize-5);
	private int maxPackages=100*1*1;	//10 minutes data, 1 seg
	private short temperatureIndex=4;
	private short accelerationIndex=8;
	private short turnRateIndex=20;
	private short magneticFieldIndex=32;
	private short quaternionsIndex=44;
	private short counterIndex=60;
	private int maxErrors=30;
	private IMUdata[] receivedData;
	
	public static byte preamble=(byte)0xFA;
	public static byte BID=(byte)0xFF;
	public static byte MID_MTData=50;
	public static byte MID_goToConfig=48;
	public static byte MID_goToMeasurement=16;
	public static byte MID_reqDID=00;
	public static byte MID_reqProductCode=28;
	public static byte MID_reqFWRev=18;
	public static byte MID_reqDataLength=10;
	public static byte MID_reqBaudrate=24;
	public static byte MID_SetPeriod=4;
	public static byte MID_SetOutputMode=(byte) 208;
	public static byte MID_SetOutputSettings=(byte) 210;
	
	public static final byte Baudrate_921k6=(byte)0x80;
	public static final byte Baudrate_460k8=(byte)0x00;
	public static final byte Baudrate_230k4=(byte)0x01;
	public static final byte Baudrate_115k2=(byte)0x02;
	public static final byte Baudrate_76k8=(byte)0x03;
	public static final byte Baudrate_57k6=(byte)0x04;
	public static final byte Baudrate_38k4=(byte)0x05;
	public static final byte Baudrate_28k8=(byte)0x06;
	public static final byte Baudrate_19k2=(byte)0x07;
	public static final byte Baudrate_14k4=(byte)0x08;
	public static final byte Baudrate_9k6=(byte)0x09;
	
	
/**
 * Starts the MTi class, it does not request the permission to the USB Device, it must completely me implemented in the main
 * 	
 * @param handlerIMU
 * @param maxPackages
 */
	public MTi_XSens_IMU(Handler handlerIMU, int maxPackages){
		this.mPendingIntent=null;
		this.handlerMTiIMU=handlerIMU;
		this.maxPackages=maxPackages;
		this.receivedData = new IMUdata[maxPackages]; 

	}

	/** Starts the MTi class
	 * 
	 * @param handlerIMU handler for receiving the messages from the class 
	 * @param mPermissionIntent	Pending intent for handling the permission request (the broadcast must be implemented in the main)
	 */
	public MTi_XSens_IMU(Handler handlerIMU, PendingIntent mPermissionIntent){
		this.mPendingIntent=mPermissionIntent;
		this.handlerMTiIMU=handlerIMU;
		this.receivedData = new IMUdata[this.maxPackages]; 

	}
	
	/**
	 * 
	 *	Scan for the MTi IMU, if it is detected,  request permission to the UsbDevice (a broadcast receiver must be implemented in the main) and return the UsbDevice, null if not detected. 
	 * 
	 * @param mmanager Must provide the UsbManager: UsbManager mmanager = (UsbManager) getSystemService(Context.USB_SERVICE)
	 */
	public UsbDevice getDevice(UsbManager mmanager){
		
		manager = mmanager;

		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
    	while(deviceIterator.hasNext()){
    		Log.i(TAG, "USB device detected");
    		UsbDevice device = deviceIterator.next();
		    if(device.getVendorId()==this.mVendorId & device.getProductId()==this.mProductId){
		    	if(mPendingIntent!=null)
		    		manager.requestPermission(device, mPendingIntent);
		    	return device;
		    }
    	}
    	Log.e(TAG, "USB not connected");
    	return null;
	}

	/**
	 * Initiate connection with the device, setting the data rate and sending a message to 
	 * the handler indicating the connection ("MensajeType"->"Connect", "ReaderName"->"XSens MTi IMU", "Connected"->true)
	 * @param UsbDevice of the MTi
	 * @return	true if connected, false otherwise
	 */
	public boolean connect(UsbDevice localdevice){
		device=localdevice;
		int count = device.getInterfaceCount();
		for(int ii=0;ii<count;ii++){
			intf=device.getInterface(ii);
			int intf_count=intf.getEndpointCount();
			for(int jj=0; jj<intf_count;jj++){
				if (intf.getEndpoint(jj).getDirection()==UsbConstants.USB_DIR_OUT){
					out_ep=intf.getEndpoint(jj);
				}else{
					in_ep=intf.getEndpoint(jj);
				}
			}
		}
		connection = manager.openDevice(device);
		if(connection==null)
			return false;
		
		if (connection.claimInterface( intf, false)) {
			int sents[]=new int[4];
			sents[0]= connection.controlTransfer(0x40, 0, 0, 0, null, 0, 0);// reset
			sents[1]= connection.controlTransfer(0x40, 0, 1, 0, null, 0, 0);// clear Rx
			sents[2]= connection.controlTransfer(0x40, 0, 2, 0, null, 0, 0);// clear Tx
			sents[3]= connection.controlTransfer(0x40, 0x03, 0x001A, 0, null, 0, 0);//115200 baud rate
			Message mensaje= new Message();
			Bundle data = new Bundle();
			data.putString("MensajeType","Connect");
			data.putString("ReaderName",MTiIMU_Name);
			data.putBoolean("Connected",true);
			mensaje.setData(data);
			handlerMTiIMU.sendMessage(mensaje);
			Log.i(TAG, "USB MTi connected");
			return true;
		}
		return false;
	}


	
	/**
	 * Starts a thread that will send the received IMU packages to the specified handler
	"MensajeType"->"IMU_Data"
	"ReaderName"->"XSens MTi IMU"
	"Time"-> System time in nanoseconds (long)
	"Accelerations_x"-> float
	"Accelerations_y"-> float
	"Accelerations_z"-> float
	"TurnRates_x"-> float
	"TurnRates_y"-> float
	"TurnRates_z"-> float
	"MagneticFields_x"-> float
	"MagneticFields_y"-> float
	"MagneticFields_z"-> float
	"Temperature"-> float
	"Counter"-> MTi internal counter (short)
	
	If it receive more than 30 packages without information it assume that the MTi is disconnected and sends a message to the handler("MensajeType"->"MTiConnect", "Connected"->false).
	
	If the USB connection is interrupted it sends a message to the handler("MensajeType"->"Connect", "Connected"->false)
	 */
	public void startMeassurements(){
		
		this.ReadingThread = new Thread(new Runnable() {
			public void run() 
			{
				readingIMU=true;
				byte msg_buffer[] = new byte[64];
		        int in_count,roundCounter=0;
		        byte MTi_pkg[] = new byte[MTiPackageSize*2];
		        for(int ii=0;ii<MTiPackageSize*2;ii++)
		        	MTi_pkg[ii]=0;
		        int loops_counts=0;
		        
		        do{
		        	in_count = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);  // Leer los datos por USB provenientes del dispositivo externo
		        	IMUdata MTiBlock=null;
		        	if((msg_buffer[1]&0x02)==0 & in_count>2)  // compruebo que son datos frescos
			        	for(int i=2;i<in_count;i++){
			        		MTi_pkg[roundCounter]=msg_buffer[i];
			        		
			        		if (MTi_pkg[roundCounter]==preamble){
			        			if(MTi_pkg[(roundCounter+MTiPackageSize+1)%(MTiPackageSize*2)]==BID &
			        			   MTi_pkg[(roundCounter+MTiPackageSize+2)%(MTiPackageSize*2)]==MID_MTData &
			        			   MTi_pkg[(roundCounter+MTiPackageSize+3)%(MTiPackageSize*2)]==dataLength &
			        			   MTi_pkg[(roundCounter+MTiPackageSize)%(MTiPackageSize*2)]==preamble)
			        			{
			        				MTiBlock=parse(MTi_pkg,(roundCounter+MTiPackageSize)%(MTiPackageSize*2));  // --------RAW parse---------
			        				Message menssaje= new Message();
			    					Bundle data = bundleParse(MTiBlock);   // -------- parse Bundle---------
			    					data.putString("MensajeType","IMU_Data");
			    					data.putString("ReaderName",MTiIMU_Name);
			    					data.putLong("Time", System.nanoTime());
			    					menssaje.setData(data);
			    					handlerMTiIMU.sendMessage(menssaje);		        				
			        			}
			        		}
			        		roundCounter=(roundCounter+1)%(MTiPackageSize*2);
			        	}
		        	if(MTiBlock!=null)
		        	{
		        		receivedData[(receivedPackages-1)%maxPackages]=MTiBlock;
		        		loops_counts=0;
		        	}
		        	else
		        	{	loops_counts++;}
       
		        }while(in_count>0 & loops_counts<maxErrors & readingIMU==true);
		        
		        if(in_count<=0){
		        	Message mensaje= new Message();
					Bundle data = new Bundle();
					data.putString("MensajeType","Connect");
					data.putString("ReaderName",MTiIMU_Name);
					data.putBoolean("Connected",false);
					mensaje.setData(data);
					handlerMTiIMU.sendMessage(mensaje);
					readingIMU=false;
		        }
		        if(loops_counts>=maxErrors){
		        	Message mensaje= new Message();
					Bundle data = new Bundle();
					data.putString("MensajeType","MTiConnect");
					data.putString("ReaderName",MTiIMU_Name);
					data.putBoolean("Connected",false);
					mensaje.setData(data);
					handlerMTiIMU.sendMessage(mensaje);
					readingIMU=false;
		        }
			}
		});
		if (ReadingThread.isAlive()==false)
		{
			ReadingThread.setName("Thread ReadingThread - XSens MTi IMU");
			ReadingThread.start();
		}
	}

	/**
	 * 	Stop reading the IMU
	 */
	public void stopReading(){
		readingIMU=false;  // indicates the IMUreading thread to stop reading from the USB
		try {
			if (ReadingThread.isAlive()){
			ReadingThread.interrupt();
			}
		} catch (Exception e) {};
		
	}
	
	
	/**
	 * Indicates that the reading thread is alive 
	 * @return true if the reading thread is alive
	 */
	public boolean isReading(){
		return readingIMU;
	}
	
	/**
	 * Release the interface and the USB device connection
	 */
	public void disconnect(){
		if (connection!=null)
		{
    	connection.releaseInterface(intf);
        connection.close();
		}

	}

	/**
	 * Enter the configuration mode and interrupt the reading thread if any 
	 * @return integer array of received bytes from the IMU
	 */
	public int[] goToConfig(){
		byte[] msg_buffer=new byte[64];
		byte[] out_buffer={preamble,BID,MID_goToConfig,0,0};
		int[] out=new int[5];
		int loops=0,out_l=0;
		

		out_buffer[4]=crc_calculation(out_buffer,out_buffer.length);
		msg_buffer[1]=2;
		if(readingIMU==false)
			while(connection.bulkTransfer(in_ep, msg_buffer, 64, 1000)>=2 & (msg_buffer[1]&0x02)>0); //Clean the buffer
		connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
		connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
		do{
			connection.bulkTransfer(out_ep, out_buffer, out_buffer.length, 1000);  // Mandar comando de pasar a modo configuracion
			if(readingIMU==true)
			{
				while(readingIMU==true);
			}
			else
			{
				out_l = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
				for(int i=0;i<out_l-2&i<5;i++)
					out[i]=((int)msg_buffer[i+2])&0x00FF;
			}
			loops++;
		}while(out_l!=7 & loops<100);
		return out;
	}

	/**
	 * Enter the Measurement state
	 * 
	 * @return Integer array of the received bytes from the IMU
	 */
	public int[] goToMeasurement(){
		byte[] out_buffer={preamble,BID,MID_goToMeasurement,0,0}, msg_buffer=new byte[64];
		int out_l,receiveCount=0;
		int[] out=new int[5];
		
		out_buffer[4]=crc_calculation(out_buffer,out_buffer.length);
		
		connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
		connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
		if(connection.bulkTransfer(out_ep, out_buffer, out_buffer.length, 1000)<out_buffer.length)  // mandar el comando poner en modo medida
			return null;
		do{
			out_l = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
			receiveCount++;
		}while(out_l<=2 & receiveCount<9);
		if(out_l<=0)
			return null;
		
		for(int i=0;i<out_l-2&i<5;i++)
			out[i]=((int)msg_buffer[i+2])&0x00FF;
		return out;
	}


	/**
	 * Request the Device ID of the IMU
	 * 
	 * @return Received Device ID as a byte array
	 */
	public byte[] reqDID(){
		byte[] msg_buffer=new byte[64];
		byte[] out_buffer={preamble,BID,MID_reqDID,0,0};
		byte[] out=new byte[4];
		int in_count=0,loops_counts=0,out_loop=0;
		
		
		out_buffer[4]=crc_calculation(out_buffer,out_buffer.length);
		do{
			connection.bulkTransfer(out_ep, out_buffer, out_buffer.length, 1000);
			if(readingIMU==true){
				return null;
			}
			else{
				loops_counts=0;
		        do{
		        	in_count = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
		        	if(in_count>=11){
		        		byte sum=msg_buffer[3];
		        		for(int i=4;i<11;i++)
		        			sum+=msg_buffer[i];
		        		if (msg_buffer[2]==preamble&msg_buffer[3]==BID&msg_buffer[4]==(MID_reqDID+1)&msg_buffer[5]==4&sum==0){//&((int)msg_buffer[3]+(int)msg_buffer[4]+(int)msg_buffer[5]+(int)msg_buffer[6]+(int)msg_buffer[7]+(int)msg_buffer[8]+msg_buffer[9]+msg_buffer[10])==0){
		        			out[0]=msg_buffer[6];
		        			out[1]=msg_buffer[7];
		        			out[2]=msg_buffer[8];
		        			out[3]=msg_buffer[9];
		        			return out;
		        		}	        			
		        		loops_counts=0;
		        	}else
		        		loops_counts++;
	            }while(in_count>0 & loops_counts<10);
			}
			out_loop++;
		}while(out_loop<3);
		return msg_buffer;
	}

	/**
	 * Request the product code of the IMU
	 * 
	 * @return The received product code of the IMU as a char array, use String.copyValueOf(char[]) 
	 * to generate the String 
	 */
	public char[] reqProductCode(){
		byte[] msg_buffer=new byte[64];
		byte[] out_buffer={preamble,BID,MID_reqProductCode,0,0};
		int in_count=0,loops_counts=0,out_loop=0;
		
		
		out_buffer[4]=crc_calculation(out_buffer,out_buffer.length);
		do{
			connection.bulkTransfer(out_ep, out_buffer, out_buffer.length, 1000);
			if(readingIMU==true){
				return null;
			}
			else{
				loops_counts=0;
		        do{
		        	in_count = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
		        	if(in_count>=7){
		        		byte sum=msg_buffer[3];
		        		for(int i=4;i<in_count;i++)
		        			sum+=msg_buffer[i];
		        		if (msg_buffer[2]==preamble&msg_buffer[3]==BID&msg_buffer[4]==(MID_reqProductCode+1)&msg_buffer[5]==in_count-7&sum==0){
			        		char[] out=new char[in_count-7];
		        			for(int i=6;i<in_count-1;i++)
			        			out[i-6]=(char)(((int)msg_buffer[i])&0x00FF);
		        			return out;
		        		}	        			
		        		loops_counts=0;
		        	}else
		        		loops_counts++;
	            }while(in_count>0&loops_counts<10);
			}
			out_loop++;
		}while(out_loop<3);
		return null;
	}

	/**
	 * Request the FirmWare Revision
	 * 
	 * @return 3 byte array containing the FirmWare Revision (Major,Minor,Revision part)
	 */
	public byte[] reqFWRev(){
		byte[] msg_buffer=new byte[64];
		byte[] out_buffer={preamble,BID,MID_reqFWRev,0,0};
		int in_count=0,loops_counts=0,out_loop=0;
		
		
		out_buffer[4]=crc_calculation(out_buffer,out_buffer.length);
		do{
			connection.bulkTransfer(out_ep, out_buffer, out_buffer.length, 1000);
			if(readingIMU==true){
				return null;
			}
			else{
				loops_counts=0;
		        do{
		        	in_count = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
		        	if(in_count>=7){
		        		byte sum=msg_buffer[3];
		        		for(int i=4;i<in_count;i++)
		        			sum+=msg_buffer[i];
		        		if (msg_buffer[2]==preamble&msg_buffer[3]==BID&msg_buffer[4]==(MID_reqFWRev+1)&msg_buffer[5]==in_count-7&sum==0){
			        		byte[] out=new byte[in_count-7];
		        			for(int i=6;i<in_count-1;i++)
			        			out[i-6]=msg_buffer[i];
		        			return out;
		        		}	        			
		        		loops_counts=0;
		        	}else
		        		loops_counts++;
	            }while(in_count>0&loops_counts<10);
			}
			out_loop++;
		}while(out_loop<3);
		return msg_buffer;
	}

	/**
	 * Request the Data length of the MTData message (without headers or crc)
	 * 
	 * @return Data length (0 if error)
	 */
	public int reqDataLength(){
		byte[] msg_buffer=new byte[64];
		byte[] out_buffer={preamble,BID,MID_reqDataLength,0,0};
		int in_count=0,loops_counts=0,out_loop=0;
		
		
		out_buffer[4]=crc_calculation(out_buffer,out_buffer.length);
		do{
			connection.bulkTransfer(out_ep, out_buffer, out_buffer.length, 1000);
			if(readingIMU==true){
				return 0;
			}
			else{
				loops_counts=0;
		        do{
		        	in_count = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
		        	if(in_count>=7){
		        		byte sum=msg_buffer[3];
		        		for(int i=4;i<in_count;i++)
		        			sum+=msg_buffer[i];
		        		if (msg_buffer[2]==preamble&msg_buffer[3]==BID&msg_buffer[4]==(MID_reqDataLength+1)&msg_buffer[5]==in_count-7&sum==0){
			        		return ((((int)msg_buffer[6]&0x00FF)<<8)+((int)msg_buffer[7]&0x00FF));
		        		}	        			
		        		loops_counts=0;
		        	}else
		        		loops_counts++;
	            }while(in_count>0&loops_counts<10);
			}
			out_loop++;
		}while(out_loop<3);
		return 0;
	}
	
	/**
	 * Request the baudrate of the communication
	 * 
	 * @return code of the active baudrate (example Baudrate_115k2) -1 if error
	 */
	public byte reqBaudrate(){
		byte[] msg_buffer=new byte[64];
		byte[] out_buffer={preamble,BID,MID_reqBaudrate,0,0};
		int in_count=0,loops_counts=0,out_loop=0;
		
		
		out_buffer[4]=crc_calculation(out_buffer,out_buffer.length);
		do{
			connection.bulkTransfer(out_ep, out_buffer, out_buffer.length, 1000);
			if(readingIMU==true){
				return -1;
			}
			else{
				loops_counts=0;
		        do{
		        	in_count = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
		        	if(in_count>=7){
		        		byte sum=msg_buffer[3];
		        		for(int i=4;i<in_count;i++)
		        			sum+=msg_buffer[i];
		        		if (msg_buffer[2]==preamble&msg_buffer[3]==BID&msg_buffer[4]==(MID_reqBaudrate+1)&msg_buffer[5]==in_count-7&sum==0){
			        		return msg_buffer[6];
		        		}	        			
		        		loops_counts=0;
		        	}else
		        		loops_counts++;
	            }while(in_count>0&loops_counts<10);
			}
			out_loop++;
		}while(out_loop<3);
		return -1;
	}

	/**
	 * Send a message to the IMU
	 * @param MID Message to send
	 * @return received MTComm message (up to 57 data bytes)
	 */
	public byte[] writeMessage(byte MID){
		byte[] msg_buffer=new byte[64];
		byte[] out_buffer={preamble,BID,MID,0,0};
		int in_count=0,loops_counts=0,out_loop;
		
		
		out_buffer[4]=crc_calculation(out_buffer,out_buffer.length);
		out_loop=0;
		do{
			if(connection.bulkTransfer(out_ep, out_buffer, out_buffer.length, 1000)<0)
				return null;
			loops_counts=0;
	        do{
	        	in_count = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
	        	if(in_count>=7){
	        		byte sum=msg_buffer[3];
	        		for(int i=4;i<in_count;i++)
	        			sum+=msg_buffer[i];
	        		if (msg_buffer[2]==preamble&msg_buffer[3]==BID&msg_buffer[4]==(MID+1)&msg_buffer[5]==in_count-7&sum==0){
		        		byte[] out=new byte[in_count-2];
	        			for(int i=2;i<in_count;i++)
		        			out[i-2]=msg_buffer[i];
	        			return out;
	        		}	        			
	        		loops_counts=0;
	        	}else
	        		loops_counts++;
            }while(in_count>0&loops_counts<10);
			
			out_loop++;
		}while(out_loop<3);
		return msg_buffer;
	}

	/**
	 * Send a message with a Data 
	 * @param MID	Message ID
	 * @param Data	Data to be send with the MID
	 * @return	Received MTComm message
	 */
	public byte[] Setting(byte MID,byte[] Data){
		byte[] msg_buffer=new byte[64];
		byte[] out_buffer=new byte[5+Data.length];
		int in_count=0,loops_counts=0,out_loop;

		out_buffer[0]=preamble;
		out_buffer[1]=BID;
		out_buffer[2]=MID;
		if(Data.length>59)
			return null;
		out_buffer[3]=(byte)(Data.length&0x00FF);
		for(int i=0;i<Data.length;i++)
			out_buffer[i+4]=Data[i];
		//{preamble,BID,MID,Data.length,Data[],0};
		
		out_buffer[out_buffer.length-1]=crc_calculation(out_buffer,out_buffer.length);
		out_loop=0;
		do{
			if(connection.bulkTransfer(out_ep, out_buffer, out_buffer.length, 1000)<0)  // mandar comando con datos
				return null;
			loops_counts=0;
	        do{
	        	in_count = connection.bulkTransfer(in_ep, msg_buffer, 64, 1000);
	        	if(in_count>=7){
	        		byte sum=msg_buffer[3];
	        		for(int i=4;i<in_count;i++)
	        			sum+=msg_buffer[i];
	        		if (msg_buffer[2]==preamble&msg_buffer[3]==BID&msg_buffer[4]==(MID+1)&msg_buffer[5]==in_count-7&sum==0){
		        		byte[] out=new byte[in_count-2];
	        			for(int i=2;i<in_count;i++)
		        			out[i-2]=msg_buffer[i];
	        			return out;
	        		}	        			
	        		loops_counts=0;
	        	}else
	        		loops_counts++;
            }while(in_count>0&loops_counts<10);
			
			out_loop++;
		}while(out_loop<3);
		return msg_buffer;
	}

	/**
	 * 
	 * @param Circular buffer array with the information to interpret
	 * @param Position of the first byte of the package
	 * @return Received information
	 */
	private IMUdata parse(byte[] msg,int pointer) {
		byte MTData=0x32;	//Message identifier for a MT data
		
		IMUdata receivedMessage=new IMUdata();
		byte sum=0;
		byte Package[]=new byte[MTiPackageSize];
		Package[0]=preamble;
		for(int i=1;i<MTiPackageSize;i++){
			sum+=msg[(pointer+i)%(MTiPackageSize*2)];
			Package[i]=msg[(pointer+i)%(MTiPackageSize*2)];
		}
		if (sum==0){
			if(Package[2]==MTData){	//Data message
				receivedMessage.acceleration[0]=Float.intBitsToFloat((((int)Package[accelerationIndex])<<24)+((((int)Package[accelerationIndex+1])&0x00FF)<<16)+((((int)Package[accelerationIndex+2])&0x00FF)<<8)+(((int)Package[accelerationIndex+3])&0x00FF));
				receivedMessage.acceleration[1]=Float.intBitsToFloat((((int)Package[accelerationIndex+4])<<24)+((((int)Package[accelerationIndex+5])&0x00FF)<<16)+((((int)Package[accelerationIndex+6])&0x00FF)<<8)+(((int)Package[accelerationIndex+7])&0x00FF));
				receivedMessage.acceleration[2]=Float.intBitsToFloat((((int)Package[accelerationIndex+8])<<24)+((((int)Package[accelerationIndex+9])&0x00FF)<<16)+((((int)Package[accelerationIndex+10])&0x00FF)<<8)+(((int)Package[accelerationIndex+11])&0x00FF));
				receivedMessage.turnRate[0]=Float.intBitsToFloat((((int)Package[turnRateIndex])<<24)+((((int)Package[turnRateIndex+1])&0x00FF)<<16)+((((int)Package[turnRateIndex+2])&0x00FF)<<8)+(((int)Package[turnRateIndex+3])&0x00FF));
				receivedMessage.turnRate[1]=Float.intBitsToFloat((((int)Package[turnRateIndex+4])<<24)+((((int)Package[turnRateIndex+5])&0x00FF)<<16)+((((int)Package[turnRateIndex+6])&0x00FF)<<8)+(((int)Package[turnRateIndex+7])&0x00FF));
				receivedMessage.turnRate[2]=Float.intBitsToFloat((((int)Package[turnRateIndex+8])<<24)+((((int)Package[turnRateIndex+9])&0x00FF)<<16)+((((int)Package[turnRateIndex+10])&0x00FF)<<8)+(((int)Package[turnRateIndex+11])&0x00FF));
				receivedMessage.magneticField[0]=Float.intBitsToFloat((((int)Package[magneticFieldIndex])<<24)+((((int)Package[magneticFieldIndex+1])&0x00FF)<<16)+((((int)Package[magneticFieldIndex+2])&0x00FF)<<8)+(((int)Package[magneticFieldIndex+3])&0x00FF));
				receivedMessage.magneticField[1]=Float.intBitsToFloat((((int)Package[magneticFieldIndex+4])<<24)+((((int)Package[magneticFieldIndex+5])&0x00FF)<<16)+((((int)Package[magneticFieldIndex+6])&0x00FF)<<8)+(((int)Package[magneticFieldIndex+7])&0x00FF));
				receivedMessage.magneticField[2]=Float.intBitsToFloat((((int)Package[magneticFieldIndex+8])<<24)+((((int)Package[magneticFieldIndex+9])&0x00FF)<<16)+((((int)Package[magneticFieldIndex+10])&0x00FF)<<8)+(((int)Package[magneticFieldIndex+11])&0x00FF));
				receivedMessage.temperature=Float.intBitsToFloat((((int)Package[temperatureIndex])<<24)+((((int)Package[temperatureIndex+1])&0x00FF)<<16)+((((int)Package[temperatureIndex+2])&0x00FF)<<8)+(((int)Package[temperatureIndex+3])&0x00FF));
				receivedMessage.quaternions[0]=Float.intBitsToFloat((((int)Package[quaternionsIndex])<<24)+((((int)Package[quaternionsIndex+1])&0x00FF)<<16)+((((int)Package[quaternionsIndex+2])&0x00FF)<<8)+(((int)Package[quaternionsIndex+3])&0x00FF));
				receivedMessage.quaternions[1]=Float.intBitsToFloat((((int)Package[quaternionsIndex+4])<<24)+((((int)Package[quaternionsIndex+5])&0x00FF)<<16)+((((int)Package[quaternionsIndex+6])&0x00FF)<<8)+(((int)Package[quaternionsIndex+7])&0x00FF));
				receivedMessage.quaternions[2]=Float.intBitsToFloat((((int)Package[quaternionsIndex+8])<<24)+((((int)Package[quaternionsIndex+9])&0x00FF)<<16)+((((int)Package[quaternionsIndex+10])&0x00FF)<<8)+(((int)Package[quaternionsIndex+11])&0x00FF));
				receivedMessage.quaternions[3]=Float.intBitsToFloat((((int)Package[quaternionsIndex+12])<<24)+((((int)Package[quaternionsIndex+13])&0x00FF)<<16)+((((int)Package[quaternionsIndex+14])&0x00FF)<<8)+(((int)Package[quaternionsIndex+15])&0x00FF));
			    receivedMessage.counter=(short)((((short)Package[counterIndex])<<8)+(((short)Package[counterIndex+1])&0x00FF));
				receivedPackages++;
			}
			return receivedMessage;
		}else
			return null;
	}

	/**
	 * 
	 * @param Data in the IMUdata class
	 * @return Data in a bundle
	 */
	private Bundle bundleParse(IMUdata data) {
		Bundle out=new Bundle();
		
		out.putFloat("Accelerations_x", data.acceleration[0]);
		out.putFloat("Accelerations_y", data.acceleration[1]);
		out.putFloat("Accelerations_z", data.acceleration[2]);
		out.putFloat("TurnRates_x", data.turnRate[0]);
		out.putFloat("TurnRates_y", data.turnRate[1]);
		out.putFloat("TurnRates_z", data.turnRate[2]);
		out.putFloat("MagneticFields_x", data.magneticField[0]);
		out.putFloat("MagneticFields_y", data.magneticField[1]);
		out.putFloat("MagneticFields_z", data.magneticField[2]);
		out.putFloat("quaternions1", data.quaternions[0]);
		out.putFloat("quaternions2", data.quaternions[1]);
		out.putFloat("quaternions3", data.quaternions[2]);
		out.putFloat("quaternions4", data.quaternions[3]);
		out.putFloat("Temperature", data.temperature);
		out.putFloat("Pressure", data.pressure);
		out.putShort("Counter", data.counter);
    	out.putFloat("Euler_Roll",0);
		out.putFloat("Euler_Pitch",0);
		out.putFloat("Euler_Yaw",0);
	
		return out;
	}

	public static class IMUdata{
		public float[] 	acceleration={0.0f,0.0f,0.0f};
		public float[] 	turnRate={0.0f,0.0f,0.0f};
		public float[] 	magneticField={0.0f,0.0f,0.0f};
		public float[] 	quaternions={0.0f,0.0f,0.0f,0.0f};
		public float   	pressure=0.0f;
		public float 	temperature=0.0f;
		public long 	time=0;
		public short	counter=0;
		
		//Package=(receivedMessage.counter + ", " + receivedMessage.acceleration[0] + ", " + receivedMessage.acceleration[1] + ", " + receivedMessage.acceleration[2] + ", " + receivedMessage.turnRate[0] + ", " + receivedMessage.turnRate[1] + ", " + receivedMessage.turnRate[2] + ", " + receivedMessage.magneticField[0] + ", " + receivedMessage.magneticField[1] + ", " + receivedMessage.magneticField[2] + ", " + receivedMessage.temperature + "\n").getBytes();
		public byte[] toBytes(){
			return (counter + ", " + time + ", " + acceleration[0] + ", " + acceleration[1] + ", " + acceleration[2] + ", " + turnRate[0] + ", " + turnRate[1] + ", " + turnRate[2] + ", " + magneticField[0] + ", " + magneticField[1] + ", " + magneticField[2] + ", " + temperature + "\n").getBytes();
			
		}
		public byte[] toBytes(float time2){
			return (counter + ", " + time + ", " + acceleration[0] + ", " + acceleration[1] + ", " + acceleration[2] + ", " + turnRate[0] + ", " + turnRate[1] + ", " + turnRate[2] + ", " + magneticField[0] + ", " + magneticField[1] + ", " + magneticField[2] + ", " + temperature + ", " + time2 + "\n").getBytes();
			
		}

	}
	
	private byte crc_calculation(byte message[],int length){
		//Calculate the crc for the XSens MTi IMU messages.
		byte sum=0;
		for(int i=1;i<length-1;i++)
			sum+=message[i];
		return (byte) -sum;
	}

	



}
