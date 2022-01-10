// *******************************************************************
//
//	File Name:		MainActivity.java
//
//	Description:	Activity that handles the selection of
//                  the BLE device
//
//	 Date		:  Ver	:	Aut :	Comment
//	------------+-------+-------+---------------------
//	 20.03.19	:  0.1	:	TF	:	creation
//   18.06.19   :  1.0  :   TF  :   finished creation
//
//	        Written for MCI - Infineon EAL
//	       Copyright © 2019 Thomas Fleischmann
//	               All rights reserved
//
// ******************************************************************

package edu.mci.example.BLE_CO2_Android_SensorNetwork;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.util.Log;
import android.content.res.Configuration;

import java.util.ArrayList;
import java.lang.String;

import edu.mci.example.BLE_CO2_Android_SensorNetwork.R;

public class MainActivity extends AppCompatActivity {
    private Handler handler = new Handler();
    private static final long INTERVAL = 5000;
    private static final int REQUEST_ENABLE_BT = 1;

    private Button searchBtn;
    private ListView lstvw;
    private ArrayList list = new ArrayList();

    private BluetoothManager bluetoothManager; // = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    private BluetoothAdapter bluetoothAdapter; // = bluetoothManager.getAdapter();
    private BluetoothLeScanner bluetoothLeScannerLeScanner; // = bluetoothAdapter.getBluetoothLeScanner();

    private ScanCallback leScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            final ArrayAdapter aAdapter;
            aAdapter = new ArrayAdapter(getApplicationContext(), R.layout.row, list);

            BluetoothDevice device = result.getDevice();
            if (!(list.contains(device.getName() + "\n" + device.getAddress()))) {
                if (!(device.getName() == null)) {
                    list.add(device.getName() + "\n" + device.getAddress());
                    Log.d("BT", device.getName() + " " + device.getAddress());
                }
            }

            aAdapter.notifyDataSetChanged();
            lstvw = findViewById(R.id.deviceList);
            lstvw.setAdapter(aAdapter);
        }
    };

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        }
    };

    //	this function is called when the app / activity is started.
    //  It will activate BT and start the scan for devices.
    //	input:  savedInstanceState (handles by the Android System)
    //
    //	output: -
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchBtn = findViewById(R.id.btnGet);

        if (!hasRequiredPermissions()) {            // Check BT & GPS Permissions and request if necessary
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, PackageManager.PERMISSION_GRANTED);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, PackageManager.PERMISSION_GRANTED);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PackageManager.PERMISSION_GRANTED);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
        }

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScannerLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (bluetoothAdapter == null) {     //check if device has BT Adapter
            Toast.makeText(getApplicationContext(), "Bluetooth not Supported", Toast.LENGTH_SHORT).show();
            return;
        } else if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {           //check if the BT Adapter is enabled
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);   // if bluetooth is close, than open it**
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        resetFoundDevices();
        searchBtn.setOnClickListener(new View.OnClickListener()
        {
            //called when the Button is clicked
            @Override
            public void onClick(View v)
            {
                resetFoundDevices();
                searchBtn.setEnabled(false);
                if (bluetoothAdapter == null) {     //check if device has BT Adapter
                    Toast.makeText(getApplicationContext(), "Bluetooth not Supported", Toast.LENGTH_SHORT).show();
                    return;
                } else if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {           //check if the BT Adapter is enabled
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);   // if bluetooth is close, than open it**
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }

                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothLeScannerLeScanner.startScan(leScanCallback);
                    }
                });

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        searchBtn.setEnabled(true);
                        AsyncTask.execute(new Runnable() {
                            @Override
                            public void run() {
                                bluetoothLeScannerLeScanner.stopScan(leScanCallback);
                            }
                        });
                    }
                }, INTERVAL);
            }
        });

        searchBtn.performClick();
        lstvw = findViewById(R.id.deviceList);
        lstvw.setOnItemClickListener(new AdapterView.OnItemClickListener()      // Called when an Item in the ListView is clicked
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (bluetoothAdapter == null) {     //check if device has BT Adapter
                    Toast.makeText(getApplicationContext(), "Bluetooth not Supported", Toast.LENGTH_SHORT).show();
                    return;
                } else if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {           //check if the BT Adapter is enabled
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);   // if bluetooth is close, than open it**
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    String device = lstvw.getAdapter().getItem(position).toString();
                    Log.i("device", device);
                    String devicename = device.split("\n")[0];
                    String macaddr = device.split("\n")[1];
                    Log.i("MAC", macaddr);

                    // the new Activity is started. Get the macaddr and devicename of the selected device.
                    Intent DiagramIntent = new Intent(MainActivity.this, diagram.class);
                    DiagramIntent.putExtra("devicename", devicename);
                    DiagramIntent.putExtra("macaddr", macaddr);
                    startActivity(DiagramIntent);
                }
            }
        });
    }

    //	this function is called whenever the device is rotated.
    //	input:  newConfig (handled by Android System)
    //
    //	output: rotation of the screen
    //          rescan of the BT devices
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);

        resetFoundDevices();
        searchBtn.performClick();
    }

    //	this function is called whenever the activity is restarted.
    //	input:  -
    //
    //	output: rescan of the BT devices
    @Override
    public void onResume() {
        super.onResume();

        resetFoundDevices();
        searchBtn.performClick();
    }

    //	this function is called whenever the activity is destroyed (e.g the App is closed).
    //	input:  -
    //
    //	output: closed App
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //	this function is called when the MCI logo is clicked
    //	input:  -
    //
    //	output: opens the MCI website in a Web Browser
    public void onMCI(View view) {
        String url = getString(R.string.urlMCI);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        try
        {
            startActivity(i);
            Log.d("Web", url);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //	this function is called when the Infineon logo is clicked
    //	input:  -
    //
    //	output: opens the Infineon website in a Web Browser
    public void onInfineon(View view) {
        String url = getString(R.string.urlInfineon);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        try {
            startActivity(i);
            Log.d("Web", url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetFoundDevices() {
        final ArrayAdapter aAdapter;
        aAdapter = new ArrayAdapter(getApplicationContext(), R.layout.row, list);

        list.clear();
        aAdapter.notifyDataSetChanged();

        lstvw = findViewById(R.id.deviceList);
        lstvw.setAdapter(aAdapter);
    }

    private boolean hasRequiredPermissions() {
        boolean hasBluetoothPermission = hasPermission(Manifest.permission.BLUETOOTH);
        boolean hasBluetoothAdminPermission = hasPermission(Manifest.permission.BLUETOOTH_ADMIN);
        boolean hasCoarseLocationPermission = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        boolean hasFineLocationPermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        return hasBluetoothPermission && hasBluetoothAdminPermission && hasCoarseLocationPermission && hasFineLocationPermission;
    }

    private boolean hasPermission(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
