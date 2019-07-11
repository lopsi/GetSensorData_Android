package es.csic.getsensordata;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.Manifest;
import android.annotation.SuppressLint;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.telemetry.EstimoteTelemetry;
import com.estimote.sdk.eddystone.Eddystone;
import com.estimote.sdk.eddystone.EddystoneTelemetry;
import com.estimote.sdk.MacAddress;

public class MainActivity extends Activity implements SensorEventListener, LocationListener, GpsStatus.Listener {
	// Flags:
	private Boolean Flag_Discover_Bluetooth=false;
	private Boolean flag_Trace=false;

	// Atributos de Clase vvvv
	private Context context=this;
	private SensorManager mSensorManager;
	private LocationManager locationManager;
	private WifiManager wifimanager;            // Wifi
	private BroadcastReceiver wifibroadcastreceiver;
	private BluetoothAdapter bluetooth;  // Bluetooth
	private boolean esta_App_encendio_bluetooth=false;
	private BroadcastReceiver bluetoothDiscoveryMonitor;
	private BroadcastReceiver bluetoothdiscoveryResult;
	private ArrayList<BluetoothDevice> bluetoothdeviceList = new ArrayList<BluetoothDevice>();
	private TimerTask scanTaskWifi;                 // Manejador Timer para scanear WiFis
	private TimerTask scanTaskBlue;                 // Manejador Timer para scanear Bluetooth
	private TimerTask TaskReloj;                    // Manejador Timer para pintar Reloj
	private final Handler handlerWifi = new Handler();
	private final Handler handlerBlue = new Handler();
	private final Handler handlerReloj = new Handler();
	private Timer timerWifi;          // Timer
	private Timer timerBlue;          // Timer
	private Timer timerReloj;          // Timer
	OnRecordPositionUpdateListener mRecordAudioListener;
	Handler handlerRFID;
	int num_connected_RFIDReader=0;
	ArrayList<String> RFIDreadernameList = new ArrayList<String>();  // Me gusta tiene los metodos: "contains" y "add"
	ArrayList<RFIDM220Reader> mRFIDM220ReaderList =new ArrayList<RFIDM220Reader>();
	Microphone mMicrophone;
	LPMSB_IMU mLpmsB;     // Objeto LPMS
	Handler handlerLPMS;  // manejador LPMS
	MIMU22BT mMIMU22BT;     // Objeto MIMU22BT
	Handler handlerMIMU22BT;  // manejador MIMU22BT

	//----XSens--
	MTi_XSens_IMU mXSens;  // Objeto XSens
	Handler handlerXSens;  // manejador XSens
	private UsbDevice mUsbDevice=null;
	private PendingIntent mPermissionIntent=null;
	private static final String ACTION_USB_PERMISSION ="es.csic.getsensordata.USB_PERMISSION";
	BroadcastReceiver mUsbReceiver;
	boolean USBConnected=false;
	boolean configMode=false;
	private UsbManager USBmanager=null;
	//---
	GpsStatus gpsstatus=null;
	int num_satellites_in_view=0;
	int num_satellites_in_use=0;
	Sensor Sensor_Acc;
	Sensor Sensor_Gyr;
	Sensor Sensor_Mag;
	Sensor Sensor_Pre;
	Sensor Sensor_Ligh;
	Sensor Sensor_Prox;
	Sensor Sensor_Humi;
	Sensor Sensor_Temp;
	Sensor Sensor_AHRS;
	String texto_Acc_Features;
	String texto_Gyr_Features;
	String texto_Mag_Features;
	String texto_Pre_Features;
	String texto_Ligh_Features;
	String texto_Prox_Features;
	String texto_Humi_Features;
	String texto_Temp_Features;
	String texto_AHRS_Features;
	String texto_GNSS_Features;
	String texto_Wifi_Features;
	String texto_Blue_Features;
	String texto_Ble4_Features;
	String texto_Soun_Features;
	String texto_RFID_Features;
	String texto_IMUX_Features;
	String texto_IMUL_Features;
	String texto_MIMU22BT_Features;
	TextView obj_txtView0;
	TextView obj_txtView1;
	TextView obj_txtView1a;
	TextView obj_txtView1b;
	TextView obj_txtView2;
	TextView obj_txtView2a;
	TextView obj_txtView2b;
	TextView obj_txtView3;
	TextView obj_txtView3a;
	TextView obj_txtView3b;
	TextView obj_txtView4;
	TextView obj_txtView4a;
	TextView obj_txtView4b;
	TextView obj_txtView5;
	TextView obj_txtView5a;
	TextView obj_txtView5b;
	TextView obj_txtView6;
	TextView obj_txtView6a;
	TextView obj_txtView6b;
	TextView obj_txtView7;
	TextView obj_txtView7a;
	TextView obj_txtView7b;
	TextView obj_txtView8;
	TextView obj_txtView8a;
	TextView obj_txtView8b;
	TextView obj_txtView9;
	TextView obj_txtView9a;
	TextView obj_txtView9b;
	TextView obj_txtView10;// GPS
	TextView obj_txtView10a;
	TextView obj_txtView10b;
	TextView obj_txtView10c;
	TextView obj_txtView11;  //Wifi
	TextView obj_txtView11a;
	TextView obj_txtView11b;
	TextView obj_txtView11c;
	TextView obj_txtView12;  // Bluetooth
	TextView obj_txtView12a;
	TextView obj_txtView12b;
	TextView obj_txtView12c;
	TextView obj_txtView13;  // Audio
	TextView obj_txtView13a;
	TextView obj_txtView13b;
	TextView obj_txtView14;  // BLE
	TextView obj_txtView14a;
	TextView obj_txtView14b;
	TextView obj_txtView14c;
	TextView obj_txtView20;// RFID-RFcode
	TextView obj_txtView20a;
	TextView obj_txtView20b;
	TextView obj_txtView20c;
	TextView obj_txtView21;  //IMU-Xsens
	TextView obj_txtView21a;
	TextView obj_txtView21b;
	TextView obj_txtView22;  //IMU-LPMS-B
	TextView obj_txtView22a;
	TextView obj_txtView22b;
	TextView obj_txtView23;  //IMU-Osmium MIMU22BT
	TextView obj_txtView23a;
	TextView obj_txtView23b;
	Button obj_btnBotonMarkPosition;
	ToggleButton obj_toggleButton1;
	ToggleButton obj_toggleButton2;
	ToggleButton obj_ToggleButtonSave;
	OutputStreamWriter fout;
	boolean primer_sensor_cambia=true;
	long tiempo_inicial_ns_raw=0;
	long timestamp_ns;
	double timestamp;
	long contador_Acce=0;
	long contador_Gyro=0;
	long contador_Magn=0;
	long contador_Pres=0;
	long contador_Ligh=0;
	long contador_Prox=0;
	long contador_Humi=0;
	long contador_Temp=0;
	long contador_Ahrs=0;
	long contador_Gnss=0;
	long contador_Wifi=0;
	long contador_Blue=0;
	long contador_Ble4=0;
	long contador_Soun=0;
	long contador_Rfid=0;
	long contador_Imux=0;
	long contador_Imul=0;
	long contador_MIMU22BT=0;
	long contador_Posi=0;
	float freq_medida_Acce=0;
	float freq_medida_Gyro=0;
	float freq_medida_Magn=0;
	float freq_medida_Pres=0;
	float freq_medida_Ligh=0;
	float freq_medida_Prox=0;
	float freq_medida_Humi=0;
	float freq_medida_Temp=0;
	float freq_medida_Ahrs=0;
	float freq_medida_Gnss=0;
	float freq_medida_Wifi=0;
	float freq_medida_Blue=0;
	float freq_medida_Ble4=0;
	float freq_medida_Soun=0;
	float freq_medida_Rfid=0;
	float freq_medida_Imux=0;
	float freq_medida_Imul=0;
	float freq_medida_MIMU22BT=0;
	double timestamp_Acce_last=0;
	double timestamp_Gyro_last=0;
	double timestamp_Magn_last=0;
	double timestamp_Pres_last=0;
	double timestamp_Ligh_last=0;
	double timestamp_Prox_last=0;
	double timestamp_Humi_last=0;
	double timestamp_Temp_last=0;
	double timestamp_Ahrs_last=0;
	double timestamp_Gnss_last=0;
	double timestamp_Wifi_last=0;
	double timestamp_Blue_last=0;
	double timestamp_Ble4_last=0;
	double timestamp_Soun_last=0;
	double timestamp_Rfid_last=0;
	double timestamp_Imux_last=0;
	double timestamp_Imul_last=0;
	double timestamp_MIMU22BT_last=0;
	double timestamp_Acce_last_update=0;
	double timestamp_Gyro_last_update=0;
	double timestamp_Magn_last_update=0;
	double timestamp_Pres_last_update=0;
	double timestamp_Ligh_last_update=0;
	double timestamp_Prox_last_update=0;
	double timestamp_Humi_last_update=0;
	double timestamp_Temp_last_update=0;
	double timestamp_Ahrs_last_update=0;
	double timestamp_Gnss_last_update=0;
	double timestamp_Wifi_last_update=0;
	double timestamp_Blue_last_update=0;
	double timestamp_Ble4_last_update=0;
	double timestamp_Soun_last_update=0;
	double timestamp_Rfid_last_update=0;
	double timestamp_Imux_last_update=0;
	double timestamp_Imul_last_update=0;
	double timestamp_MIMU22BT_last_update=0;
	double deltaT_update=0.25;   // cada 0.25 segundo actualizo la pantalla con las medidas (4Hz)

	// Estimote:
	private BeaconManager beaconManager;
	private Region region;
	String scanId;  // esado en telemetry
	String scanId_Eddystone;
	boolean flag_BLE=true;   // se cambia por programa
	boolean flag_EstimoteTelemetry=false;   // poner true si quiero recoger telemetria

	// MIMU22
	Boolean flag_MIMUStream=false;    // PDR (false) or IMUstream (true)

	String phone_manufacturer;
	String phone_model;
	int phone_version;
	String phone_versionRelease;


	private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
	private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;


