package es.csic.getsensordata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;

import static java.lang.Thread.sleep;

/* ----------------Example of use:------------------
1) First Create an "mMIMU22BT" object:
MIMU22BT mMIMU22BT=new MIMU22BT(handlerMIMU22BT,Bluetooth_MAC);
where:
 - "handlerMIMU22BT" is a "Handler" object that processes (e.g. to update your UI with MIMU22BT data) the data sent by the object in a message.
   You have to create this code: for.example:
   Handler handlerMIMU22BT=new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle data=msg.getData();
			String mensajetype=data.getString("MensajeType");
			String readername=data.getString("ReaderName");
			float delta_X=data.getFloat("delta_X");
			float delta_Y=data.getFloat("delta_Y");
			float delta_Z=data.getFloat("delta_Z");
			float delta_Theta=data.getFloat("delta_Theta");

			// Do something with this data (e.g. update your UI)
			}
	}
 - "Bluetooth_MAC": is the MAC address of the MIMU22BT
2) Connect a socket to the Bluetooth IMU reader device
mMIMU22BT.connect();
3) Start reading the MIMU22BT (put the reader in Measurement mode):
mMIMU22BT.startreading();
4) Now all the processing of data is done in the handlerMIMU22BT (in your activity)
5) When you do not need the MIMU22BT anymore stop and disconnect it:
mMIMU22BT.stopreading();
mMIMU22BT.disconnect();

 */

public class MIMU22BT  extends Thread{

    // Standard Bluetooth serial protocol UUID
    final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String TAG="MIMU22BT";
    private BluetoothAdapter bluetooth;
    private BluetoothDevice IMU_BluetoothDevice;
    private InputStream mInStream;
    private OutputStream mOutStream;
    private BluetoothSocket mSocket;
    private BluetoothAdapter mAdapter;
    Thread ConnectThread;
    private Thread ReadingThread;
    private Handler handlerIMU;
    private String IMU_bluetoothName;
    private UUID uuid;
    private Boolean uuid_obtained=false;
    private Boolean en_ReadingIMU_Thread=false;
    Boolean socket_connected=false;
    private Boolean imposible_conectarme=false;
    Boolean IMUstream=false;  // PDR deltas (false) or IMU stream sampled at 125Hz (true)


