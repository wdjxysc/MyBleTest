package com.example.mybletest;

import java.util.List;
import java.util.UUID;

import android.R.string;
import android.support.v7.app.ActionBarActivity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.annotation.TargetApi;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends ActionBarActivity {

	private final static String TAG = MainActivity.class.getSimpleName();  
	
	public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    
    
    public static final UUID SERVIE_UUID = UUID
			.fromString("0000FFF0-0000-1000-8000-00805f9b34fb");
	public static final UUID READ_UUID = UUID
			.fromString("0000FFF4-0000-1000-8000-00805f9b34fb");
	public static final UUID WRITE_UUID = UUID
			.fromString("0000FFF3-0000-1000-8000-00805f9b34fb");
	public static final UUID SET_DEVICE_UUID = UUID
			.fromString("0000FFF1-0000-1000-8000-00805f9b34fb");
    /**搜索BLE终端*/  
    private BluetoothAdapter mBluetoothAdapter;  
    private BluetoothGatt mBluetoothGatt;
    /**读写BLE终端*/  
//    private BluetoothLeClass mBLE;  
    private boolean mScanning;  
    private Handler mHandler; 
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    
    private void  scanLeDevice(final boolean enable) {
    	if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            
           UUID deviceUUID[] = {SERVIE_UUID};

            mScanning = true;
            mBluetoothAdapter.startLeScan(deviceUUID,mLeScanCallback);
            Log.i("状态", "开始扫描");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
	}
    
 // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
    	
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        	connectDevice(device);
            runOnUiThread(new Runnable() {
               @Override
               public void run() {

               }
           });
       }
    };
    
    private void connectDevice (BluetoothDevice device ) {
    	mBluetoothGatt = device.connectGatt(this, false, mBluetoothGattCallback);
		
	}
    
    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

    	//连接状态改变
    	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) 
    	{
    		if(status == BluetoothProfile.STATE_CONNECTED)
    		{
    			//连接成功
    			gatt.discoverServices();
    		}
    		else if (status == BluetoothProfile.STATE_CONNECTING) {
				//正在连接
			}
    		else if (status == BluetoothProfile.STATE_DISCONNECTED) {
				//断开连接
			}
    	};
    	
    	//特征值发生改变  notify
    	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) 
    	{
    		broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
    	};
    	//发现服务
    	public void onServicesDiscovered(BluetoothGatt gatt, int status) 
    	{
    		if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                BluetoothGattService mBluetoothGattService = new BluetoothGattService(SERVIE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                List<BluetoothGattCharacteristic> characteristics = mBluetoothGattService.getCharacteristics();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
    	};
    	
    	@Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
    	
	};
	
	private void broadcastUpdate(final String action) {
	    final Intent intent = new Intent(action);
	    sendBroadcast(intent);
	}
	private void broadcastUpdate(final String action,final BluetoothGattCharacteristic characteristic) {
			final Intent intent = new Intent(action);

			// This is special handling for the Heart Rate Measurement profile. Data
			// parsing is carried out as per profile specifications.
			if (READ_UUID.equals(characteristic.getUuid())) {
				int flag = characteristic.getProperties();
				byte data[] = characteristic.getValue();
				int format = -1;
				if ((flag & 0x01) != 0) {
				format = BluetoothGattCharacteristic.FORMAT_UINT16;
				Log.d(TAG, "Heart rate format UINT16.");
			} else {
				format = BluetoothGattCharacteristic.FORMAT_UINT8;
				Log.d(TAG, "Heart rate format UINT8.");
			}
			final int heartRate = characteristic.getIntValue(format, 1);
			Log.d(TAG, String.format("Received heart rate: %d", heartRate));
//			intent.putExtra(WRITE_UUID, String.valueOf(heartRate));
		} else {
			// For all other profiles, writes the data formatted in HEX.
			final byte[] data = characteristic.getValue();
			if (data != null && data.length > 0) {
				final StringBuilder stringBuilder = new StringBuilder(
						data.length);
				for (byte byteChar : data)
					stringBuilder.append(String.format("%02X ", byteChar));
//				intent.putExtra(SET_DEVICE_UUID, new String(data) + "\n" + stringBuilder.toString());
			}
		}
		sendBroadcast(intent);
	}
	
	public void close() {
	    if (mBluetoothGatt == null) {
	        return;
	    }
	    mBluetoothGatt.close();
	    mBluetoothGatt = null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mHandler = new Handler();

		// Initializes Bluetooth adapter.
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		mBluetoothAdapter.enable();

		scanLeDevice(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