	//======================OnCreate==================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (flag_Trace) {// start tracing to "/sdcard/GetSensorData_Trace.trace"
			Debug.startMethodTracing("GetSensorData_Trace");
		}

		phone_manufacturer = android.os.Build.MANUFACTURER;
		phone_model = android.os.Build.MODEL;
		phone_version = android.os.Build.VERSION.SDK_INT;
		phone_versionRelease = android.os.Build.VERSION.RELEASE;

		Log.i("OnCreate", "Phone_manufacturer " + phone_manufacturer
				+ " \n Phone_model " + phone_model
				+ " \n Phone_version " + phone_version
				+ " \n Phone_versionRelease " + phone_versionRelease
		);

		Log.i("OnCreate", "Inicializando");
		//------------Inicializar UI---------------
		Log.i("OnCreate", "Poner manejadores botones");
		inicializar_objetos_UI();
		poner_manejador_boton1();
		poner_manejador_boton2();
		poner_manejador_botonSave();
		poner_manejador_boton_MarkPosition();

		obj_txtView0.setText("Phone: " + phone_manufacturer+"  "+ phone_model+"  API"+phone_version+"  Android_"+phone_versionRelease);

		// ----------Ver los sensores internos disponibles ------------
		Log.i("OnCreate", "ver sensores internos disponibles");
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Sensor_Acc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor_Gyr = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		Sensor_Mag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		Sensor_Pre = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		Sensor_Ligh = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		Sensor_Prox = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		Sensor_Humi = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
		Sensor_Temp = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		Sensor_AHRS = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

		// Mostrar datos generales del accelerometro:
		if (Sensor_Acc != null) {
			obj_txtView1.setText(" ACCE: "+Sensor_Acc.getName());
			texto_Acc_Features =" Manufacturer: "+Sensor_Acc.getVendor()
			+",\n Version: "+Sensor_Acc.getVersion()
			+", Type:"+Sensor_Acc.getType()
			+", \n Resolution: "+Sensor_Acc.getResolution()+" m/s^2"
			+", \n MaxRange: "+Sensor_Acc.getMaximumRange()+" m/s^2"
			+", \n Power consumption: "+Sensor_Acc.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Acc.getMinDelay();
		} else {
			obj_txtView1.setText(" ACCE: No Accelerometer detected");
			texto_Acc_Features =" No Features";
			obj_txtView1.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView1a.setText(texto_Acc_Features);

		// Mostrar Datos generales del gyroscope:        
		if (Sensor_Gyr != null) {
			obj_txtView2.setText(" GYRO: "+Sensor_Gyr.getName());
			texto_Gyr_Features =" Manufacturer: "+Sensor_Gyr.getVendor()
			+",\n Version: "+Sensor_Gyr.getVersion()
			+", Type: "+Sensor_Gyr.getType()
			+", \n Resolution: "+Sensor_Gyr.getResolution()+" rad/s"
			+", \n MaxRange: "+Sensor_Gyr.getMaximumRange()+" rad/s"
			+", \n Power consumption: "+Sensor_Gyr.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Gyr.getMinDelay();
		} else {
			obj_txtView2.setText(" GYRO: No Gyroscope detected");
			texto_Gyr_Features =(String)" No Features";
			obj_txtView2.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView2a.setText(texto_Gyr_Features);

		// Mostrar Datos generales del Magnetometro:        
		if (Sensor_Mag != null) {
			obj_txtView3.setText(" MAGN: "+Sensor_Mag.getName());
			texto_Mag_Features =" Manufacturer: "+Sensor_Mag.getVendor()
			+",\n Version: "+Sensor_Mag.getVersion()
			+", Type: "+Sensor_Mag.getType()
			+", \n Resolution: "+Sensor_Mag.getResolution()+" uT"
			+", \n MaxRange: "+Sensor_Mag.getMaximumRange()+" uT"
			+", \n Power consumption: "+Sensor_Mag.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Mag.getMinDelay();
		} else {
			obj_txtView3.setText(" MAGN: No Magnetometer detected");
			texto_Mag_Features =(String)" No Features";
			obj_txtView3.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView3a.setText(texto_Mag_Features);

		// Mostrar Datos generales del Barometro:        
		if (Sensor_Pre != null) {
			obj_txtView4.setText(" PRES: "+Sensor_Pre.getName());
			texto_Pre_Features =" Manufacturer: "+Sensor_Pre.getVendor()
			+",\n Version: "+Sensor_Pre.getVersion()
			+", Type: "+Sensor_Pre.getType()
			+", \n Resolution: "+Sensor_Pre.getResolution()+" mbar"
			+", \n MaxRange: "+Sensor_Pre.getMaximumRange()+" mbar"
			+", \n Power consumption: "+Sensor_Pre.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Pre.getMinDelay();
		} else {
			obj_txtView4.setText(" PRES: No Barometer detected");
			texto_Pre_Features =(String)" No Features";
			obj_txtView4.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView4a.setText(texto_Pre_Features);

		// Mostrar Datos generales del Sensor de Luz:        
		if (Sensor_Ligh != null) {
			obj_txtView5.setText(" LIGH: "+Sensor_Ligh.getName());
			texto_Ligh_Features =" Manufacturer: "+Sensor_Ligh.getVendor()
			+",\n Version: "+Sensor_Ligh.getVersion()
			+", Type: "+Sensor_Ligh.getType()
			+", \n Resolution: "+Sensor_Ligh.getResolution()+" lux"
			+", \n MaxRange: "+Sensor_Ligh.getMaximumRange()+" lux"
			+", \n Power consumption: "+Sensor_Ligh.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Ligh.getMinDelay();
		} else {
			obj_txtView5.setText(" LIGH: No Light Sensor detected");
			texto_Ligh_Features =(String)" No Features";
			obj_txtView5.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView5a.setText(texto_Ligh_Features);

		// Mostrar Datos generales del Sensor de Prox:        
		if (Sensor_Prox != null) {
			obj_txtView6.setText(" PROX: "+Sensor_Prox.getName());
			texto_Prox_Features =" Manufacturer: "+Sensor_Prox.getVendor()
			+",\n Version: "+Sensor_Prox.getVersion()
			+", Type: "+Sensor_Prox.getType()
			+", \n Resolution: "+Sensor_Prox.getResolution()+" units?"
			+", \n MaxRange: "+Sensor_Prox.getMaximumRange()+" units?"
			+", \n Power consumption: "+Sensor_Prox.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Prox.getMinDelay();
		} else {
			obj_txtView6.setText(" PROX: No Proximity Sensor detected");
			texto_Prox_Features =(String)" No Features";
			obj_txtView6.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView6a.setText(texto_Prox_Features);

		// Mostrar Datos generales del Sensor de Humidity:        
		if (Sensor_Humi != null) {
			obj_txtView7.setText(" HUMI: "+Sensor_Humi.getName());
			texto_Humi_Features =" Manufacturer: "+Sensor_Humi.getVendor()
			+",\n Version: "+Sensor_Humi.getVersion()
			+", Type: "+Sensor_Humi.getType()
			+", \n Resolution: "+Sensor_Humi.getResolution()+" units?"
			+", \n MaxRange: "+Sensor_Humi.getMaximumRange()+" units?"
			+", \n Power consumption: "+Sensor_Humi.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Humi.getMinDelay();
		} else {
			obj_txtView7.setText(" HUMI: No Humidity Sensor detected");
			texto_Humi_Features =(String)" No Features";
			obj_txtView7.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView7a.setText(texto_Humi_Features);

		// Mostrar Datos generales del Sensor de Ambient Temperature:        
		if (Sensor_Temp != null) {
			obj_txtView8.setText(" TEMP: "+Sensor_Temp.getName());
			texto_Temp_Features =" Manufacturer: "+Sensor_Temp.getVendor()
			+",\n Version: "+Sensor_Temp.getVersion()
			+", Type: "+Sensor_Temp.getType()
			+", \n Resolution: "+Sensor_Temp.getResolution()+" units?"
			+", \n MaxRange: "+Sensor_Temp.getMaximumRange()+" units?"
			+", \n Power consumption: "+Sensor_Temp.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Temp.getMinDelay();
		} else {
			obj_txtView8.setText(" TEMP: No Temperature Sensor detected");
			texto_Temp_Features =(String)" No Features";
			obj_txtView8.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView8a.setText(texto_Temp_Features);

		// Mostrar Datos generales de Orientacion:        
		if (Sensor_AHRS != null) {
			obj_txtView9.setText(" AHRS: "+Sensor_AHRS.getName());
			texto_AHRS_Features =" Manufacturer: "+Sensor_AHRS.getVendor()
			+",\n Version: "+Sensor_AHRS.getVersion()
			+", Type: "+Sensor_AHRS.getType()
			+", \n Resolution: "+Sensor_AHRS.getResolution()+" a.u."
			+", \n MaxRange: "+Sensor_AHRS.getMaximumRange()+" a.u."
			+", \n Power consumption: "+Sensor_AHRS.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_AHRS.getMinDelay();
		} else {
			obj_txtView9.setText(" AHRS: No Attitude&Heading estimation");
			texto_AHRS_Features =(String)" No Features";
			obj_txtView9.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView9a.setText(texto_AHRS_Features);

		// ------------Ver los servicios de LOCALIZACION (GNSS/Network)--------------
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		// Mostrar Datos generales de GNSS:        
		if (locationManager != null) {
			LocationProvider provider = null;

			texto_GNSS_Features ="";
			obj_txtView10.setText(" GNSS: Location Service (GPS/Network)");

			//List<String> listaProviders=locationManager.getAllProviders();
			List<String> listaProviders=locationManager.getProviders(true);

			for (String provider_str : listaProviders) {
				int indice_proveedor=listaProviders.indexOf(provider_str);
				try {
					provider = locationManager.getProvider(provider_str);  // aqui CASCA con el emulador!!!!!!!!
				} catch (Exception e) {
					Log.i("OnCreate", "No responde bien getProvider");
				texto_GNSS_Features =" GNSS: No Location Providers";
				obj_txtView10.setBackgroundColor(0xFFFF0000);  // red color
				}
				if (provider != null) {
					texto_GNSS_Features =texto_GNSS_Features+ " -Location Provider"+indice_proveedor+": "+provider_str.toUpperCase()+
					", Accuracy: "+provider.getAccuracy()+", \n  Supports Altitude: "+provider.supportsAltitude()+
					", Power Cons.: "+provider.getPowerRequirement()+" mA"+"\n";
				}
			}
		} else {
			obj_txtView10.setText(" GNSS: No LOCATION system detected");
			texto_GNSS_Features =(String)"No Features";
			obj_txtView10.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView10a.setText(texto_GNSS_Features);

		// ----------Ver los Servicios WIFI-------------------
		wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE); // jtorres 20180314 fixed compilation error
		//wifimanager = (WifiManager) getSystemService(Context.WIFI_SERVICE);                       // jtorres 20180314 original

		if (wifimanager != null) {
			if (wifimanager.isWifiEnabled()==false)  // si no está activa, lo indico pero intento encender Wifi
			{
				Log.i("Wifi","Se ha detectado que WiFi apagado");
				obj_txtView11.setText(" WIFI: Switched OFF");
				obj_txtView11.setBackgroundColor(0xFFFF0000);  // red color (más tarde el timer me lo pone a verde)
				texto_Wifi_Features =" Not available ";
				if (wifimanager.getWifiState() != WifiManager.WIFI_STATE_ENABLING) {  // si no est� ya en proceso de encendido => mandar encender
					wifimanager.setWifiEnabled(true);  // mandar encender WiFi
				}
			} else {
				obj_txtView11.setText(" WIFI: Switched ON");
				texto_Wifi_Features =" WiFi MAC address: "+wifimanager.getConnectionInfo().getMacAddress();
			}
		} else {
			obj_txtView11.setText(" WIFI: Not available");
			obj_txtView11.setBackgroundColor(0xFFFF0000);  // red color
			texto_Wifi_Features =" No Features";
		}
		obj_txtView11a.setText(texto_Wifi_Features);

		poner_manejador_WiFiScan();
		// Lanzar el Timer a 1Hz de WiFi Scan
		timerWifi = new Timer("Hilo Timer WiFiScan");
		timerWifi.scheduleAtFixedRate(scanTaskWifi, 2000, 2000);  // llamar a Timer cada 0.5 segundo (con retardo de 2s)

		//--------------------manejador BLE------------------------------------------
		boolean hasBLE =getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		if (hasBLE) {
			Log.i("OnCreate", "Tiene BLE");
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			flag_BLE = pref.getBoolean("opcion6", true);
			Log.i("OnCreate", "flag_BLE leido");
		} else {
			Log.i("OnCreate", "NO tiene BLE");
			flag_BLE =false;
			Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();

		}

		if (flag_BLE) {
			if (phone_version<18)   // si la API no es 18 o mayor no se puede usar BLE y Estimote
			{
				flag_BLE=false;
				Toast.makeText(this, "API version: "+phone_version+"lower than 18", Toast.LENGTH_SHORT).show();
			}
		}

		if (flag_BLE==true)  // si tiene BLE y deseo usarlo
		{
			obj_txtView14.setText(" BLE4: BLE mode is available and active");
			obj_txtView14.setBackgroundColor(0xFF00FF00);  // gree color
			texto_Ble4_Features =" No Features";

			poner_manejador_BLE();
		} else {
			obj_txtView14.setText(" BLE4: No BLE active or available");
			obj_txtView14.setBackgroundColor(0xFFFF0000);  // red color
			texto_Ble4_Features =" No Features";
		}

		// -----------Ver los servicios Bluetooth-----------------------
		obj_txtView12.setText(" BLUE: No Bluetooth adapter available");
		obj_txtView12.setBackgroundColor(0xFFFF0000);  // red color
		texto_Blue_Features =" No Features";
		bluetooth = BluetoothAdapter.getDefaultAdapter();
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && bluetooth != null && flag_BLE == false) {
			if (bluetooth.isEnabled() == false) {
				while (bluetooth.getState() != BluetoothAdapter.STATE_ON) {
					if (bluetooth.getState() != BluetoothAdapter.STATE_TURNING_ON) { // Enciendo Bluetooth
						bluetooth.enable();
						Log.i("OnCreate", "Mando encender Bluetooth");
						esta_App_encendio_bluetooth=true;
					}
				}
			}  // debe salir de aqui con el bluetooth ya enabled
			if (bluetooth.isEnabled() == true) {
				String address = bluetooth.getAddress();
				String name = bluetooth.getName();
				//	bluetooth.startDiscovery();  // buscar dispositivos bluetooth
				Log.i("OnCreate","Bluetooth address: "+address+" Nombre: "+name);
				obj_txtView12.setText(" BLUE: Bluetooth Switched ON");
				obj_txtView12.setBackgroundColor(0xFF00FF00);  // red color
				texto_Blue_Features =" Bluetooth Name: "+name+ " Address: "+address;
			}
			obj_txtView12a.setText(texto_Blue_Features);
			// mostrar dispositivos bonded/paired 
			Set<BluetoothDevice> bondedDevicesBlue = bluetooth.getBondedDevices();
			String cadena_display="";
			cadena_display=cadena_display+"\tBonded Bluetooth devices:"+bondedDevicesBlue.size();
			for (BluetoothDevice device : bondedDevicesBlue) {
				cadena_display=cadena_display+"\n\t-"+device.getName()+" Address: "+device.getAddress();
			}
			obj_txtView12b.setText(cadena_display);

			poner_manejador_BluetoothScan();
			// Lanzar el Timer a 0.1Hz de Bluetooth Discovery
			timerBlue = new Timer("Hilo Timer Bluetooth");
			timerBlue.schedule(scanTaskBlue, 15000, 10000);  // llamar a Timer cada 10 segundos (con retardo de 15s)
		}


		//------------------Servicios AUDIO------------------
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
			obj_txtView13.setText(" SOUN: Microphone Available");
			obj_txtView13.setBackgroundColor(0xFF00FF00);  // gree color
			texto_Soun_Features =" No Features";
		} else {
			obj_txtView13.setText(" SOUN: No Microphone available");
			obj_txtView13.setBackgroundColor(0xFFFF0000);  // red color
			texto_Soun_Features =" No Features";
		}

		if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
			finish();
		}
		try {
			poner_manejador_Audio();  // Codigo donde hacer cosas con el audio leido
			mMicrophone = new Microphone(mRecordAudioListener);
		}catch (SecurityException e){
			mMicrophone=null;
			}


		//-----------------Sensor Externo RFID M220 Reader de RFCode-----------------------
		obj_txtView20.setText(" RFID: No RFCode M220 Reader detected");
		obj_txtView20.setBackgroundColor(0xFFFF0000);  // red color
		obj_txtView20a.setText(" No Features");

		// Intentar Conectarse a todos los Lectores RFID que están Bonded(Paired)
		if (bluetooth != null && flag_BLE == false) {
			Set<BluetoothDevice> bondeddevices=bluetooth.getBondedDevices();
			for (BluetoothDevice device : bondeddevices) {
				String Bluettothname=device.getName();
				if (Bluettothname.contains("M220")) {
					String Bluetooth_MAC=device.getAddress();
					poner_manejador_RFIDReader(); // Codigo donde hacer cosas con el RSSI leido
					RFIDM220Reader mRFIDM220Reader=new RFIDM220Reader(handlerRFID,Bluetooth_MAC);
					mRFIDM220Reader.connect();
					//mRFIDM220Reader.startreading();   //Llamado en onResume()
					mRFIDM220ReaderList.add(mRFIDM220Reader);  //Añado objeto en lista
				}
			}
		}

		// -----------------Sensor Externo IMU de XSens------------------------------------
		obj_txtView21.setText(" IMUX: No MTi Inertial Sensor detected");
		obj_txtView21.setBackgroundColor(0xFFFF0000);  // red color
		obj_txtView21a.setText(" No Features");


		USBmanager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		IntentFilter filterDetached = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
		Log.i("OnCreate","Pongo manejadores USB");
		poner_manejador_USB();    // Codigo "mUsbReceiver" donde hacer cosas con los datos leidos del XSens MTi
		registerReceiver(mUsbReceiver, filter);
		registerReceiver(mUsbReceiver, filterDetached);

		Log.i("OnCreate","Pongo manejadores XSens");
		poner_manejador_XSens();  // Codigo "handlerXSens" donde hacer cosas con los datos leidos del XSens MTi
		Log.i("OnCreate","Instancio Objeto 'MTi_XSens_IMU'");
		mXSens =new MTi_XSens_IMU(handlerXSens,mPermissionIntent);

		if (mUsbDevice == null) {
			//showToast("OnCreate: Pidiendo permiso");
			mUsbDevice=mXSens.getDevice(USBmanager);
			// Una vez concedido el permiso => 
			// dentro del broadcastreceiver "mUsbReceiver" se hace la llamada a mXSens.connect() y mXSens.startmeasurement()
		}


		// -----------------Sensor Externo IMU de LPMS-B------------------------------------
		obj_txtView22.setText(" IMUL: No LPMS-B Inertial Sensor detected");
		obj_txtView22.setBackgroundColor(0xFFFF0000);  // red color
		obj_txtView22a.setText(" No Features");
		//obj_txtView22a.setText(" Life Performance Research (LP) Inertial Measurement Unit (IMU) model LPMS-B. Features: 3 Acc, 3 Gyr, 3 Mag, 1 Baro, Bluetooth connectivity");

		if (bluetooth != null && flag_BLE==false) {
			poner_manejador_LPMS(); // Codigo donde hacer cosas con los datos leidos del LPMS-B
			// Averiguar la direccion MAC del LPMS:
			String Bluetooth_LPMS_MAC="";
			Boolean encontrado_LPMS=false;
			Set<BluetoothDevice> bondeddevices=bluetooth.getBondedDevices();
			for (BluetoothDevice device : bondeddevices) {
				String Bluettothname=device.getName();
				if (Bluettothname.contains("LPMS-B")) {
					Bluetooth_LPMS_MAC=device.getAddress();
					encontrado_LPMS=true;
				}
			}
			if (!encontrado_LPMS) {
				Bluetooth_LPMS_MAC = "00:06:66:45:D2:83";
			} // el que tenemos en el CAR

			mLpmsB = new LPMSB_IMU(bluetooth,Bluetooth_LPMS_MAC,handlerLPMS);
			// Connect and set acquisition paramterst en este oren: Counter, Gyro, Accel, Magneto, Quat, Euler, Pressure, Temperature
			mLpmsB.connect(true, true, true, true, true, true,true,true);
			// comenzar a leer en modo streaming
			//mLpmsB.startreading(); //Llamado en onResume()
		}


// -----------------Sensor Externo IMU modelo Osmium MIMU22BT (Bluetooth 3.0)------------------------------------
		obj_txtView23.setText(" IMUI: No MIMU22BT Inertial Sensor detected");
		obj_txtView23.setBackgroundColor(0xFFFF0000);  // red color
		obj_txtView23a.setText(" No Features");
		obj_txtView23a.setText(" Inertial Elements Osmium MIMU22BT with 4 IMUs fused. Features: 3 Acc, 3 Gyr, 1 Baro, Bluetooth 3.0 connectivity");

		if (bluetooth != null && flag_BLE==false) {
			poner_manejador_MIMU22BT(); // Codigo donde hacer cosas con los datos leidos del MIMU22BT
			// Averiguar la direccion MAC del MIMU22BT:
			String Bluetooth_MIMU22BT_MAC="";
			Boolean encontrado_MIMU22BT=false;
			Set<BluetoothDevice> bondeddevices=bluetooth.getBondedDevices();
			for (BluetoothDevice device : bondeddevices) {
				String Bluettothname=device.getName();
				if (Bluettothname.contains("Amp'ed Up!")) {
					Bluetooth_MIMU22BT_MAC=device.getAddress();
					encontrado_MIMU22BT=true;
				}
			}
			if (!encontrado_MIMU22BT) {
				Bluetooth_MIMU22BT_MAC = "00:80:E1:B2:75:FE";
			} // el que tenemos en el CAR

			mMIMU22BT = new MIMU22BT(handlerMIMU22BT,Bluetooth_MIMU22BT_MAC,flag_MIMUStream);
			mMIMU22BT.connect();
			// comenzar a leer en modo streaming
			//mMIMU22BT.startreading();  //Llamado en onResume()
		}


		Log.i("OnCreate","Fin de OnCreate");

	} //---------------end OnCreate-----------


	// M�todos
	private void inicializar_objetos_UI() {
		// Instanciar objetos locales del interfaz
		obj_txtView0 = (TextView)findViewById(R.id.textView0);

		obj_txtView1 = (TextView)findViewById(R.id.textView1);
		obj_txtView1.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView1a = (TextView)findViewById(R.id.textView1a);
		obj_txtView1b = (TextView)findViewById(R.id.textView1b);
		obj_txtView1a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView1a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView1b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView2 = (TextView)findViewById(R.id.textView2);
		obj_txtView2.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView2a = (TextView)findViewById(R.id.textView2a);
		obj_txtView2b = (TextView)findViewById(R.id.textView2b);
		obj_txtView2a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView2a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView2b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView3 = (TextView)findViewById(R.id.textView3);
		obj_txtView3.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView3a = (TextView)findViewById(R.id.textView3a);
		obj_txtView3b = (TextView)findViewById(R.id.textView3b);
		obj_txtView3a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView3a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView3b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView4 = (TextView)findViewById(R.id.textView4);
		obj_txtView4.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView4a = (TextView)findViewById(R.id.textView4a);
		obj_txtView4b = (TextView)findViewById(R.id.textView4b);
		obj_txtView4a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView4a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView4b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView5 = (TextView)findViewById(R.id.textView5);
		obj_txtView5.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView5a = (TextView)findViewById(R.id.textView5a);
		obj_txtView5b = (TextView)findViewById(R.id.textView5b);
		obj_txtView5a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView5a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView5b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView6 = (TextView)findViewById(R.id.textView6);
		obj_txtView6.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView6a = (TextView)findViewById(R.id.textView6a);
		obj_txtView6b = (TextView)findViewById(R.id.textView6b);
		obj_txtView6a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView6a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView6b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView7 = (TextView)findViewById(R.id.textView7);
		obj_txtView7.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView7a = (TextView)findViewById(R.id.textView7a);
		obj_txtView7b = (TextView)findViewById(R.id.textView7b);
		obj_txtView7a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView7a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView7b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView8 = (TextView)findViewById(R.id.textView8);
		obj_txtView8.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView8a = (TextView)findViewById(R.id.textView8a);
		obj_txtView8b = (TextView)findViewById(R.id.textView8b);
		obj_txtView8a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView8a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView8b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView9 = (TextView)findViewById(R.id.textView9);
		obj_txtView9.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView9a = (TextView)findViewById(R.id.textView9a);
		obj_txtView9b = (TextView)findViewById(R.id.textView9b);
		obj_txtView9a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView9a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView9b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView10 = (TextView)findViewById(R.id.textView10);
		obj_txtView10.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView10a = (TextView)findViewById(R.id.textView10a);
		obj_txtView10b = (TextView)findViewById(R.id.textView10b);
		obj_txtView10c = (TextView)findViewById(R.id.textView10c);
		obj_txtView10a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView10a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView10b.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView10c.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView11 = (TextView)findViewById(R.id.textView11);
		obj_txtView11.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView11a = (TextView)findViewById(R.id.textView11a);
		obj_txtView11b = (TextView)findViewById(R.id.textView11b);
		obj_txtView11c = (TextView)findViewById(R.id.textView11c);
		obj_txtView11a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView11a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView11b.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView11c.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView12 = (TextView)findViewById(R.id.textView12);
		obj_txtView12.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView12a = (TextView)findViewById(R.id.textView12a);
		obj_txtView12b = (TextView)findViewById(R.id.textView12b);
		obj_txtView12c = (TextView)findViewById(R.id.textView12c);
		obj_txtView12a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView12a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView12b.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView12c.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView13 = (TextView)findViewById(R.id.textView13);
		obj_txtView13.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView13a = (TextView)findViewById(R.id.textView13a);
		obj_txtView13b = (TextView)findViewById(R.id.textView13b);
		obj_txtView13a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView13a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView13b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView14 = (TextView)findViewById(R.id.textView14);
		obj_txtView14.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView14a = (TextView)findViewById(R.id.textView14a);
		obj_txtView14b = (TextView)findViewById(R.id.textView14b);
		obj_txtView14c = (TextView)findViewById(R.id.textView14c);
		obj_txtView14a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView14a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView14b.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView14c.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView20 = (TextView)findViewById(R.id.textView20);  // RFID-RFCode
		obj_txtView20.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView20a = (TextView)findViewById(R.id.textView20a);
		obj_txtView20b = (TextView)findViewById(R.id.textView20b);
		obj_txtView20c = (TextView)findViewById(R.id.textView20c);
		obj_txtView20a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView20a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView20b.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView20c.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView21 = (TextView)findViewById(R.id.textView21);  // IMU-Xsens
		obj_txtView21.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView21a = (TextView)findViewById(R.id.textView21a);
		obj_txtView21b = (TextView)findViewById(R.id.textView21b);
		obj_txtView21a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView21a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView21b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView22 = (TextView)findViewById(R.id.textView22);  // IMU-LPMS-B
		obj_txtView22.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView22a = (TextView)findViewById(R.id.textView22a);
		obj_txtView22b = (TextView)findViewById(R.id.textView22b);
		obj_txtView22a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView22a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView22b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView23 = (TextView)findViewById(R.id.textView23);  // IMU-Osmium MIMU22BT
		obj_txtView23.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView23a = (TextView)findViewById(R.id.textView23a);
		obj_txtView23b = (TextView)findViewById(R.id.textView23b);
		obj_txtView23a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView23a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView23b.setVisibility(View.GONE);  // GONE 0x08

		// Gestion de botones
		obj_toggleButton1 = (ToggleButton)findViewById(R.id.togglebutton1);
		obj_toggleButton1.setChecked(false);
		obj_toggleButton2 = (ToggleButton)findViewById(R.id.togglebutton2);
		obj_toggleButton2.setChecked(false);
		obj_ToggleButtonSave = (ToggleButton)findViewById(R.id.togglebuttonsave);
		obj_ToggleButtonSave.setChecked(false);
		obj_btnBotonMarkPosition = (Button)findViewById(R.id.BtnBotonMarkPosition);
	}

	private void poner_manejador_boton_MarkPosition() {
		obj_btnBotonMarkPosition.setText("Mark First Position");

		obj_btnBotonMarkPosition.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {

				// Gestionar las pulsaciones del boton marcar
				if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
				{
					Log.i("OnBotonMarkPosition", "Posicion marcada con botón mientras grabo fichero");
					contador_Posi=contador_Posi+1;  //incremento contador
					obj_btnBotonMarkPosition.setText("Mark Next Position #"+(contador_Posi+1));
				} else {
					Log.i("OnBotonMarkPosition", "Posicion no marcada pues no estoy grabando fichero");
					if (contador_Posi == 0) {
						obj_btnBotonMarkPosition.setText("Mark First Position");
					} else {
						obj_btnBotonMarkPosition.setText("Mark Next Position #" + (contador_Posi + 1));
					}
					Toast.makeText(getApplicationContext(), "Not marked. Start saving first", Toast.LENGTH_SHORT).show();
				}


				if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
				{
					// Poner TimeStamp de la App (seg�n le llega el dato)
					long timestamp_ns_raw = System.nanoTime(); // in nano seconds
					if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
					{
						timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
					} else {
						timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
					}
					timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

					// grabar en fichero
					try {
						// POSI;Timestamp(s);Counter;Latitude(degrees); Longitude(degrees);floor ID(0,1,2..4);Building ID(0,1,2..3)
						String cadena_file=String.format(Locale.US,"\nPOSI;%.3f;%d;%.8f;%.8f;%d;%d",timestamp,contador_Posi,0.0,0.0,0,0);
						fout.write(cadena_file);
					} catch (IOException e) {// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}

	private void poner_manejador_boton1() {
		//...................................................................
		// Manejador de ToggleButton1 (mostrar o no datos generales de sensores)
		obj_toggleButton1.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (obj_toggleButton1.isChecked()) {
					obj_txtView1a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView2a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView3a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView4a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView5a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView6a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView7a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView8a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView9a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView10a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView11a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView12a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView13a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView14a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView20a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView21a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView22a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView23a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					Log.i("","Boton pulsado: Show Sensor Features");
				} else {
					obj_txtView1a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView2a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView3a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView4a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView5a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView6a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView7a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView8a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView9a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView10a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView11a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView12a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView13a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView14a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView20a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView21a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView22a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView23a.setVisibility(View.GONE);  // GONE 0x08
					Log.i("","Boton pulsado: Hide Sensor Features");
				}
	}
		});
	}

	private void poner_manejador_boton2() {
		//...................................................................
		// Manejador de ToggleButton2 (mostrar o no datos en tiempo real)
		obj_toggleButton2.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (obj_toggleButton2.isChecked()) {
					obj_txtView1b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView2b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView3b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView4b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView5b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView6b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView7b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView8b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView9b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView10b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView10c.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView11b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView11c.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView12b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView12c.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView13b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView14b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView14c.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView20b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView20c.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView21b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView22b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView23b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					Log.i("","Boton pulsado: Show Real-time Data");
				} else {
					obj_txtView1b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView2b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView3b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView4b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView5b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView6b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView7b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView8b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView9b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView10b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView10c.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView11b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView11c.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView12b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView12c.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView13b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView14b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView14c.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView20b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView20c.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView21b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView22b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView23b.setVisibility(View.GONE);  // GONE 0x08
					Log.i("","Boton pulsado: Hide Real-time Data");
				}
	}
		});
	}

	private void poner_manejador_botonSave() {
		if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
			finish();
		}
		//...................................................................
		// Manejador de ToggleButtonSave
		obj_ToggleButtonSave.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Probar si es posible realizar almacenamiento externo 
				boolean mExternalStorageAvailable = false;
				boolean mExternalStorageWriteable = false;
				String state = Environment.getExternalStorageState();

				if (Environment.MEDIA_MOUNTED.equals(state)) {
					// We can read and write the media
					mExternalStorageAvailable = mExternalStorageWriteable = true;
				} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
					// We can only read the media
					mExternalStorageAvailable = true;
					mExternalStorageWriteable = false;
				} else {
					// Something else is wrong. It may be one of many other states, but all we need
					//  to know is we can neither read nor write
					mExternalStorageAvailable = mExternalStorageWriteable = false;
				}
				Log.i("OnCreate","ALMACENAMIENTO EXTERNO:"+mExternalStorageAvailable+mExternalStorageWriteable);

				// Intentar almacenar o cerrar
				if (obj_ToggleButtonSave.isChecked())  // Comenzar a grabar
				{
					Log.i("OnCheckedchanged","Botón Save pulsado!. Me pongo a grabar...");

					long CpuTimeStamp = System.nanoTime(); // in nano seconds
					if (primer_sensor_cambia && obj_ToggleButtonSave.isChecked()) {
						tiempo_inicial_ns_raw=CpuTimeStamp;  // en nano segundos
						Log.i("","Tiempo inicial: "+tiempo_inicial_ns_raw+" ms");
						timestamp_Acce_last_update=0;
						timestamp_Gyro_last_update=0;
						timestamp_Magn_last_update=0;
						timestamp_Pres_last_update=0;
						timestamp_Ligh_last_update=0;
						timestamp_Prox_last_update=0;
						timestamp_Humi_last_update=0;
						timestamp_Temp_last_update=0;
						timestamp_Ahrs_last_update=0;
						timestamp_Gnss_last_update=0;
						timestamp_Wifi_last_update=0;
						timestamp_Blue_last_update=0;
						timestamp_Ble4_last_update=0;
						timestamp_Soun_last_update=0;
						timestamp_Rfid_last_update=0;
						timestamp_Imul_last_update=0;
						timestamp_Imux_last_update=0;
						timestamp_MIMU22BT_last_update=0;
					}

					SimpleDateFormat sf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",Locale.US);  // formato de la fecha
					Date fecha_actual = new Date();  // coger la fecha de hoy
					String str_fecha_actual = sf.format(fecha_actual);  // formatear fecha

					try {
						if (mExternalStorageAvailable) {
							File path = Environment.getExternalStoragePublicDirectory("LogFiles_GetSensorData");
							Log.i("OnCheckedChanged","Path donde guardo - ExternalStoragePublic: "+path);
							path.mkdirs();  // asegurarse que el directorio "./../LogFiles_GetSensorData" existe
							File fichero = new File(path.getAbsolutePath(), "logfile_"+str_fecha_actual+".txt");
							fout =	new OutputStreamWriter(	new FileOutputStream(fichero));
							Log.i("OncheckedChanged","Abierto fichero 'Externo' para escribir");
						} else {
							fout=	new OutputStreamWriter(	openFileOutput("logfile_"+str_fecha_actual+".txt", Context.MODE_PRIVATE));
							Log.i("OncheckedChanged","Abierto fichero 'Interno' para escribir");
						}

						Toast.makeText(getApplicationContext(), "Saving sensor data", Toast.LENGTH_SHORT).show();

						fout.write("% LogFile created by the 'GetSensorData' App for Android.");
						fout.write("\n% Date of creation: "+fecha_actual.toString());
						fout.write("\n% Developed by LOPSI research group at CAR-CSIC, Spain (http://www.car.upm-csic.es/lopsi)");
						fout.write("\n% Version 2.1 January 2018");
						fout.write("\n% The 'GetSensorData' program stores information from Smartphone/Tablet internal sensors (Accelerometers, Gyroscopes, Magnetometers, Pressure, Ambient Light, Orientation, Sound level, GPS/GNSS positio, WiFi RSS, Cellular/GSM/3G signal strength,...) and also from external devices (e.g. RFCode RFID reader, XSens IMU, LPMS-B IMU or MIMU22BT)");
						fout.write("\n%\n% Phone used for this logfile:");
						fout.write("\n% Manufacturer:            \t"+phone_manufacturer);
						fout.write("\n% Model:                   \t"+phone_model);
						fout.write("\n% API Android version:     \t"+phone_version);
						fout.write("\n% Android version Release: \t"+phone_versionRelease);
						fout.write("\n%\n% LogFile Data format:");
						fout.write("\n% Accelerometer data: \t'ACCE;AppTimestamp(s);SensorTimestamp(s);Acc_X(m/s^2);Acc_Y(m/s^2);Acc_Z(m/s^2);Accuracy(integer)'");
						fout.write("\n% Gyroscope data:     \t'GYRO;AppTimestamp(s);SensorTimestamp(s);Gyr_X(rad/s);Gyr_Y(rad/s);Gyr_Z(rad/s);Accuracy(integer)'");
						fout.write("\n% Magnetometer data:  \t'MAGN;AppTimestamp(s);SensorTimestamp(s);Mag_X(uT);;Mag_Y(uT);Mag_Z(uT);Accuracy(integer)'");
						fout.write("\n% Pressure data:      \t'PRES;AppTimestamp(s);SensorTimestamp(s);Pres(mbar);Accuracy(integer)'");
						fout.write("\n% Light data:         \t'LIGH;AppTimestamp(s);SensorTimestamp(s);Light(lux);Accuracy(integer)'");
						fout.write("\n% Proximity data:     \t'PROX;AppTimestamp(s);SensorTimestamp(s);prox(?);Accuracy(integer)'");
						fout.write("\n% Humidity data:      \t'HUMI;AppTimestamp(s);SensorTimestamp(s);humi(Percentage);Accuracy(integer)'");
						fout.write("\n% Temperature data:   \t'TEMP;AppTimestamp(s);SensorTimestamp(s);temp(Celsius);Accuracy(integer)'");
						fout.write("\n% Orientation data:   \t'AHRS;AppTimestamp(s);SensorTimestamp(s);PitchX(deg);RollY(deg);YawZ(deg);Quat(2);Quat(3);Quat(4);Accuracy(int)'");
						fout.write("\n% GNSS/GPS data:      \t'GNSS;AppTimestamp(s);SensorTimeStamp(s);Latit(deg);Long(deg);Altitude(m);Bearing(deg);Accuracy(m);Speed(m/s);SatInView;SatInUse'");
						fout.write("\n% WIFI data:          \t'WIFI;AppTimestamp(s);SensorTimeStamp(s);Name_SSID;MAC_BSSID;Frequency;RSS(dBm);'"); // Added frequency by jtorres
						// fout.write("\n% WIFI data:          \t'WIFI;AppTimestamp(s);SensorTimeStamp(s);Name_SSID;MAC_BSSID;RSS(dBm);'");               original
						fout.write("\n% Bluetooth data:     \t'BLUE;AppTimestamp(s);Name;MAC_Address;RSS(dBm);'");
						fout.write("\n% BLE 4.0 data:       \t'BLE4;AppTimestamp(s);iBeacon;MAC;RSSI(dBm);Power;MajorID;MinorID;UUID'"); // Added power and UUID by jtorres
						// fout.write("\n% BLE 4.0 data:       \t'BLE4;AppTimestamp(s);iBeacon;MAC;RSSI(dBm);MajorID;MinorID;'");               original
						fout.write("\n% BLE 4.0 data:       \t'BLE4;AppTimestamp(s);Eddystone;MAC;RSSI(dBm);instanceID;OptionalTelemetry[voltaje;temperature;uptime;count]");
						fout.write("\n% Sound data:         \t'SOUN;AppTimestamp(s);RMS;Pressure(Pa);SPL(dB);'");
						fout.write("\n% RFID Reader data:   \t'RFID;AppTimestamp(s);ReaderNumber(int);TagID(int);RSS_A(dBm);RSS_B(dBm);'");
						fout.write("\n% IMU XSens data:     \t'IMUX;AppTimestamp(s);SensorTimestamp(s);Counter;Acc_X(m/s^2);Acc_Y(m/s^2);Acc_Z(m/s^2);Gyr_X(rad/s);Gyr_Y(rad/s);Gyr_Z(rad/s);Mag_X(uT);;Mag_Y(uT);Mag_Z(uT);Roll(deg);Pitch(deg);Yaw(deg);Quat(1);Quat(2);Quat(3);Quat(4);Pressure(mbar);Temp(Celsius)'");
						fout.write("\n% IMU LPMS-B data:    \t'IMUL;AppTimestamp(s);SensorTimestamp(s);Counter;Acc_X(m/s^2);Acc_Y(m/s^2);Acc_Z(m/s^2);Gyr_X(rad/s);Gyr_Y(rad/s);Gyr_Z(rad/s);Mag_X(uT);;Mag_Y(uT);Mag_Z(uT);Roll(deg);Pitch(deg);Yaw(deg);Quat(1);Quat(2);Quat(3);Quat(4);Pressure(mbar);Temp(Celsius)'");
						fout.write("\n% IMU MIMU22BT data:  \t'IMUI;AppTimestamp(s);Packet_count;Step_Counter;delta_X(m);delta_Y(m);delta_Z(m);delta_theta(degrees);Covariance4x4[1:10]'");
						fout.write("\n% POSI Reference:    	\t'POSI;Timestamp(s);Counter;Latitude(degrees); Longitude(degrees);floor ID(0,1,2..4);Building ID(0,1,2..3);'");
						fout.write("\n% ");
						fout.write("\n% Note that there are two timestamps: ");
						fout.write("\n%  -'AppTimestamp' is set by the Android App as data is read. It is not representative of when data is actually captured by the sensor (but has a common time reference for all sensors)");
						fout.write("\n%  -'SensorTimestamp' is set by the sensor itself (the delta_time=SensorTimestamp(k)-SensorTimestamp(k-1) between two consecutive samples is an accurate estimate of the sampling interval). This timestamp is better for integrating inertial data. \n");
					} catch (Exception ex) {
						Log.e("Ficheros", "Error al escribir fichero a memoria del dispositivo");
					}
					// Lanzar el Timer a 1Hz de Pintar los segundos trascurridos con timer
					poner_manejador_Reloj();
					timerReloj = new Timer("Hilo Timer Reloj");
					timerReloj.schedule(TaskReloj, 1000, 1000);  // llamar a Timer cada 1 segundo (con retardo inicial de 1s)
				} else  // Parar de grabar
				{
					// Parar el Timer a 1Hz de Pintar los segundos trascurridos con timer
					timerReloj.cancel();

					Log.i("Oncheckedchanged","Botón Save pulsado para parar de grabar!. Cierro el fichero");
					try {
						primer_sensor_cambia=true;  // resetear marca tiempo
						tiempo_inicial_ns_raw=0;
						fout.close();
						Toast.makeText(getApplicationContext(), "End of Saving", Toast.LENGTH_SHORT).show();

					} catch (Exception ex) {
						Log.e("Ficheros", "Error al intentar cerrar el fichero de memoria interna");
					}

				}
			}
		});
	}

	private void poner_manejador_Reloj() {

		// Manejador del Timer para hacer pintar el reloj
		TaskReloj = new TimerTask() {
			public void run() {
				handlerReloj.post(new Runnable() {
					public void run() {
				// ................Do something at Timer rate.....................
				// Averiguar los segundos trascurridos desde inicio de grabaci�n
				long timestamp_ns_raw = System.nanoTime(); // in nano seconds
				if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
						{
							timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
						} else {
							timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
						}
				long segundos_trascurridos = (long)(((double)(timestamp_ns))*1E-9);  // de nano_s a segundos (segundos desde el inicio de la grabacion)
				// pintar los segundos en alg�n sitio d ela pantalla (p.ej. en el boton de grabar)
				if (obj_ToggleButtonSave.isChecked())  // Comenzar a grabar
				{
					obj_ToggleButtonSave.setText("Stop Saving.\n "+Long.toString(segundos_trascurridos)+" s");
				}
			}
		} );
	}
		};
	}


	private void poner_manejador_WiFiScan() {
		// Register a broadcast receiver that listens for WiFi scan results.
		//.......................................................
		wifibroadcastreceiver= new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// Poner TimeStamp de la App (seg�n le llega el dato)
				long timestamp_ns_raw = System.nanoTime(); // in nano seconds
				if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
				{
					timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
				} else {
					timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
				}
				timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

				// See WiFi Scan results
				List<ScanResult> results = wifimanager.getScanResults();
				int numero_AP=results.size();
				Log.i("WiFi Scan"," Numero AP: "+numero_AP);
				String strWifi="";
				for (ScanResult result : results) {
					String SSID=result.SSID;
					String BSSID=result.BSSID;
					int frequency = result.frequency;
					long tiempo_us=result.timestamp;  // en micro segundos (solo a partir de API 17, JeallyBean 4.2 o superior)
					double SensorTimeStamp = ((double)(tiempo_us))*1E-6;  // de micro_s a segundos
					int RSS=result.level;
					strWifi=strWifi+ "\n\t- " + SSID + ",\t" + BSSID + ",\tRSS:" +RSS+" dBm";
					if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
					{
						try {
							String cadena_file=String.format(Locale.US,"\nWIFI;%.3f;%.3f;%s;%s;%d;%d",timestamp,SensorTimeStamp,SSID,BSSID,frequency ,RSS); // jtorres added frequency
							//String cadena_file = String.format(Locale.US, "\nWIFI;%.3f;%.3f;%s;%s;%d", timestamp, SensorTimeStamp, SSID, BSSID, RSS);               original
							fout.write(cadena_file);
						} catch (IOException e) {// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				String texto="\tNumber of Wifi APs: "+numero_AP+strWifi;
				obj_txtView11c.setText(texto);
				// get freq update
				contador_Wifi++;
				freq_medida_Wifi=(float) (0.8*freq_medida_Wifi	+ 0.2/(timestamp-timestamp_Wifi_last));
				timestamp_Wifi_last=timestamp;
				// Mostrar features de WiFi en UI
				if (wifimanager.isWifiEnabled()==true)  // si est� activo WiFi
				{
					obj_txtView11.setBackgroundColor(0xFF00FF00);  // green
					obj_txtView11.setText(" WIFI: Switched ON");
					texto_Wifi_Features =" WiFi MAC address: "+wifimanager.getConnectionInfo().getMacAddress();
					obj_txtView11a.setText(texto_Wifi_Features);
				}
				// mostrar el WiFi connection status
				WifiInfo wifiinfo = wifimanager.getConnectionInfo();
				if (wifiinfo.getBSSID() != null) {
					//Log.i("OnCreate-Wifi","WiFi Status: " + wifiinfo.toString());
					String cadena_display=String.format(Locale.US,"\tConnected to: %s\n\tBSSID: %s\n\tRSSI: %d dBm \n\tLinkSpeed: %d Mbps\n\t\t\t\t\t\t\t\tFreq: %5.1f Hz",wifiinfo.getSSID(),wifiinfo.getBSSID(),wifiinfo.getRssi(),wifiinfo.getLinkSpeed(),freq_medida_Wifi);
					obj_txtView11b.setText(cadena_display);
				} else {
					obj_txtView11b.setText(" No connection detected");
				}
			}
		};

		// Manejador del Timer para hacer WiFi scan
		scanTaskWifi = new TimerTask() {
			public void run() {
				handlerWifi.post(new Runnable() {
					public void run() {
				// ................Do something at Timer rate.....................
				// Scan WiFi
				if (wifimanager!=null && wifimanager.isWifiEnabled()==true)  // si est� activo WiFi
				{
					wifimanager.startScan(); // Lanzar Scanear WiFis
				}
			}
		} );
	}
		};
	}

	private void poner_manejador_BLE() {
		Log.i("BLE"," START: Pongo manejador BLE BeaconManager");
		beaconManager = new BeaconManager(this);
		//region = new Region("ranged region",
		//		UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);
		region = new Region("Todo tipo de motas",null,null, null);

		beaconManager.setRangingListener(new BeaconManager.RangingListener() {  // iBeacon tags
			@Override
			public void onBeaconsDiscovered(Region region, List<Beacon> list_BLE) {
				int Major=0;
				int Minor=0;
				int Rssi=0;
				int Power=0;
				MacAddress Mac_iBeacon;
				String strBLE="";
				String UUID="";

				if (!list_BLE.isEmpty()) {

					int numero_BLE=list_BLE.size();

					for (Beacon mote_BLE : list_BLE) {
						Major=mote_BLE.getMajor();
						Minor=mote_BLE.getMinor();
						Rssi=mote_BLE.getRssi();
						Mac_iBeacon=mote_BLE.getMacAddress();
						Power=mote_BLE.getMeasuredPower();
						UUID= mote_BLE.getProximityUUID().toString();
						//UUID=mote_BLE.getProximityUUID();
						String MAC_str=Mac_iBeacon.toStandardString();

						strBLE=strBLE+ "\n\t-"+MAC_str+"\t\tRSS:"+Rssi+"dBm"+"\t\tID: " + Major + ":" + Minor;
						if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
						{
							// Poner TimeStamp de la App (seg�n le llega el dato)
							long timestamp_ns_raw = System.nanoTime(); // in nano seconds
							if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
							{
								timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
							} else {
								timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
							}
							timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

							// grabar en fichero
							try {
								// 'BLE4;AppTimestamp(s);"iBeacon";MAC;RSSI(dBm);MajorID;MinorID;'
								// 'BLE4;AppTimestamp(s);"Eddystone";MAC;RSSI(dBm);instanceID;OptionalTelemetry[voltaje;temperature;uptime;count]'
								String cadena_file=String.format(Locale.US,"\nBLE4;%.3f;%s;%s;%d;%d;%d;%d;%s",timestamp,"iBeacon",MAC_str,Rssi,Power,Major,Minor,UUID);  // jtorres added Power and UUID
								//String cadena_file = String.format(Locale.US, "\nBLE4;%.3f;%s;%s;%d;%d;%d", timestamp, "iBeacon", MAC_str, Rssi, Major, Minor);           original
								fout.write(cadena_file);
							} catch (IOException e) {// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					String texto="\tNumber of iBEACON BLE motes: "+numero_BLE+strBLE;
					obj_txtView14b.setText(texto);
					Log.i("BLE Scan",texto);
				} else {
					Log.i("BLE Scan"," No hay motas BLE");
					String texto="\tNo iBEACON BLE motes in range";
					obj_txtView14b.setText(texto);
				}
			}
		});


		beaconManager.setEddystoneListener(new BeaconManager.EddystoneListener() {  // Eddystone tags
			@Override
			public void onEddystonesFound(List<Eddystone> list_eddystones) {
				String str_eddystones_BLE = "";
				MacAddress Mac_EddyStone;
				String instanceID;
				int Rssi=0;
				EddystoneTelemetry telemetria;
				int tel_voltaje;
				long tel_contador;
				double tel_temperatura;
				long tel_uptime;

				if (!list_eddystones.isEmpty()) {

					int numero_eddystones_BLE = list_eddystones.size();


					for (Eddystone mote_eddystones_BLE : list_eddystones) {
						Mac_EddyStone=mote_eddystones_BLE.macAddress;
						instanceID = mote_eddystones_BLE.instance;
						Rssi=mote_eddystones_BLE.rssi;
						telemetria=mote_eddystones_BLE.telemetry;
						if (telemetria != null) {
							tel_voltaje = telemetria.batteryVoltage;
							tel_temperatura = (double)(((int)(telemetria.temperature*10))/10);
							tel_contador = telemetria.packetCounter;
							tel_uptime = telemetria.uptimeMillis/(1000*3600);  // en horas
						} else {
							tel_voltaje = -1;
							tel_temperatura = -1;
							tel_contador = -1;
							tel_uptime = -1;
						}
						String MAC_str=Mac_EddyStone.toStandardString();

						str_eddystones_BLE=str_eddystones_BLE + "\n\t-"+MAC_str+",\t\tRSS:" +Rssi+"dBm"+",\t\tID: " + instanceID +
								         "\n  Vols:"+tel_voltaje+"\tTemp:"+tel_temperatura+"\tUpHours:"+tel_uptime+"\tCount:"+tel_contador;

						if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
						{
							// Poner TimeStamp de la App (seg�n le llega el dato)
							long timestamp_ns_raw = System.nanoTime(); // in nano seconds
							if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
							{
								timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
							} else {
								timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
							}
							timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

							// grabar en fichero
							try {
								// 'BLE4;AppTimestamp(s);"iBeacon";MAC;RSSI(dBm);MajorID;MinorID;'
								// 'BLE4;AppTimestamp(s);"Eddystone";MAC;RSSI(dBm);instanceID;OptionalTelemetry[voltaje;temperature;uptime;count]'
								String cadena_file=String.format(Locale.US,"\nBLE4;%.3f;%s;%s;%d;%s;%d;%.1f;%d;%d",timestamp,"Eddystone",MAC_str,Rssi,instanceID,tel_voltaje,tel_temperatura,tel_uptime,tel_contador);
								fout.write(cadena_file);
							} catch (IOException e) {// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					String texto="\tNumber of EDDYSTONE BLE motes: "+numero_eddystones_BLE+str_eddystones_BLE;
					obj_txtView14c.setText(texto);
					Log.i("eddystones BLE Scan", texto);
				} else {
					Log.i("eddystones BLE Scan", " No hay motas eddystones BLE");
					String texto="\tNo EDDYSTONE BLE motes in range";
					obj_txtView14c.setText(texto);
				}
			}
		});

		if (flag_EstimoteTelemetry) {   // No creo el lisener a no ser que quiera la telemetria en el formato especial de "Estimote"
			beaconManager.setTelemetryListener(new BeaconManager.TelemetryListener() {
				@Override
				public void onTelemetriesFound(List<EstimoteTelemetry> list_telemetries) {
					if (!list_telemetries.isEmpty()) {
						int numero_telemetries_BLE = list_telemetries.size();
						Log.i("telemetries BLE Scan", " Numero telemetries BLE: " + numero_telemetries_BLE);

						for (EstimoteTelemetry tlm : list_telemetries) {
							Log.i("TELEMETRY", "BLE beaconID: " + tlm.deviceId +
									", temperature: " + tlm.temperature + " °C" +
									", batteryPercentage: " + tlm.batteryPercentage + " %" +
									", pressure: " + tlm.pressure + " mbar" +
									", rssi: " + tlm.rssi + " dB" +
									", ambientLight: " + tlm.ambientLight + " lux");
						}
					} else {
						Log.i("telemetries BLE Scan", " No hay telemetries BLE");

					}

				}
			});
		}

		Log.i("BLE","END: Pongo manejador BLE BeaconManager");
	}

	private void poner_manejador_BluetoothScan() {
		// Broadcast receiver that listens for Bluetooth (cuando finaliza Discovery)
		//.......................................
		bluetoothDiscoveryMonitor = new BroadcastReceiver() {
			String dFinished = BluetoothAdapter.ACTION_DISCOVERY_FINISHED;

			@Override
			public void onReceive(Context context, Intent intent) {
				if (dFinished.equals(intent.getAction())) {
					// Discovery has completed.
					Log.i("OnBluetooth", "Discovery Finished.");
					String cadena_display="";
					cadena_display=cadena_display+"\tDiscovered Bluetooth devices:"+bluetoothdeviceList.size();
					for (BluetoothDevice device : bluetoothdeviceList) {
						cadena_display=cadena_display+"\n\t-"+device.getName()+" Address: "+device.getAddress()+" RSSI: n.a.";
					}
					// get freq update
					contador_Blue++;
					freq_medida_Blue=(float) (1/(timestamp-timestamp_Blue_last));
					timestamp_Blue_last=timestamp;
					cadena_display=String.format(Locale.US,"\t\t\t\t\t\t\t\tFreq: %5.2f Hz\n",freq_medida_Blue)+cadena_display;
					obj_txtView12c.setText(cadena_display);
					// mostrar dispositivos bonded/paired 
					Set<BluetoothDevice> bondedDevicesBlue = bluetooth.getBondedDevices();
					cadena_display="";
					cadena_display=cadena_display+"\tBonded Bluetooth devices:"+bondedDevicesBlue.size();
					for (BluetoothDevice device : bondedDevicesBlue) {
						cadena_display=cadena_display+"\n\t-"+device.getName()+" Address: "+device.getAddress();
					}
					obj_txtView12b.setText(cadena_display);

					//	String Address=remoteDevice.getAddress();
					//	int bonded=remoteDevice.getBondState();
					//	String name=remoteDevice.getName();
					//	ParcelUuid[] uuids=remoteDevice.getUuids();
					//	String todostr=remoteDevice.toString();


				}
			}
		};
		// Broadcast receiver that listens for Bluetooth (cada vez que encuentra un dispositivo durante el discovery)
		//.......................................
		bluetoothdiscoveryResult = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				// Poner TimeStamp de la App (seg�n le llega el dato)
				long timestamp_ns_raw = System.nanoTime(); // in nano seconds
				if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
				{
					timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
				} else {
					timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
				}
				timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

				BluetoothDevice remoteDevice =intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				String remoteDeviceName =intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
				//String remoteDeviceRSSI =intent.getStringExtra(BluetoothDevice.EXTRA_RSSI);  // No lee ningun RSSI (devuelve null =< no lo uso)
				String remoteDeviceAddress=remoteDevice.getAddress();
				Log.i("Bluetooth", "Discovered " + remoteDeviceName+" Address: "+remoteDeviceAddress+" RSSI: n.a.");
				if (bluetoothdeviceList.contains((BluetoothDevice) remoteDevice) == false) {
					bluetoothdeviceList.add(remoteDevice);  // Si no estaba ya => Lo voy metiendo en la lista
				}
				if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
				{
					try {
						String Address=remoteDevice.getAddress();
						String name=remoteDevice.getName();
						int RSS=0;
						String cadena_file=String.format(Locale.US,"\nBLUE;%.3f;%s;%s;%d",timestamp,name,Address,RSS);
						fout.write(cadena_file);
					} catch (IOException e) {// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};

		// Manejador del Timer para hacer Bluetooth Scan (discovery)
		scanTaskBlue = new TimerTask() {
			public void run() {
				handlerBlue.post(new Runnable() {
					public void run() {
				// ................Do something at Timer rate.....................
				// Scan Bluetooth
						if (bluetooth != null && bluetooth.isEnabled() && !bluetooth.isDiscovering() && Flag_Discover_Bluetooth == true) {
					bluetoothdeviceList.clear();
					bluetooth.startDiscovery();
					Log.i("TimerBlue","Bluettoth discovery started");
				}
			}
		} );
			}
		};

	}

	//=================Manejador Audio (microfono)=====================
	private void poner_manejador_Audio() {
		Log.i("Audio","START Audio Recording inside Thread");


		mRecordAudioListener = new OnRecordPositionUpdateListener() {
			@Override
			public void onPeriodicNotification(AudioRecord recorder) {
				// Poner TimeStamp de la App (seg�n le llega el dato)
				long timestamp_ns_raw = System.nanoTime(); // in nano seconds
				if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
				{
					timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
				} else {
					timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
				}
				timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

				// Me han avisado de que hay nuevos datos => los proceso  
				//	Log.i("Audio", "Entro en Listener de Microfono");
				// Leo el buffer
				//short[] buffer=mMicrophone.getbuffer();
				short[] buffer=mMicrophone.buffer;
				// Proceso los datos de audio
				double rms = 0;
				for (int i = 0; i < buffer.length; ++i) {
					rms += Math.pow(Math.abs(buffer[i]),2);
				}
				rms=Math.sqrt(rms/buffer.length);
				double pressure=rms/Short.MAX_VALUE; // asumo que el sensor satura con 20 Pascales (120dB)
				double SPL = 20 * Math.log10(pressure/2E-5);  // referido a presion de 2E-5 Pa (limite audici�n humana)
				//	Log.i("Audio", "Buffer size: "+buffer.length);
				// Freq:
				contador_Soun++;
				if (timestamp - timestamp_Soun_last > 0) {
					freq_medida_Soun = (float) (0.9 * freq_medida_Soun + 0.1 / (timestamp - timestamp_Soun_last));
				}
				timestamp_Soun_last=timestamp;
				if (timestamp-timestamp_Soun_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
				{
					// Muestro resultados
					String cadena_display=String.format(Locale.US,"\tRMS: \t\t\t%6.1f \n\tPressure: \t%6.2f \tmPa\n\tSPL: \t\t\t%6.1f \tdB\n\t\t\t\t\t\t\t\tFreq: %5.1f Hz",rms,pressure*1000,SPL,freq_medida_Soun);
					obj_txtView13b.setText(cadena_display);  //+"\n Raw data: "+buffer[1]+"\t"+buffer[2]+"\t"+buffer[3]
					timestamp_Soun_last_update=timestamp;
				}
				// Grabar en fichero
				if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
				{
					try {
						String cadena_file=String.format(Locale.US,"\nSOUN;%.3f;%.2f;%.5f;%.2f",timestamp,rms,pressure,SPL);
						/*	for (int i = 0; i <   44; ++i)  // buffer.length  o menos p.ej.: 20 muestras
						{
							cadena_file=cadena_file+";"+buffer[i];
						} */
						fout.write(cadena_file);
					} catch (IOException e) {// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onMarkerReached(AudioRecord recorder) {
			}
		};
	}

	//===================Manejador RFID==========================
	private void poner_manejador_RFIDReader() {
		handlerRFID=new Handler() {
	//		int num_connected_RFIDReader=22;
	//		ArrayList<String> readernameList = new ArrayList<String>();  // Me gusta tiene los metodos: "contains" y "add"

//			int num_connected_RFIDReader;
//			ArrayList<String> readernameList;
//			
//			void Handler(){
//				num_connected_RFIDReader=0;
//				readernameList = new ArrayList<String>();  // Me gusta tiene los metodos: "contains" y "add"
//			}
			@Override
			public void handleMessage(Message msg) {
				//add your text
				//values_RFID ;  // Tag_ID, RSS_A, RSS_B
				Log.i("HandlerRFID","Actualizando UI con datos de RFID Reader");
				Bundle data=msg.getData();
				String mensajetype=data.getString("MensajeType");
				String readername=data.getString("ReaderName");
				if (mensajetype.equals("Connect")) {
					Boolean connected=data.getBoolean("Connected");
					if (connected) {
						// Header
						obj_txtView20.setText(" RFID: RFCode M220 Reader detected");
						obj_txtView20.setBackgroundColor(0xFF00FF00);  // green color
						// Features
						obj_txtView20a.setText("\tManufacturer: RFCode\n\tModel: M220 Portable Reader\n\tRF Frequency: 433 MHz\n\tNumber of Antennas: 2\n\tConnectivity: Bluetooth & USB");
						// Info connected readers
						num_connected_RFIDReader++;
						Log.i("ConnectRFID","RFIDReader "+readername+" Connected. "+num_connected_RFIDReader);

						if (RFIDreadernameList.contains(readername) == false) {
							RFIDreadernameList.add(readername);
						}
						// Poner lista de lectores
						String string_text=" Number of Connected RFID readers: "+num_connected_RFIDReader;
						for (String name : RFIDreadernameList) {
							string_text=string_text+"\n\t-"+name;
						}
						obj_txtView20b.setText(string_text);

					}

				}
				if (mensajetype.equals("RFID_Data")) {
					// Poner TimeStamp de la App (seg�n le llega el dato)
					long timestamp_ns_raw = System.nanoTime(); // in nano seconds
					if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
					{
						timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
					} else {
						timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
					}
					timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

					int RSS_A=data.getInt("RSS_A");
					int RSS_B=data.getInt("RSS_B");
					long TagID=data.getLong("TagID");
					// Real-time tag Data:
					String cadena_RFID=" Real-time RFID data:\n\t-Reader: "+readername+"\t Tag ID: "+TagID+"\n\t\tRSS_A: -"+RSS_A+"dBm  RSS_B: -"+RSS_B+" dBm";
					// Freq:
					contador_Rfid++;
					if (timestamp - timestamp_Rfid_last > 0.005) {
						freq_medida_Rfid = (float) (0.99 * freq_medida_Rfid + 0.01 / (timestamp - timestamp_Rfid_last));
					}
					timestamp_Rfid_last=timestamp;
					cadena_RFID=cadena_RFID+String.format(Locale.US,"\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz ",freq_medida_Rfid);
					obj_txtView20c.setText(cadena_RFID);
					/*if (TagID==67941)  // Para buscar un tag concreto
					{
						obj_txtView20c.setText(cadena_RFID);
					}*/
					// Grbar en fichero
					if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
					{
						try {
							int readernumber=Integer.parseInt(readername.substring(readername.length()-2, readername.length()));
							String cadena_file=String.format(Locale.US,"\nRFID;%.3f;%d;%d;%d;%d",timestamp,readernumber,TagID,RSS_A,RSS_B);
							fout.write(cadena_file);
						} catch (IOException e) {// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
			}
		};
	}
	//=======================poner_manejador_LPMS=======================0

	private void poner_manejador_LPMS(){
		handlerLPMS=new Handler() {

			@Override
			public void handleMessage(Message msg) {
				/**
				 * handle MTi data and connection messages
				 */
				Bundle data=msg.getData();
				String mensajetype=data.getString("MensajeType");
				String readername=data.getString("ReaderName");
				if (mensajetype.equals("Connect")) {
					Boolean connected=data.getBoolean("Connected");
					if (connected){
						// Header
						obj_txtView22.setText(" IMUL: LPMS-B Inertial Sensor detected");
						obj_txtView22.setBackgroundColor(0xFF00FF00);  // green color
						// Features
						obj_txtView22a.setText("\tManufacturer: Life Performance Research\n\tModel: LPMS-B IMU\n\tConfigured to transmit: Acc, Gyr, Mag, Quat, Euler, Pres, Temp\n"+
								      "\tRanges: Acc (4G), Gyr (2000�/s), Mag (250uT)\n\tSamplig Frequency: 100Hz\n\tConnectivity: Bluetooth");
					} else {
						// Header
						obj_txtView22.setText(" IMUL: No LPMS-B Inertial Sensor detected");
						obj_txtView22.setBackgroundColor(0xFFFF0000);  // red color
						// Features
						obj_txtView22a.setText("\tNo Features");
					}
				}

				if (mensajetype.equals("IMU_Data")) {
					// Poner TimeStamp de la App (seg�n le llega el dato)
					long timestamp_ns_raw = System.nanoTime(); // in nano seconds
					if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
					{
						timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
					} else {
						timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
					}
					timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

					//Log.i("LpmsB","[LPMS handler] Dato recibido");
					float[] acceleration = new float[3];
					acceleration[0]=data.getFloat("Accelerations_x");
					acceleration[1]=data.getFloat("Accelerations_y");
					acceleration[2]=data.getFloat("Accelerations_z");
					float[] turnRate = new float[3];
					turnRate[0]=data.getFloat("TurnRates_x");
					turnRate[1]=data.getFloat("TurnRates_y");
					turnRate[2]=data.getFloat("TurnRates_z");
					float[] magneticField = new float[3];
					magneticField[0]=data.getFloat("MagneticFields_x");
					magneticField[1]=data.getFloat("MagneticFields_y");
					magneticField[2]=data.getFloat("MagneticFields_z");
					float[] euler = new float[3];
					euler[0]=data.getFloat("Euler_Roll");
					euler[1]=data.getFloat("Euler_Pitch");
					euler[2]=data.getFloat("Euler_Yaw");
					float[] quaternions = new float[4];
					quaternions[0]=data.getFloat("quaternions1");
					quaternions[1]=data.getFloat("quaternions2");
					quaternions[2]=data.getFloat("quaternions3");
					quaternions[3]=data.getFloat("quaternions4");
					float temperature=data.getFloat("Temperature");
					float pressure=data.getFloat("Pressure");
					long counter=data.getLong("Counter");
					//long time=data.getLong("Time");
					float SensorTimestamp=data.getFloat("timeStamp");

					// Freq:
					contador_Imul++;
					if (SensorTimestamp - timestamp_Imul_last > 0.002) {
						freq_medida_Imul = (float) (0.995 * freq_medida_Imul + 0.005 / (SensorTimestamp - timestamp_Imul_last));
					}
					timestamp_Imul_last=SensorTimestamp;

					if (timestamp-timestamp_Imul_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
					{
						String cadena_display=String.format(Locale.US,"\tAcc(X): \t%10.5f \tm/s^2\n\tAcc(Y): \t%10.5f \tm/s^2\n\tAcc(Z): \t%10.5f \tm/s^2\n",acceleration[0],acceleration[1],acceleration[2]);
						cadena_display=cadena_display+String.format(Locale.US,"\tGyr(X): \t%10.5f \trad/s\n\tGyr(Y): \t%10.5f \trad/s\n\tGyr(Z): \t%10.5f \trad/s\n",turnRate[0],turnRate[1],turnRate[2]);
						cadena_display=cadena_display+String.format(Locale.US,"\tMag(X): \t%10.5f \tuT\n\tMag(Y): \t%10.5f \tuT\n\tMag(Z): \t%10.5f \tuT\n",magneticField[0],magneticField[1],magneticField[2]);
						cadena_display=cadena_display+String.format(Locale.US,"\tEuler(�): \t%5.1f \t%5.1f \t%5.1f\n",euler[0],euler[1],euler[2]);
						cadena_display=cadena_display+String.format(Locale.US,"\tQuater: \t%3.4f \t%3.4f \t%3.4f \t%3.4f\n",quaternions[0],quaternions[1],quaternions[2],quaternions[3]);
						cadena_display=cadena_display+String.format(Locale.US,"\tPressu: \t%10.2f \tmbar\n\tTemp: \t%10.1f \t�C\n\tTimeStamp: \t%10.1f \ts\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",pressure,temperature,SensorTimestamp,freq_medida_Imul);
						obj_txtView22b.setText(cadena_display);
						timestamp_Imul_last_update=timestamp;
					}

					// Grbar en fichero
					if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
					{
						try {
							String cadena_file=String.format(Locale.US,"\nIMUL;%.3f;%.4f;%d;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.3f;%.2f",
									timestamp,SensorTimestamp,counter,acceleration[0],acceleration[1],acceleration[2],turnRate[0],turnRate[1],turnRate[2],
									magneticField[0],magneticField[1],magneticField[2],euler[0],euler[1],euler[2],quaternions[0],quaternions[1],quaternions[2],
									quaternions[3],pressure,temperature);
							fout.write(cadena_file);
						} catch (IOException e) {// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}


				}
			}
		};
	}


	//=======================poner_manejador_MIMU22BT=======================0

	private void poner_manejador_MIMU22BT(){
		handlerMIMU22BT=new Handler() {

			@Override
			public void handleMessage(Message msg) {
				/**
				 * handle MTi data and connection messages
				 */
				Log.i("[MIMU22BT handler]","Actualizando UI con datos de MIMU22BT");
				Bundle data=msg.getData();
				String mensajetype=data.getString("MensajeType");
				String readername=data.getString("ReaderName");
				if (mensajetype.equals("Connect")) {
					Boolean connected=data.getBoolean("Connected");
					if (connected){
						// Header
						obj_txtView23.setText(" IMUI: MIMU22BT Inertial Sensor detected");
						obj_txtView23.setBackgroundColor(0xFF00FF00);  // green color
						// Features
						obj_txtView23a.setText("\tManufacturer: Inertial Elements\n\tModel: MIMU22BT IMU\n\tConfigured to transmit: delta_X,Y,Z,theta,step_count\n"+
								"\tRanges: Acc (+/-4G), Gyr (1000 degrees/s)\n\tSamplig Frequency: 125Hz\n\tConnectivity: Bluetooth 3.0");
					} else {
						// Header
						obj_txtView23.setText(" IMUI: No MIMU22BT Inertial Sensor detected");
						obj_txtView23.setBackgroundColor(0xFFFF0000);  // red color
						// Features
						obj_txtView23a.setText("\tNo Features");
					}
				}

				if (mensajetype.equals("IMU_PDR_Data")) {
					// Poner TimeStamp de la App (según le llega el dato)
					long timestamp_ns_raw = System.nanoTime(); // in nano seconds
					if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
					{
						timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
					} else {
						timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
					}
					timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

					Log.i("MIMU22BT","[MIMU22BT handler] Dato recibido");
					float[] deltas = new float[4];
					deltas[0]=data.getFloat("Dx");
					deltas[1]=data.getFloat("Dy");
					deltas[2]=data.getFloat("Dz");
					deltas[3]=data.getFloat("D_Theta");
					int packet_number=data.getInt("Packet");
					int step_counter=data.getInt("Steps");
					float[] Cov= new float[10];
					Cov=data.getFloatArray("Cov");

					// Freq:
					contador_MIMU22BT++;
					if (timestamp - timestamp_MIMU22BT_last > 0.002) {
						freq_medida_MIMU22BT = (float) (0.995 * freq_medida_MIMU22BT + 0.005 / (timestamp - timestamp_MIMU22BT_last));
					}
					timestamp_MIMU22BT_last=timestamp;

					if (timestamp-timestamp_MIMU22BT_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
					{
						String cadena_display=String.format(Locale.US,"\tDeltaX: \t%5.3f \tm \tDeltaY: \t%5.3f \tm \tDeltaZ: \t%5.3f \tm\n \tDeltaTheta: \t%5.3f \tdegrees\n",deltas[0],deltas[1],deltas[2],deltas[3]);
						cadena_display=cadena_display+String.format(Locale.US,"\tPacket: \t%d \tStep_count: \t%d \tsteps \n\tTimestamp: \t%.3f\n",packet_number,step_counter,timestamp);
						obj_txtView23b.setText(cadena_display);
						timestamp_MIMU22BT_last_update=timestamp;
					}

					// Grabar en fichero
					if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
					{
						try {
							String cadena_file=String.format(Locale.US,"\nIM1P;%.3f;%d;%d;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f",
									timestamp,packet_number,step_counter,deltas[0],deltas[1],deltas[2],deltas[3],Cov[0],Cov[1],Cov[2],Cov[3],Cov[4],Cov[5],Cov[6],Cov[7],Cov[8],Cov[9]);
							fout.write(cadena_file);
						} catch (IOException e) {// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}


				}
				if (mensajetype.equals("IMU_Stream_Data")) {
                    // Poner TimeStamp de la App (según le llega el dato)
                    long timestamp_ns_raw = System.nanoTime(); // in nano seconds
                    if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
					{
						timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
					} else {
						timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
					}
                    timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

                    Log.i("MIMU22BT handler","Stream Dato recibido");
                    int packet_number=data.getInt("Packet");
                    int timestamp_sensor=data.getInt("Timestamp");
					//Float timestamp_sensor=data.getFloat("Timestamp");
                    float[] Gyro = new float[3];
                    float[] Acce = new float[3];
                    Gyro[0]=data.getFloat("GyroX");
                    Gyro[1]=data.getFloat("GyroY");
                    Gyro[2]=data.getFloat("GyroZ");
                    Acce[0]=data.getFloat("AcceX");
                    Acce[1]=data.getFloat("AcceY");
                    Acce[2]=data.getFloat("AcceZ");

                    // Freq:
                    contador_MIMU22BT++;
					if (timestamp - timestamp_MIMU22BT_last > 0.002) {
						freq_medida_MIMU22BT = (float) (0.995 * freq_medida_MIMU22BT + 0.005 / (timestamp - timestamp_MIMU22BT_last));
					}
                    timestamp_MIMU22BT_last=timestamp;

                    if (timestamp-timestamp_MIMU22BT_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
                    {
                        String cadena_display=String.format(Locale.US,"\tAx: \t%5.3f \t \tAy: \t%5.3f \t \tAz: \t%5.3f\n",Acce[0],Acce[1],Acce[2]) ;
                        cadena_display=cadena_display+String.format(Locale.US,"\tGx: \t%5.3f \t \tGy: \t%5.3f \t \tGz: \t%5.3f\n",Gyro[0],Gyro[1],Gyro[2]);
                        cadena_display=cadena_display+String.format(Locale.US,"\tPacket:%d \tTimestamp: \t%d\n",packet_number,timestamp_sensor);
                        obj_txtView23b.setText(cadena_display);
                        timestamp_MIMU22BT_last_update=timestamp;
                    }

                    // Grabar en fichero
                    if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
                    {
                        try {
                            String cadena_file=String.format(Locale.US,"\nIM1X;%.3f;%d;%d;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f",
                                    timestamp,timestamp_sensor,packet_number,Acce[0],Acce[1],Acce[2],Gyro[0],Gyro[1],Gyro[2]);
                            fout.write(cadena_file);
						} catch (IOException e) {// TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }


                }
			}
		};
	}


	//=======================poner_manejador_XSENS=======================0

	private void poner_manejador_XSens(){
		handlerXSens=new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Bundle data=msg.getData();
				String mensajetype=data.getString("MensajeType");
				String readername=data.getString("ReaderName");
				if (mensajetype.equals("Connect")) {
					Boolean connected=data.getBoolean("Connected");
					if (connected){
						USBConnected=true;
						//showToast("manejador_XSens: USB connected");
						// Header
						obj_txtView21.setText(" IMUX: XSens MTi Inertial Sensor detected");
						obj_txtView21.setBackgroundColor(0xFF00FF00);  // green color
						// Features
						obj_txtView21a.setText("\tManufacturer: Xsens\n\tModel: MTi IMU\n\tConfigured to transmit: Acc, Gyr, Mag, Euler, Temp\n"+
								      "\tRanges: Acc (5G), Gyr (300�/s), Mag (?uT)\n\tSamplig Frequency: 100Hz\n\tConnectivity: USB");
					} else {
						USBConnected=false;
						showToast("manejador_XSens: USB disconnected");
						// Header
						obj_txtView21.setText(" IMUX: No XSens MTi Inertial Sensor detected");
						obj_txtView21.setBackgroundColor(0xFFFF0000);  // red color
						// Features
						obj_txtView21a.setText("\tNo Features");
					}
				}

				if (mensajetype.equals("MTiConnect")) {
					Boolean connected=data.getBoolean("Connected");
					if (!connected) {
						if (!configMode) {
							showToast("MTi disconnected or in config mode");
						}
					}
				}

				if (mensajetype.equals("IMU_Data")) {
					// Poner TimeStamp de la App (seg�n le llega el dato)
					long timestamp_ns_raw = System.nanoTime(); // in nano seconds
					if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
					{
						timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
					} else {
						timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
					}
					timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

					//Log.i("Xsens","[Xsens handler] Dato recibido");
					float[] acceleration = new float[3];
					acceleration[0]=data.getFloat("Accelerations_x");
					acceleration[1]=data.getFloat("Accelerations_y");
					acceleration[2]=data.getFloat("Accelerations_z");
					float[] turnRate = new float[3];
					turnRate[0]=data.getFloat("TurnRates_x");
					turnRate[1]=data.getFloat("TurnRates_y");
					turnRate[2]=data.getFloat("TurnRates_z");
					float[] magneticField = new float[3];
					magneticField[0]=data.getFloat("MagneticFields_x");
					magneticField[1]=data.getFloat("MagneticFields_y");
					magneticField[2]=data.getFloat("MagneticFields_z");
					float[] quaternions = new float[4];
					quaternions[0]=data.getFloat("quaternions1");
					quaternions[1]=data.getFloat("quaternions2");
					quaternions[2]=data.getFloat("quaternions3");
					quaternions[3]=data.getFloat("quaternions4");
					float[] euler = new float[3];
					euler[0]=data.getFloat("Euler_Roll");
					euler[1]=data.getFloat("Euler_Pitch");
					euler[2]=data.getFloat("Euler_Yaw");
					float temperature=data.getFloat("Temperature");
					float pressure=data.getFloat("Pressure");
					short counter=data.getShort("Counter");
					float SensorTimestamp=(float)timestamp_Imux_last+0.01f;  // asumo muestreo cada 0.01s i.e. 100Hz //((float)counter/100);

					// Freq:
					contador_Imux++;
					if (SensorTimestamp - timestamp_Imux_last > 0.002) {
						freq_medida_Imux = (float) (0.99 * freq_medida_Imux + 0.01 / (SensorTimestamp - timestamp_Imux_last));
					}
					timestamp_Imux_last=SensorTimestamp;

					if (timestamp-timestamp_Imux_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
					{
						String cadena_display=String.format(Locale.US,"\tAcc(X): \t%10.5f \tm/s^2\n\tAcc(Y): \t%10.5f \tm/s^2\n\tAcc(Z): \t%10.5f \tm/s^2\n",acceleration[0],acceleration[1],acceleration[2]);
						cadena_display=cadena_display+String.format(Locale.US,"\tGyr(X): \t%10.5f \trad/s\n\tGyr(Y): \t%10.5f \trad/s\n\tGyr(Z): \t%10.5f \trad/s\n",turnRate[0],turnRate[1],turnRate[2]);
						cadena_display=cadena_display+String.format(Locale.US,"\tMag(X): \t%10.5f \tuT\n\tMag(Y): \t%10.5f \tuT\n\tMag(Z): \t%10.5f \tuT\n",magneticField[0],magneticField[1],magneticField[2]);
						cadena_display=cadena_display+String.format(Locale.US,"\tEuler(�): \t%5.1f \t%5.1f \t%5.1f\n",euler[0],euler[1],euler[2]);
						cadena_display=cadena_display+String.format(Locale.US,"\tQuater: \t%3.4f \t%3.4f \t%3.4f \t%3.4f\n",quaternions[0],quaternions[1],quaternions[2],quaternions[3]);
						cadena_display=cadena_display+String.format(Locale.US,"\tPressu: \t%10.2f \tmbar\n\tTemp: \t%10.1f \t�C\n\tTimeStamp: \t%10.1f \ts\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",pressure,temperature,SensorTimestamp,freq_medida_Imux);
						obj_txtView21b.setText(cadena_display);
						timestamp_Imux_last_update=timestamp;
					}

					// Grabar en fichero
					if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
					{
						try {
							String cadena_file=String.format(Locale.US,"\nIMUX;%.3f;%.3f;%d;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.5f;%.3f;%.2f",
									timestamp,SensorTimestamp,counter,acceleration[0],acceleration[1],acceleration[2],turnRate[0],turnRate[1],turnRate[2],
									magneticField[0],magneticField[1],magneticField[2],euler[0],euler[1],euler[2],quaternions[0],quaternions[1],quaternions[2],
									quaternions[3],pressure,temperature);
							fout.write(cadena_file);
						} catch (IOException e) {// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
			}
		};
	}


	//-----------------------manejador USB-------------------
	void poner_manejador_USB() {
		mUsbReceiver = new BroadcastReceiver() {

			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (ACTION_USB_PERMISSION.equals(action)) {
					synchronized (this) {
						UsbDevice localdevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

						if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
							if (localdevice != null) {
								if (!mXSens.connect(mUsbDevice)) {
									Log.e("USB Receiver","MTi not Responding");
									showToast("Permision: MTi not Responding");
								} else {
									mXSens.goToConfig();  // --- config-----
									byte[] Period=new byte[2];  // --- Freq. medida 100 Hz-----
									Period[0] = (byte) 0x04;
									Period[1] = (byte) 0x80;    // freq=115200/period: (0x0480->100 Hz)  (0x0280 -> 180 Hz)
									mXSens.Setting(mXSens.MID_SetPeriod, Period);
									byte[] OutputMode=new byte[2];  // --- Set Output Mode-----
									OutputMode[0] = (byte) 0x00;
									OutputMode[1] = (byte) 0x07;    // Set on: Temperature data, Calibrated Data and Orientation data (pag 20 manual xsens)
									mXSens.Setting(mXSens.MID_SetOutputMode, OutputMode);
									byte[] OutputSettings=new byte[4];  // --- Set Output Settings-----
									OutputSettings[0] = (byte) 0x00;
									OutputSettings[1] = (byte) 0x00;    // Set on: Sample Counter, Quaternion, AccGyrMag calibrated, Float output,... (pag 21 manual xsens)
									OutputSettings[2] = (byte) 0x00;
									OutputSettings[3] = (byte) 0x01;
									mXSens.Setting(mXSens.MID_SetOutputSettings, OutputSettings);
									mXSens.goToMeasurement();  // --- mesurement-----
									mXSens.startMeassurements(); // -------Comienzo la lectura de Mti-----
									Log.i("USB Receiver","MTi detected and Start Reading");
								//	showToast("Permision: MTi detected and Start Reading");
								}
							}
						} else {
							mUsbDevice=null;
							Log.e("USB Receiver","MTi detected, but permission not granted");
							showToast("Permision: MTi detected, but permission not granted");
						}
					}
				}

				if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (device != null) {
						if (mXSens != null) {
							mXSens.stopReading();  // Paro la lectura de Mti 
							mXSens.disconnect();  // Libero USB de Mti 
						}
						USBConnected=false;
						Log.e("USB Receiver","USB disconnected");
						mUsbDevice=null;
					}
				}
			}
		};
		}
		//------------end manejador USB--------------


	//======================onAccuracyChanged==================================0
	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	//======================onSensorChanged==================================
	@Override
	public final void onSensorChanged(SensorEvent event) {
		long accuracy = event.accuracy;

		// TimeStamp del Sensor (a poner en el log_file)
		long   SensorTimestamp_ns_raw =  event.timestamp;      // in nano seconds
		double SensorTimestamp = ((double)(SensorTimestamp_ns_raw))*1E-9;  // de nano_s a segundos

		// Poner TimeStamp de la App (seg�n le llega el dato)
		long timestamp_ns_raw = System.nanoTime(); // in nano seconds
		if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
		{
			timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
		} else {
			timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
		}
		timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

		//Log.i("","timestamp (ns): "+timestamp_ns);
		//Log.i("","timestamp (s): "+timestamp);

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			contador_Acce++;
			//double resto=Math.IEEEremainder(contador_Acce, 10);
			if (SensorTimestamp - timestamp_Acce_last > 0) {
				freq_medida_Acce = (float) (0.9 * freq_medida_Acce + 0.1 / (SensorTimestamp - timestamp_Acce_last));
			} else {
				Log.e("ACCE SENSOR","timestamp<timestamp_Acce_last");
			}
			timestamp_Acce_last=SensorTimestamp;

			// Many sensors return 3 values, one for each axis.
			float[] Acc_data = event.values;

			// Do something with this sensor value.
			if (timestamp-timestamp_Acce_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tAcc(X): \t%10.5f \tm/s^2\n\tAcc(Y): \t%10.5f \tm/s^2\n\tAcc(Z): \t%10.5f \tm/s^2\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Acc_data[0],Acc_data[1],Acc_data[2],freq_medida_Acce);
				obj_txtView1b.setText(cadena_display);
				timestamp_Acce_last_update=timestamp;
			}

			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nACCE;%.3f;%.3f;%.5f;%.5f;%.5f;%d",timestamp,SensorTimestamp,Acc_data[0],Acc_data[1],Acc_data[2],accuracy);
					fout.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			contador_Gyro++;

			// Many sensors return 3 values, one for each axis.
			float[] Gyr_data = event.values;

			// Do something with this sensor value.
			if (SensorTimestamp - timestamp_Gyro_last > 0) {
				freq_medida_Gyro = (float) (0.99 * freq_medida_Gyro + 0.01 / (SensorTimestamp - timestamp_Gyro_last));
			}
			timestamp_Gyro_last=SensorTimestamp;

			if (timestamp-timestamp_Gyro_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tGyr(X): \t%10.5f \trad/s\n\tGyr(Y): \t%10.5f \trad/s\n\tGyr(Z): \t%10.5f \trad/s\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Gyr_data[0],Gyr_data[1],Gyr_data[2],freq_medida_Gyro);
				obj_txtView2b.setText(cadena_display);
				timestamp_Gyro_last_update=timestamp;
			}

			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nGYRO;%.3f;%.3f;%.5f;%.5f;%.5f;%d",timestamp,SensorTimestamp,Gyr_data[0],Gyr_data[1],Gyr_data[2],accuracy);
					fout.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			contador_Magn++;
			if (SensorTimestamp - timestamp_Magn_last > 0) {
				freq_medida_Magn = (float) (0.9 * freq_medida_Magn + 0.1 / (SensorTimestamp - timestamp_Magn_last));
			}
			timestamp_Magn_last=SensorTimestamp;
			// Many sensors return 3 values, one for each axis.
			float[] Mag_data = event.values;
			// Do something with this sensor value.
			if (timestamp-timestamp_Magn_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tMag(X): \t%10.5f \tuT\n\tMag(Y): \t%10.5f \tuT\n\tMag(Z): \t%10.5f \tuT\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Mag_data[0],Mag_data[1],Mag_data[2],freq_medida_Magn);
				obj_txtView3b.setText(cadena_display);
				timestamp_Magn_last_update=timestamp;
			}
			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nMAGN;%.3f;%.3f;%.5f;%.5f;%.5f;%d",timestamp,SensorTimestamp,Mag_data[0],Mag_data[1],Mag_data[2],accuracy);
					fout.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			contador_Pres++;
			freq_medida_Pres=(float) (0.9*freq_medida_Pres	+ 0.1/(SensorTimestamp-timestamp_Pres_last));
			timestamp_Pres_last=SensorTimestamp;
			// Many sensors return 3 values, one for each axis.
			float[] Pre_data = event.values;
			// Do something with this sensor value.
			if (timestamp-timestamp_Pres_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tPresssure: \t%8.2f \tmbar\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Pre_data[0],freq_medida_Pres);
				obj_txtView4b.setText(cadena_display);
				timestamp_Pres_last_update=timestamp;
			}
			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nPRES;%.3f;%.3f;%.4f;%d",timestamp,SensorTimestamp,Pre_data[0],accuracy);
					fout.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
			contador_Ligh++;
			freq_medida_Ligh=(float) (0.9*freq_medida_Ligh	+ 0.1/(SensorTimestamp-timestamp_Ligh_last));
			timestamp_Ligh_last=SensorTimestamp;
			// Many sensors return 3 values, one for each axis.
			float[] Ligh_data = event.values;
			// Do something with this sensor value.
			if (timestamp-timestamp_Ligh_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tLight Intensity: \t%8.1f \tLux\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Ligh_data[0],freq_medida_Ligh);
				obj_txtView5b.setText(cadena_display);
				timestamp_Ligh_last_update=timestamp;
			}
			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nLIGH;%.3f;%.3f;%.1f;%d",timestamp,SensorTimestamp,Ligh_data[0],accuracy);
					fout.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
			contador_Prox++;
			freq_medida_Prox=(float) (0.9*freq_medida_Prox	+ 0.1/(SensorTimestamp-timestamp_Prox_last));
			timestamp_Prox_last=SensorTimestamp;
			// Many sensors return 3 values, one for each axis.
			float[] Prox_data = event.values;
			// Do something with this sensor value.
			if (timestamp-timestamp_Prox_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tProximity: \t%8.1f \tUnits\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Prox_data[0],freq_medida_Prox);
				obj_txtView6b.setText(cadena_display);
				timestamp_Prox_last_update=timestamp;
			}
			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nPROX;%.3f;%.3f;%.1f;%d",timestamp,SensorTimestamp,Prox_data[0],accuracy);
					fout.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
			contador_Humi++;
			freq_medida_Humi=(float) (0.9*freq_medida_Humi	+ 0.1/(SensorTimestamp-timestamp_Humi_last));
			timestamp_Humi_last=SensorTimestamp;
			// Many sensors return 3 values, one for each axis.
			float[] Humi_data = event.values;
			// Do something with this sensor value.
			if (timestamp-timestamp_Humi_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tRelative Humidity: \t%8.1f \t%%\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Humi_data[0],freq_medida_Humi);  // para poner "%" en string => poner %%
				obj_txtView7b.setText(cadena_display);
				timestamp_Humi_last_update=timestamp;
			}
			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nHUMI;%.3f;%.3f;%.1f;%d",timestamp,SensorTimestamp,Humi_data[0],accuracy);
					fout.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
			contador_Temp++;
			freq_medida_Temp=(float) (0.9*freq_medida_Temp	+ 0.1/(SensorTimestamp-timestamp_Temp_last));
			timestamp_Temp_last=SensorTimestamp;
			// Many sensors return 3 values, one for each axis.
			float[] Temp_data = event.values;
			// Do something with this sensor value.
			if (timestamp-timestamp_Temp_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tAmbient Temperature: \t%8.1f \t�C\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Temp_data[0],freq_medida_Temp);
				obj_txtView8b.setText(cadena_display);
				timestamp_Temp_last_update=timestamp;
			}
			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nTEMP;%.3f;%.3f;%.1f;%d",timestamp,SensorTimestamp,Temp_data[0],accuracy);
					fout.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			contador_Ahrs++;
			freq_medida_Ahrs=(float) (0.9*freq_medida_Ahrs	+ 0.1/(SensorTimestamp-timestamp_Ahrs_last));
			timestamp_Ahrs_last=SensorTimestamp;
			// Many sensors return 3 values, one for each axis.
			float[] AHRS_data = event.values;
			/*values[0]: x*sin(θ/2)
			values[1]: y*sin(θ/2)
			values[2]: z*sin(θ/2)
			values[3]: cos(θ/2)
			values[4]: estimated heading Accuracy (in radians) (-1 if unavailable)
			values[3], originally optional, will always be present from SDK Level 18 onwards. values[4] is a new value that has been added in SDK Level 18. */

			// Cambiar formato de "Rotation vector" a "Azimuth,Pitch,Roll"
			float[] Rot_b_g={1, 0, 0, 0, 1, 0, 0, 0, 1};

			//............................
			// Aplico remedio para evitar el error:
			//     "java.lang.IllegalArgumentException: R array length must be 3 or 4"
			//     "at android.hardware.SensorManager.getRotationMatrixFromVector(SensorManager.java:1336)"
			// leido en: https://groups.google.com/forum/#!topic/android-developers/U3N9eL5BcJk

			// En vez de esto:
			//      SensorManager.getRotationMatrixFromVector(Rot_b_g, AHRS_data);  // Obtengo "Matriz rotacion" (en ENU)
			//pongo esto:
			try {
				SensorManager.getRotationMatrixFromVector(Rot_b_g, AHRS_data);
			} catch (IllegalArgumentException e) {
				if (AHRS_data.length > 3) {
					// Note 3 bug
					float[] newVector = new float[] {
							AHRS_data[0],
							AHRS_data[1],
							AHRS_data[2]
					};
					SensorManager.getRotationMatrixFromVector(Rot_b_g, newVector);
				}
			}
			//............................

			float[] orientacion={0,0,0};  // AzimutZ[0], PitchX[1] and RollY[2]
			orientacion=SensorManager.getOrientation(Rot_b_g, orientacion);  // En radianes pero lo da en WND (West/North/Down)
			orientacion[0] = -orientacion[0];
			orientacion[1] = -orientacion[1]; // Paso de WND a de nuevo ENU (que es donde quiero trabajar)
			double PI=3.14159265358979323846;
			float yaw_Z=(float) (orientacion[0]*180/PI);  // Yaw, ejeZ en grados
			float pitch_X=(float) (orientacion[1]*180/PI);  // Pitch, eje X
			float roll_Y=(float) (orientacion[2]*180/PI);  // Roll, eje Y

			//DecimalFormat
			if (timestamp-timestamp_Ahrs_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tPitch(X): \t%10.3f \t\tdegrees\n\tRoll(Y): \t%10.3f \t\tdegrees\n\tYaw(Z): \t%10.3f \tdegrees\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",pitch_X,roll_Y,yaw_Z,freq_medida_Ahrs);
				obj_txtView9b.setText(cadena_display);
				timestamp_Ahrs_last_update=timestamp;
			}

			// Do something with this sensor value.

			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					//String cadena_file=String.format(Locale.US,"\nAHRS;%.3f;%.3f;%.4f;%.4f;%.4f;%.6f;%.6f;%.6f;%.6f;%d;%.9f;%.9f;%.9f;%.9f;%.9f;%.9f;%.9f;%.9f;%.9f",
					//		timestamp,SensorTimestamp,pitch_X,roll_Y,yaw_Z,AHRS_data[0],AHRS_data[1],AHRS_data[2],AHRS_data[3],accuracy,Rot_b_g[0],Rot_b_g[1],Rot_b_g[2],Rot_b_g[3],Rot_b_g[4],Rot_b_g[5],Rot_b_g[6],Rot_b_g[7],Rot_b_g[8]);

					String cadena_file=String.format(Locale.US,"\nAHRS;%.3f;%.3f;%.6f;%.6f;%.6f;%.8f;%.8f;%.8f;%d",
							timestamp,SensorTimestamp,pitch_X,roll_Y,yaw_Z,AHRS_data[0],AHRS_data[1],AHRS_data[2],accuracy);
					fout.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if


	}


	//======================onLocationChanged==================================
	// Called when location has changed
	public void onLocationChanged(Location location) { //
		Log.i("LocationChanged","Gps data");
		double latitude=location.getLatitude();
		double longitude=location.getLongitude();
		double altitude=location.getAltitude();
		float bearing=location.getBearing();
		float accuracy=location.getAccuracy();
		float speed=location.getSpeed();
		double SensorTimeStamp=(double)(location.getTime())/1000;   // lo pongo en segundos (desde 1 Enero 1970)
		String provider=location.getProvider();

		contador_Gnss++;

		// Poner TimeStamp de la App (seg�n le llega el dato)
		long timestamp_ns_raw = System.nanoTime(); // in nano seconds
		if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
		{
			timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
		} else {
			timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
		}
		timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

		if (timestamp - timestamp_Gnss_last > 0) {
			freq_medida_Gnss = (float) (0.9 * freq_medida_Gnss + 0.1 / (timestamp - timestamp_Gnss_last));
		}
		timestamp_Gnss_last=timestamp;
		/*	String text = String.format(
				"Lat:\t %f\nLong:\t %f\nAlt:\t %f\nBearing:\t %f\nAccuracy:\t %f\nSpeed:\t %f\nTime:\t %f", 
				location.getLatitude(), location.getLongitude(), location.getAltitude(),
				location.getBearing(),location.getAccuracy(),location.getSpeed(),location.getTime()); // Leer la posicion */

		if (timestamp-timestamp_Gnss_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
		{
			String cadena_display=String.format(Locale.US,"\tLatitude: \t%10.6f \tdegrees\n\tLongitude: \t%10.6f \tdegrees\n",latitude,longitude);
			String cadena_display_aux="";
			if (location.hasAltitude()) {
				cadena_display_aux = String.format(Locale.US, "\tAltitude: \t%6.1f \t m\n", altitude);
			} else {
				cadena_display_aux = "\tAltitude: \t\t? \tm\n";
			}
			cadena_display=cadena_display+cadena_display_aux;

			if (location.hasAccuracy()) {
				cadena_display_aux = String.format(Locale.US, "\tAccuracy: \t%8.3f \tm\n", accuracy);
			} else {
				cadena_display_aux = String.format(Locale.US, "\tAccuracy: \t\t? \tm\n", accuracy);
			}
			cadena_display=cadena_display+cadena_display_aux;

			if (location.hasBearing()) {
				cadena_display_aux = String.format(Locale.US, "\tBearing: \t\t%8.3f \tdegrees\n", bearing);
			} else {
				cadena_display_aux = String.format(Locale.US, "\tBearing: \t\t? \tdegrees\n", bearing);
			}
			cadena_display=cadena_display+cadena_display_aux;

			if (location.hasSpeed()) {
				cadena_display_aux = String.format(Locale.US, "\tSpeed: \t%8.3f \tm\n", speed);
			} else {
				cadena_display_aux = String.format(Locale.US, "\tSpeed: \t\t? \tm\n", speed);
			}
			cadena_display=cadena_display+cadena_display_aux;

			cadena_display = cadena_display + String.format(Locale.US, "\tTime: \t%8.3f \ts\n", SensorTimeStamp);
			;

			cadena_display=cadena_display+String.format(Locale.US,"\t(Provider: \t%s;  Freq: %5.0f Hz)\n",provider.toUpperCase(),freq_medida_Gnss);

			// Do something with this location value.
			TextView obj_txtView = (TextView)findViewById(R.id.textView10b);
			obj_txtView.setText(cadena_display);
			timestamp_Gnss_last_update=timestamp;
		}
		if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
		{
			try {
				String cadena_file=String.format(Locale.US,"\nGNSS;%.3f;%.3f;%.6f;%.6f;%.3f;%.3f;%.1f;%.1f;%d;%d",timestamp,SensorTimeStamp,latitude,longitude,altitude,bearing,accuracy,speed,num_satellites_in_view,num_satellites_in_use);
				fout.write(cadena_file);
			} catch (IOException e) {// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	//======================onGpsStatusChanged==================================
	@SuppressLint("MissingPermission") // jtorres added to fix compilation problem
	                                   // TODO: check permission as shown below
	public void onGpsStatusChanged(int event) {
	/*	if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
	*/
		gpsstatus=locationManager.getGpsStatus(gpsstatus);

		if(gpsstatus != null) {
			Iterable<GpsSatellite>satellites = gpsstatus.getSatellites();
			Iterator<GpsSatellite>sat = satellites.iterator();
			int num_inview=0;
			int num_inuse=0;
			String strGpsStats="";
			while (sat.hasNext()) {
				GpsSatellite satellite = sat.next();
				strGpsStats=strGpsStats+ "\n\t- PRN:" + satellite.getPrn() + ", Used:" + satellite.usedInFix() + ", SNR:" +
				satellite.getSnr() + ", Az:" + satellite.getAzimuth() + "º,\n\t   Elev: " + satellite.getElevation()+
				"º, Alma: "+satellite.hasAlmanac()+ ", Ephem: "+satellite.hasEphemeris();
				num_inview++;
				if (satellite.usedInFix())
				{num_inuse=num_inuse+1;}
			}
			String texto="\tSatellites in View: "+num_inview+", Satellites in Use: "+num_inuse+strGpsStats;
			TextView obj_txtView = (TextView)findViewById(R.id.textView10c);
			obj_txtView.setText(texto);

			num_satellites_in_view=num_inview;
			num_satellites_in_use=num_inuse;
		}
	}



	// =================Methods required by LocationListener=================
	public void onProviderDisabled(String provider) {
		TextView obj_txtView = (TextView)findViewById(R.id.textView10b);
		obj_txtView.setText("GNSS Provider Disabled");
	}
	public void onProviderEnabled(String provider) {
		TextView obj_txtView = (TextView)findViewById(R.id.textView10b);
		obj_txtView.setText("GNSS Provider Enabled");
	}
	public void onStatusChanged(String provider, int status, Bundle extras) {
		TextView obj_txtView = (TextView)findViewById(R.id.textView10b);
		obj_txtView.setText("GNSS Provider Sattus: "+ status);
	}


	//======================onCreateOptionsMenue==================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Log.i("Menu","Opcion 1 Menu_Settings pulsada!");
			startActivity(new Intent(MainActivity.this,PantallaPreferencias.class));
			return true;
		case R.id.menu_exit:
			Log.i("Menu","Opcion 2 Exit pulsada!");
			//MainActivity.this.onPause();
			//MainActivity.this.onStop();
			//MainActivity.this.onDestroy();

			MainActivity.this.finish();
			MainActivity.this.onDestroy();
			return true;
		case R.id.menu_about:
			Log.i("Menu","Opcion 2 About pulsada!");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	//======================onSaveInstanceState==================================
	// Called to save UI state changes at the
	// end of the active lifecycle.
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate and
		// onRestoreInstanceState if the process is
		// killed and restarted by the run time.
		super.onSaveInstanceState(savedInstanceState);
	}

	//======================onRestoreInstanceState==================================
	// Called after onCreate has finished, use to restore UI state
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.
		// Will only be called if the Activity has been
		// killed by the system since it was last visible.
	}

	//----------showToast----------------------
	private void showToast(String msg)
	{
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}

	//======================onResume==================================
	@Override
	protected void onResume() {
		super.onResume();
		int delay;

		SystemRequirementsChecker.checkWithDefaultDialogs(this);

		if (flag_BLE==true) {
			beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
				@Override
				public void onServiceReady() {
					long scanPeriodMillis=200;  // 5Hz  (maximum scan rate according to SDK and experimental; with 10 Hz or 100ms the result is the same as 5Hz)
					beaconManager.setForegroundScanPeriod(scanPeriodMillis,0);
					beaconManager.startRanging(region);                         // Scan for iBeacon BLE tags
					scanId_Eddystone = beaconManager.startEddystoneScanning();  // Scan for EddyStone BLE tags
					if (flag_EstimoteTelemetry){
					scanId = beaconManager.startTelemetryDiscovery();}
				}
			});



		}




		Log.i("OnResume","Estoy en OnResume");

		SharedPreferences pref =PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		switch (Integer.parseInt(pref.getString("opcion1", "2")))
		{
		case 1:
			delay=SensorManager.SENSOR_DELAY_FASTEST;
			Log.i("OnResume", "Opcion 1: SENSOR_DELAY_FASTEST");
			break;
		case 2:
			delay=SensorManager.SENSOR_DELAY_GAME;
			Log.i("OnResume", "Opcion 1: SENSOR_DELAY_GAME");
			break;
		case 3:
			delay=SensorManager.SENSOR_DELAY_NORMAL;
			Log.i("OnResume", "Opcion 1: SENSOR_DELAY_NORMAL");
			break;
		case 4:
			delay=SensorManager.SENSOR_DELAY_UI;
			Log.i("OnResume", "Opcion 1: SENSOR_DELAY_UI");
			break;
		default:
			delay=SensorManager.SENSOR_DELAY_GAME;
		}
		// Ver si quiero fijar la frecuencia a mi gusto en Hz
		Boolean agusto=pref.getBoolean("opcion5", false);
		if ( agusto )
		{
			try
			{
				double freq=Integer.parseInt(pref.getString("opcion2", "100"));
				delay=(int) (1/freq*1000000);
			} catch (Exception x)
			{ Log.i("Preference","Update rate in Hz not parsed");}
		}


		//.....register sensors............
		if (Sensor_Acc!=null)
		{
			mSensorManager.registerListener(this, Sensor_Acc, delay);
		}
		if (Sensor_Gyr!=null)
		{
			mSensorManager.registerListener(this, Sensor_Gyr, delay);
		}
		if (Sensor_Mag!=null)
		{
			mSensorManager.registerListener(this, Sensor_Mag, delay);
		}
		if (Sensor_Pre!=null)
		{
			mSensorManager.registerListener(this, Sensor_Pre, delay);
		}
		if (Sensor_Ligh!=null)
		{
			mSensorManager.registerListener(this, Sensor_Ligh, delay);
		}
		if (Sensor_Prox!=null)
		{
			mSensorManager.registerListener(this, Sensor_Prox, delay);
		}
		if (Sensor_Humi!=null)
		{
			mSensorManager.registerListener(this, Sensor_Humi, delay);
		}
		if (Sensor_Temp!=null)
		{
			mSensorManager.registerListener(this, Sensor_Temp, delay);
		}
		if (Sensor_AHRS!=null)
		{
			mSensorManager.registerListener(this, Sensor_AHRS, delay);
		}
		Log.i("OnResume","mSensorManager registered again");

		// ........Register Location manager...........
		if (locationManager!=null)
		{
			if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },	MY_PERMISSION_ACCESS_COARSE_LOCATION );
				ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_FINE_LOCATION  },	MY_PERMISSION_ACCESS_FINE_LOCATION );
				finish();
			}
			locationManager.addGpsStatusListener(this);
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			{
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200,0, this);
				Log.i("OnResume","GPS provider requested");
			}
			if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			{
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 200,0, this);
				Log.i("OnResume","NETWORK provider requested");
			}
			if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
			{
				locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 200,0, this);
				Log.i("OnResume","PASSIVE provider requested");
			}
		}
		Log.i("OnResume","locationManager registered again");

		//............. Register WiFi .................
		if (wifimanager!=null)
		{
			registerReceiver(wifibroadcastreceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)  );
		}
		Log.i("OnResume","timerWifi Created");

		// .............Register Bluetooth...............
		if (flag_BLE==false) {
			if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && bluetooth != null) {
				registerReceiver(bluetoothDiscoveryMonitor, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
				registerReceiver(bluetoothdiscoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
			}
			Log.i("OnResume", "timerBlue Created");
		}

		//............Register Audio..................
		if (mMicrophone!=null) {
			mMicrophone.start_read_audio_Thread();
		}
		// .............Register RFID Reader.....................
		if (flag_BLE==false) {
			for (int i = 0; i < mRFIDM220ReaderList.size(); i++) {
				RFIDM220Reader mRFIDM220Reader = mRFIDM220ReaderList.get(i);
				mRFIDM220Reader.startreading();
			}
		}
		// .............Register LPMS IMU.....................
		if (flag_BLE==false) {
			if (mLpmsB != null) {
			//	if (mLpmsB.isConnected & mLpmsB.thread_read.isAlive() == false) {
					mLpmsB.startreading();
			//	}
			}
		}
		// .............Register MIMU22BT IMU.....................
		if (flag_BLE==false) {
			if (mMIMU22BT != null) {
			//	if (mMIMU22BT.socket_connected & mMIMU22BT.ConnectThread.isAlive() == false) {
					mMIMU22BT.startreading();
			//	}
				}
		}
		//............Start reading XSens.............
		if(mUsbDevice!=null  && mXSens!=null)
		{
		//mXSens.startMeassurements();    // No llamo a "startmeasurement" para que no empiece antes de tener los permisos USB. 
		                                 // La llamada a "startmerasurement" se la dejo totalmente al "broadcastreceiver" del USB
		}
	
	}

	//======================onStart==================================
	@Override
	protected void onStart() {
		super.onStart();
		Log.i("OnStart","Estoy en OnStart");
	}

	//======================onReStart==================================
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i("OnRestart","Estoy en OnRestart");
	}

	//======================onPause==================================
	@Override
	protected void onPause() {
		super.onPause();

		Log.i("OnPause","INI: OnPause");

		if (flag_BLE==true) {
			beaconManager.stopRanging(region);                      // Stop iBeacon scanning
			beaconManager.stopEddystoneScanning(scanId_Eddystone);  // Stop Eddystone scanning
			if (flag_EstimoteTelemetry){
			beaconManager.stopTelemetryDiscovery(scanId);}
		}

		//......unregister Sensors.................
		mSensorManager.unregisterListener(this);
		locationManager.removeUpdates(this);

		//......unregister Wifi....................
		if (wifimanager!=null)
		{
			unregisterReceiver(wifibroadcastreceiver);
		}

		//......unregister Bluetooth.................
		if (flag_BLE==false) {
			if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) && bluetooth != null) {
				unregisterReceiver(bluetoothDiscoveryMonitor);
				unregisterReceiver(bluetoothdiscoveryResult);
			}
		}

		//......unregister RFID Readers....................
		if (flag_BLE==false) {
			for (int i = 0; i < mRFIDM220ReaderList.size(); i++) {
				RFIDM220Reader mRFIDM220Reader = mRFIDM220ReaderList.get(i);
				mRFIDM220Reader.stopreading();
			}
		}

		//.........unregister LPMS IMU............
		if (flag_BLE==false) {
			if (mLpmsB != null) {
				mLpmsB.stopreading();
				//mLpmsB.disconnect();
			}
		}

		//.........unregister MIMU22BT IMU............
		if (flag_BLE==false) {
			if (mMIMU22BT != null) {
				mMIMU22BT.stopreading();
				//mMIMU22BT.disconnect();
			}
		}
		//.........unregister XSens..............
		//unregisterReceiver(mUsbReceiver);
		if(mUsbDevice!=null  && mXSens!=null)
		{
		//mXSens.stopReading();  // No lo paro ya que en OnResume ya no llamo a StartMeasurement() por los motivos alli explicados. 
		}
		

		//......unregister Microphone ...............
		if (mMicrophone!=null) {
			mMicrophone.stop_audio_thread();
		}
		Log.i("OnPause","END: OnPause");
	}

	//======================onStop==================================
	@Override
	protected void onStop() {
		super.onStop();
		Log.i("OnStop","Estoy en OnStop");
	}

	//======================onDestroy==================================
	@Override
	protected void onDestroy() {
		super.onDestroy();

		Log.i("OnDestroy","INI: OnDestroy");
		if (flag_BLE==false) {
			//........disconnect from RFID Reader..........
			for (int i = 0; i < mRFIDM220ReaderList.size(); i++) {
				RFIDM220Reader mRFIDM220Reader = mRFIDM220ReaderList.get(i);
				mRFIDM220Reader.disconnect();
			}
			//...........disconnect from LPMS...............
			mLpmsB.disconnect();
			//...........disconnect from MIMU22BT...............
			mMIMU22BT.disconnect();

		}
		
		//.........disconnect XSens.........
		mXSens.stopReading();  // Paro la lectura de Mti
		unregisterReceiver(mUsbReceiver);
		mXSens.disconnect();  // Libero USB de Mti 
		
		//----quitar Timers------------
		if (flag_BLE==false) {
			timerBlue.cancel();
		}
		timerWifi.cancel();
		if (timerReloj!=null)
		{ timerReloj.cancel();}
		
		
		//----------liberar microphono-----
		if (mMicrophone!=null) {
			mMicrophone.release();
		}
		// ----apagar el bluetooth si yo fui el que lo encendio----------
		if (esta_App_encendio_bluetooth)
		{
			if (bluetooth.isEnabled()==true)
			{
				bluetooth.disable();
			}
		}

		if (flag_Trace)
		{	Debug.stopMethodTracing();	}
		Log.i("OnDestroy","END: OnDestroy");
	}



}  // end - MainActivity