    //----------------- Constructor---------------------
    MIMU22BT(Handler handlerIMU,String IMUBluetooth_MAC,Boolean IMU_mode){
        this.handlerIMU= handlerIMU;
        IMUstream=IMU_mode;
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth.isEnabled()==true)
        {
            IMU_BluetoothDevice =	bluetooth.getRemoteDevice(IMUBluetooth_MAC);
            IMU_bluetoothName = IMU_BluetoothDevice.getName();
            if ( IMU_BluetoothDevice.getBondState()==BluetoothDevice.BOND_BONDED )
            {
                ParcelUuid[] uuids=IMU_BluetoothDevice.getUuids();
                if (uuids.length>0)
                {
                    uuid=uuids[0].getUuid();
                    Log.i(TAG,"Object MIMU22BT Created. Got UUID");
                    uuid_obtained=true;
                }
            }
        }
        else
        {
            System.out.println("MIMU22BT Error: Bluetooth not activated");
        }
    }


    //----------------Connect-----------------
    public  void connect(){
        if (uuid_obtained)
        {
            try {
                // Forma como lo hago yo, y que funciona perfectamente en el S3
              //  mSocket = IMU_BluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                mSocket = IMU_BluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);


                // Forma como lo hace Francisco para el LPMS-B:
                //	mSocket = IMU_BluetoothDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);

                // Segun un foro: http://stackoverflow.com/questions/12274210/android-bluetooth-spp-with-galaxy-s3
                // Hay un bug de Android con el Galaxy S3 que dificulta la conexion de Bluetooth.
                // Lo resuelven asi:
                //device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
                //Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
                //socket = (BluetoothSocket)m.invoke(device, Integer.valueOf(1));

                //	Method m = IMU_BluetoothDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
                //	mSocket = (BluetoothSocket)m.invoke(IMU_BluetoothDevice, Integer.valueOf(1));

                System.out.println("MIMU22BT OK: Socket created");
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
                        System.out.println("MIMU22BT OK: Socket connected");
                        // Enviar notificacion al Handler encargado de pintar en UI
                        Message mensaje= new Message();
                        Bundle data = new Bundle();
                        data.putString("MensajeType","Connect");
                        data.putString("ReaderName",IMU_bluetoothName);
                        data.putBoolean("Connected",true);
                        mensaje.setData(data);
                        handlerIMU.sendMessage(mensaje);
                    } catch (IOException e) {
                        //System.out.println("MIMU22BT ERROR: Socket NOT connected");
                        //Log.e(TAG, "ERROR: Socket NOT connected", e);  // Saca el mensaje en texto rojo en "LogCat"
                        Log.e(TAG, "ERROR: Socket NOT connected");
                        imposible_conectarme=true;
                    }
                }
            });
            if (ConnectThread.isAlive()==false)
            {
                ConnectThread.setName("Hilo ConnectThread - MIMU22BT");
                ConnectThread.start();
            }
        }
        else {
            System.out.println("MIMU22BT Error: No UUID obtained");
        }
    }


    //------------StartReading---------------------
    public  void startreading(){
        if (uuid_obtained)
        {
            ReadingThread = new Thread(new Runnable() {
                public void run()
                {
                    en_ReadingIMU_Thread=true;
                    try {
                        // Put IMU reader in measurement mode
                        // Configurar lector IMU
                        while (socket_connected==false  && en_ReadingIMU_Thread==true  && imposible_conectarme==false)
                        {
                            Log.i(TAG,"=============Loop====================="+IMU_bluetoothName);
                            sleep(1000);
                        } // Esperar a que el socket (en "ConnectThred") se conecte de verdad
                        Log.i(TAG,"=============Start Reading====================="+IMU_bluetoothName);

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



                            // Enviar comando inicio lectura
                            comando_empezar_lectura_PDR();


                            // Listen the Incoming DATA
                            Log.i(TAG,"Start ListenforMessages"+IMU_bluetoothName);
                            listenForDataIMU();   // Blocking or very long (infinite) process
                            Log.i(TAG,"End ListenforMessages"+IMU_bluetoothName);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "listenForDataIMU Exception", e);
                    }
                }
            });  // end-thread

            if (ReadingThread.isAlive()==false)
            {
                ConnectThread.setName("Hilo ReadingThread - MIMU22BT");
                ReadingThread.start();
                Log.i("MIMU22BT", "ReadingThread.start();");
               // SystemClock.sleep(400);  // Para que de tiempo a ReadingThread a comenzar y no se ejecute esto 2 veces o mas
            }
        }
    }




    //----------------
    private  void comando_empezar_lectura_PDR()
    {
        // Parar llegada de datos desde la IMU al movil
        byte[] output_off={0x22, 0x00, 0x22};   // {34,0,34}
        SendDataSocket(output_off);

        // Vaciar buffer entrada:
        try {
            int bytes_basura = mInStream.available();
            Log.i("MIMU22BT", "Bytes basura en buffer:" + bytes_basura);
            while (mInStream.available() > 0) {
                mInStream.read();  // Leer 1 solo byte ( Blocking function ) 4 veces
            }
        }catch (IOException e) {    }


        // ............Enviar comando inicio lectura PDR............

        if (IMUstream==false) {   // .............PDR_mode.................
            Log.i(TAG, "Config MIMU22BT reader" + IMU_bluetoothName);
            byte[] PDR_on = {0x34, 0x00, 0x34};         //{52,0,52}
            SendDataSocket(PDR_on);  // Send command

            //Read ACK de 4 bytes
            Read_4_bytes_as_ACK();
            Log.i("MIMU22BT", "read 4bytes ACK after {52,0,52} command sent");
        } else    // ..................IMU stream mode...................
        {
            byte[] IMU_sampling = {0x30, 0x13, 0x00, 0x00, 0x43};         //{48,19,0,0,67}   // sampling mode
            SendDataSocket(IMU_sampling);  // Send command

            //Read ACK de 4 bytes
            Read_4_bytes_as_ACK();
            Log.i("MIMU22BT", "read 4bytes ACK after {48,19,0,0,67} command sent");

            byte[] IMU_on = {0x40, 0x04, 0x00, 0x44};         //{64,4,0,68}   // sampling 125 Hz
            SendDataSocket(IMU_on);  // Send command

            //Read ACK de 4 bytes (solo la primera vez tras mandar el comanmdo de lectura IMU_on={64,4,0,68}   // start output at sampling rate of  125 Hz
            Read_4_bytes_as_ACK();
           Log.i("MIMU22BT", "read 4bytes ACK after {64,4,0,68} command sent");



        }
    }




    //------------------------listenForDataIMU-----------------------------------
    private  void listenForDataIMU()
    {
        int byteLeido;
        byte mibyte;
        byte[] linea = new byte[64];  //64
        byte [] ack = new byte[5];
        int package_number_1,package_number_2;
        int bytes_disponibles;
        int index=0;


            while (en_ReadingIMU_Thread)  //LOOP
            {
                 int bytes_packet;
                if (IMUstream==false) {   // .............PDR_mode..........
                    bytes_packet=64; //Total 64 bytes= header (4 bytes)+ payload (56 bytes)+ Step counter (2 bytes)+checksum (2 bytes) :
                }else{    // .............stream mode..............
                    bytes_packet=34; //Total 34 bytes= header (4 bytes)+ timestamp(4 bytes)+ Gyr/Acc(24 bytes)+ checksum (2 bytes) :
                }

                    int counter=0;
                    while(counter<bytes_packet)
                    {
                        Log.i("MIMU22BT", "Intento leer los "+bytes_packet+" bytes (counter="+counter+")");
                        try {
                            counter+=mInStream.read(linea, counter, bytes_packet-counter);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                Bundle data=parsear_linea_IMU(linea);   // .......PARSEAR......data contiene los datos IMU, o 'null' (si checksum mmalo)...

                if (data!=null) {  // if check-sum is ok
                    if (IMUstream==false) {   // PDR_mode
                        //Inform MIMU witk ACK that pakect received (1 P1 P2 Checksum(1) checksum(2)):
                        package_number_1 = linea[1] & 0xFF;
                        package_number_2 = linea[2] & 0xFF;
                        ack[0] = 0x01;
                        ack[1] = (byte) package_number_1;
                        ack[2] = (byte) package_number_2;
                        ack[3] = (byte) ((1 + package_number_1 + package_number_2 - (1 + package_number_1 + package_number_2) % 256) / 256);
                        ack[4] = (byte) ((1 + package_number_1 + package_number_2) % 256);
                        SendDataSocket(ack);  // send ACK
                        Log.i(TAG, "ACK sent to socket.");
                    }

                    // Enviar datos IMU a Handler en hilo UI para que los pinte
                    Message mensaje = new Message();
                    if (IMUstream==false) {   // PDR_mode
                        data.putString("MensajeType", "IMU_PDR_Data");
                    }else{
                        data.putString("MensajeType", "IMU_Stream_Data");
                    }
                    data.putString("ReaderName", IMU_bluetoothName);
                    mensaje.setData(data);
                    handlerIMU.sendMessage(mensaje);
                } else{  // if checksum is wrong
                    comando_empezar_lectura_PDR();    // Le digo a IMU que empieze de nuevo los envios
                }

            }  // end-while

            Log.i("ListenIMU","Salgo de LOOP listenForMessages");

    }



    //-----------------------------parsear_linea_IMU---------------------
    private  Bundle parsear_linea_IMU(byte[] linea)
    {
        Boolean linea_erronea=false;
        Bundle bundle = new Bundle();
        int suma=0;
        int Checksum1,Checksum2;

        // ----------------Comprobar CHECKSUM----------
        suma=0;
        int bytes_packet;
        if (IMUstream==false) {   // PDR_mode
            bytes_packet=64; //Total 64 bytes= header (4 bytes)+ payload (56 bytes)+ Step counter (2 bytes)+checksum (2 bytes) :
        }else{
            bytes_packet=34; //Total 34 bytes= header (4 bytes)+ timestamp(4 bytes)+ Gyr/Acc(24 bytes)+ checksum (2 bytes) :
        }
        for (int i=0; i<=bytes_packet-3; i++)
        {
            suma= suma + (linea[i] & 0xFF);
        }
        Checksum1=linea[bytes_packet-2] & 0xFF;
        Checksum2=linea[bytes_packet-1] & 0xFF;
        if (suma!=(Checksum1*256+Checksum2)) {
         //   linea_erronea = true;  // continue: "Pass control to next iteration of for or while loop"
            Log.i("MIMU22BT","Linea con mal Checksum");
            linea_erronea=true;
        }

        if (linea_erronea==false)
        {
            if (IMUstream==false) {   // ..................PDR_mode..........................................
                //Total 64 bytes= header (4 bytes)+ payload (56 bytes)+ Step counter (2 bytes)+checksum (2 bytes) :

                //int packet2=linea[1]*256+linea[2];
                int packet = ((linea[1] & 0xff) << 8) | ((linea[2] & 0xff));

                byte[] bytes_float_dx={linea[4],linea[5],linea[6],linea[7]};
                float dx = ByteBuffer.wrap(bytes_float_dx).order(ByteOrder.BIG_ENDIAN).getFloat();

                byte[] bytes_float_dy={linea[8],linea[9],linea[10],linea[11]};
                float dy = ByteBuffer.wrap(bytes_float_dy).order(ByteOrder.BIG_ENDIAN).getFloat();

                byte[] bytes_float_dz={linea[12],linea[13],linea[14],linea[15]};
                float dz = ByteBuffer.wrap(bytes_float_dz).order(ByteOrder.BIG_ENDIAN).getFloat();

                byte[] bytes_float_d_theta={linea[16],linea[17],linea[18],linea[19]};
                float d_theta = ByteBuffer.wrap(bytes_float_d_theta).order(ByteOrder.BIG_ENDIAN).getFloat();

                byte[] bytes_float_Cov  = new byte[4];
                float[] Cov  = new float[10];
                int idx_ini=20;
                for (int i=0;i<=9;i++){
                    bytes_float_Cov[0]=linea[idx_ini+i*4+0];
                    bytes_float_Cov[1]=linea[idx_ini+i*4+1];
                    bytes_float_Cov[2]=linea[idx_ini+i*4+2];
                bytes_float_Cov[3]=linea[idx_ini+i*4+3];
                Cov[i] = ByteBuffer.wrap(bytes_float_Cov).order(ByteOrder.BIG_ENDIAN).getFloat();
            }

           // byte[] bytes_steps={linea[60],linea[61]};
           // int steps = ByteBuffer.wrap(bytes_steps).order(ByteOrder.BIG_ENDIAN).getInt();
           //int steps2=linea[60]*256+linea[61];
            int steps = ((linea[60] & 0xff) << 8) | ((linea[61] & 0xff));

            String cadena=String.format(Locale.US,"\nIM1P;%d;%d;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f",
                    packet,steps,dx,dy,dz,d_theta,Cov[0],Cov[1],Cov[2],Cov[3],Cov[4],Cov[5],Cov[6],Cov[7],Cov[8],Cov[9]);
            Log.i("MIMU22BT",cadena);

            // empaquetar
            bundle.putInt("Packet", packet);
            bundle.putInt("Steps", steps);
            bundle.putFloat("Dx", dx);
            bundle.putFloat("Dy", dy);
            bundle.putFloat("Dz", dz);
            bundle.putFloat("D_Theta", d_theta);
            bundle.putFloatArray("Cov", Cov);

            }else{   // ....................IMU streaming.................................
                //Total 34 bytes= header (4 bytes)+ timestamp(4 bytes)+Acc(12 bytes)+Gyr(12 bytes)+checksum (2 bytes) :

                //int packet2=linea[1]*256+linea[2];
                int packet = ((linea[1] & 0xff) << 8) | ((linea[2] & 0xff));

//                byte[] bytes_timestamp={linea[4],linea[5],linea[6],linea[7]};
//                float timestamp = ByteBuffer.wrap(bytes_timestamp).order(ByteOrder.BIG_ENDIAN).getFloat();

                int timestamp = ((linea[4] & 0xff) << 24) | ((linea[5] & 0xff) << 16) | ((linea[6] & 0xff) << 8) | (linea[7] & 0xff);

                byte[] bytes_AcceX={linea[8],linea[9],linea[10],linea[11]};
                float AcceX = ByteBuffer.wrap(bytes_AcceX).order(ByteOrder.BIG_ENDIAN).getFloat();

                byte[] bytes_AcceY={linea[12],linea[13],linea[14],linea[15]};
                float AcceY = ByteBuffer.wrap(bytes_AcceY).order(ByteOrder.BIG_ENDIAN).getFloat();

                byte[] bytes_AcceZ={linea[16],linea[17],linea[18],linea[19]};
                float AcceZ = ByteBuffer.wrap(bytes_AcceZ).order(ByteOrder.BIG_ENDIAN).getFloat();

                byte[] bytes_GyroX={linea[20],linea[21],linea[22],linea[23]};
                float GyroX = ByteBuffer.wrap(bytes_GyroX).order(ByteOrder.BIG_ENDIAN).getFloat();

                byte[] bytes_GyroY={linea[24],linea[25],linea[26],linea[27]};
                float GyroY = ByteBuffer.wrap(bytes_GyroY).order(ByteOrder.BIG_ENDIAN).getFloat();

                byte[] bytes_GyroZ={linea[28],linea[29],linea[30],linea[31]};
                float GyroZ = ByteBuffer.wrap(bytes_GyroZ).order(ByteOrder.BIG_ENDIAN).getFloat();


                String cadena=String.format(Locale.US,"\nIM1X;%d;%d;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f",
                        packet,timestamp,AcceX,AcceY,AcceZ,GyroX,GyroY,GyroZ);
                Log.i("MIMU22BT",cadena);

                // empaquetar
                bundle.putInt("Packet", packet);
                bundle.putInt("Timestamp",timestamp);
                //bundle.putFloat("Timestamp",timestamp);
                bundle.putFloat("AcceX", AcceX);
                bundle.putFloat("AcceY", AcceY);
                bundle.putFloat("AcceZ", AcceZ);
                bundle.putFloat("GyroX", GyroX);
                bundle.putFloat("GyroY", GyroY);
                bundle.putFloat("GyroZ", GyroZ);
            }

        }else   // Checksum error
        {bundle=null;}
        return bundle;
    }

    //-------------------StopReading--------------------------
    public  void stopreading(){
        Log.i(TAG,"StopReading: INI");
        en_ReadingIMU_Thread=false;  // para indicarle que salga del loop en el thread "ReadingIMU"
        if (socket_connected)
        {
            Log.i(TAG,"StopReading: Send M,0");
            byte[] process_off={0x32, 0x00, 0x32};  // {50,0,50}
            byte[] output_off={0x22, 0x00, 0x22};   // {34,0,34}

            SendDataSocket(output_off);
            SendDataSocket(process_off);

            try {
                if (ReadingThread.isAlive())
                {
                    ReadingThread.interrupt();
                }
                Log.i(TAG,"StopReading: ReadingThread interrupted");
            } catch (Exception e) {};


        }
        else
        {
            System.out.println("MIMU22BT. Info: No stopping done since it was not connected nor reading");
        }
        Log.i(TAG,"StopReading: END");
    }

    //-------------Disconnect-----------------------------
    public  void disconnect(){
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
                    socket_connected=false;
                }
                Log.i(TAG,"END:Socket closed on disconnetct method");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("MIMU22BT. Info: No disconnection done since it was not connected");
        }

    }
    //---------

    //----------------SendDatatoSocket----------------
    private  void SendDataSocket(byte[] byte_array)
    {
        //byte[] buffer=byte_array;
        try {
            // mOutStream.write(buffer);
            mOutStream.write(byte_array);

        } catch (IOException e) {
            Log.i(TAG, "Data Send failed.", e);
        }
    }

    private void Read_4_bytes_as_ACK(){
        //Read ACK de 4 bytes
        try {
            int intentos = 0;
            while (mInStream.available() < 4 & intentos < 20) {
                SystemClock.sleep(100);
                intentos++;
                Log.i("MIMU22BT", "ainting in loop for ACK de IMU (IMU stream mode)");
            }
            if (mInStream.available() >= 4) {
                for (int i = 1; i <= 4; i++) {
                    mInStream.read();  // Leer 1 solo byte ( Blocking function ) 4 veces
                }
            }
        } catch (IOException e) {
            Log.i("MIMU22BT", "comamdo empezar_lectura_IMUstream failed", e);
        }
    }
}
