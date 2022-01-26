// *******************************************************************
//
//	File Name:		diagram.java
//
//	Description:	Activity that handles BLE and graphics
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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.Viewport;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.widget.RelativeLayout.ALIGN_LEFT;
import static android.widget.RelativeLayout.BELOW;
import static android.widget.RelativeLayout.END_OF;

public class diagram extends AppCompatActivity //implements OnItemSelectedListener
{
    private SwipeRefreshLayout swipeLayout;
    private String macaddr = "";
    private TextView Statusview;
    private GraphView graph;
    private String BTstatus = "";
    private RelativeLayout RelativeLayoutInScrollView;
    private final int basicIdRB = 1000;
    private final int basicIdTV = 5000;
    private int activeMode = 0;
    private String dataForCSV = "Timestamp,DataId,Value\n";
    private String filename = "";
    private boolean isSaveing = false;
    private File file;

    private String TAG = "LOG";
    private List<String> SensorNames;
    private List<String> SensorUnits;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private static BluetoothGatt bluetoothGatt = null;

    public static BluetoothGatt getBT(){
        return bluetoothGatt;
    }

    private LineGraphSeries<DataPoint> GraphSeries;
    private int DataSetsCount = 0;

    private int Setting_Buffer_Calibration = 0;
    private int Setting_Buffer_SampleRate = 0;
    private int Setting_Buffer_AlarmThreshold = 0;
    private int Setting_Buffer_SolderingCompensation = 0;

    List<Double> Data_Buffer_CO2 = new ArrayList<Double>();
    List<Double> Data_Buffer_Press = new ArrayList<Double>();
    List<Double> Data_Buffer_Temp = new ArrayList<Double>();
    List<Double> Data_Buffer_Hum = new ArrayList<Double>();
    List<Date> Data_Buffer_Calendar = new ArrayList<Date>();

    private static List<BluetoothGattCharacteristic> Characteristics_WriteQueue = new ArrayList<>();
    List<BluetoothGattCharacteristic> Characteristics_RequestQueue = new ArrayList<>();

    public static List<BluetoothGattCharacteristic> getWQ(){
        return Characteristics_WriteQueue;
    }

    private int connectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    static UUID MEASUREMENTS_SERVICE_UUID = UUID.fromString("2a13dada-295d-f7af-064f-28eac027639f");
    static UUID CO2_DATA_CHARACTERISTIC_UUID = UUID.fromString("4ef31e63-93b4-eca8-3846-84684719c484");
    static UUID PRESS_DATA_CHARACTERISTIC_UUID = UUID.fromString("0b4f4b0c-0795-1fab-a44d-ab5297a9d33b");
    static UUID TEMP_DATA_CHARACTERISTIC_UUID = UUID.fromString("7eb330af-8c43-f0ab-8e41-dc2adb4a3ce4");
    static UUID HUM_DATA_CHARACTERISTIC_UUID = UUID.fromString("421da449-112f-44b6-4743-5c5a7e9c9a1f");

    static UUID SETTINGS_SERVICE_UUID = UUID.fromString("2119458a-f72c-269b-4d4d-2df0319121dd");
    static UUID SAMPLE_RATE_CHARACTERISTIC_UUID = UUID.fromString("8420e6c6-49ba-7c8d-104f-10fe496d061f");
    static UUID ALARM_THRESHOLD_CHARACTERISTIC_UUID = UUID.fromString("4ffb7e99-85ba-de86-4242-004f76f23409");
    static UUID SOLDERING_COMPENSATION_CHARACTERISTIC_UUID = UUID.fromString("6f8afe94-a93d-cfb2-1b47-da0f98d9bfa1");
    static UUID ENABLE_CALIBRATION_CHARACTERISTIC_UUID = UUID.fromString("e64d0510-07f3-ac96-9c4d-5af82839425c");

    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

    public void requestCharacteristics(BluetoothGatt gatt) {
        gatt.readCharacteristic(Characteristics_RequestQueue.get(Characteristics_RequestQueue.size() - 1));
    }

    public void writeCharacteristics(BluetoothGatt gatt) {
        if (Characteristics_WriteQueue.get(Characteristics_WriteQueue.size()-1).getUuid().toString().equalsIgnoreCase(SAMPLE_RATE_CHARACTERISTIC_UUID.toString())) {      // Sample Rate Setting Read
            byte[] SampleRateTest = new byte[2];
            SampleRateTest[0] = (byte) (Setting_Buffer_SampleRate & 0xFF);       // Low Byte
            SampleRateTest[1] = (byte) (Setting_Buffer_SampleRate >> 8);       // High Byte

            Characteristics_WriteQueue.get(Characteristics_WriteQueue.size()-1).setValue(SampleRateTest);
        } else if (Characteristics_WriteQueue.get(Characteristics_WriteQueue.size()-1).getUuid().toString().equalsIgnoreCase(ALARM_THRESHOLD_CHARACTERISTIC_UUID.toString())) {      // Alarm Threshold Setting Read
            byte[] AlarmThresholdTest = new byte[2];
            AlarmThresholdTest[0] = (byte) (Setting_Buffer_AlarmThreshold & 0xFF);       // Low Byte
            AlarmThresholdTest[1] = (byte) (Setting_Buffer_AlarmThreshold >> 8);       // High Byte

            Characteristics_WriteQueue.get(Characteristics_WriteQueue.size()-1).setValue(AlarmThresholdTest);
        } else if (Characteristics_WriteQueue.get(Characteristics_WriteQueue.size()-1).getUuid().toString().equalsIgnoreCase(SOLDERING_COMPENSATION_CHARACTERISTIC_UUID.toString())) {      // Soldering Compensation Setting Read
            byte[] SolderingCompensationTest = new byte[4];
            SolderingCompensationTest[0] = (byte) (Setting_Buffer_SolderingCompensation & 0xFF);       // Low Byte
            SolderingCompensationTest[1] = (byte) (Setting_Buffer_SolderingCompensation >> 8);        // High Byte
            SolderingCompensationTest[2] = (byte) (Setting_Buffer_SolderingCompensation >> 16);       // High Byte
            SolderingCompensationTest[3] = (byte) (Setting_Buffer_SolderingCompensation >> 24);       // High Byte

            Characteristics_WriteQueue.get(Characteristics_WriteQueue.size()-1).setValue(SolderingCompensationTest);
        }

        Characteristics_WriteQueue.get(Characteristics_WriteQueue.size()-1).setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        gatt.writeCharacteristic(Characteristics_WriteQueue.get(Characteristics_WriteQueue.size()-1));
    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                Statusview.setText("BT: Connected to " + macaddr);

                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
                Statusview.setText("BT: Disconnected");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                Log.i(TAG, "Services (Len: " + services.size() + "): " + services.toString());

                for (BluetoothGattService service : services) {
                    if (service.getUuid().toString().equalsIgnoreCase(MEASUREMENTS_SERVICE_UUID.toString())) {      // Measurement Service Found
                        try {
                            BluetoothGattCharacteristic CO2_Characteristic = bluetoothGatt.getService(MEASUREMENTS_SERVICE_UUID).getCharacteristic(CO2_DATA_CHARACTERISTIC_UUID);
                            bluetoothGatt.setCharacteristicNotification(CO2_Characteristic, true);
                            BluetoothGattCharacteristic Press_Characteristic = bluetoothGatt.getService(MEASUREMENTS_SERVICE_UUID).getCharacteristic(PRESS_DATA_CHARACTERISTIC_UUID);
                            bluetoothGatt.setCharacteristicNotification(Press_Characteristic, true);
                            BluetoothGattCharacteristic Temp_Characteristic = bluetoothGatt.getService(MEASUREMENTS_SERVICE_UUID).getCharacteristic(TEMP_DATA_CHARACTERISTIC_UUID);
                            bluetoothGatt.setCharacteristicNotification(Temp_Characteristic, true);
                            BluetoothGattCharacteristic Hum_Characteristic = bluetoothGatt.getService(MEASUREMENTS_SERVICE_UUID).getCharacteristic(HUM_DATA_CHARACTERISTIC_UUID);
                            bluetoothGatt.setCharacteristicNotification(Hum_Characteristic, true);

                            Characteristics_RequestQueue.add(Temp_Characteristic);
                            Characteristics_RequestQueue.add(Press_Characteristic);
                            Characteristics_RequestQueue.add(CO2_Characteristic);
                            Characteristics_RequestQueue.add(Hum_Characteristic);
                        }
                        catch(Exception e){};
                    }
                }
                requestCharacteristics(gatt);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Calendar calendar = Calendar.getInstance();
                Date d1 = calendar.getTime();

                if (characteristic.getUuid().toString().equalsIgnoreCase(CO2_DATA_CHARACTERISTIC_UUID.toString())) {       // CO2 Data Notification
                    byte[] charValue = characteristic.getValue();
                    Data_Buffer_Calendar.add((Date) d1);        // Update Time/Calendar Buffer only on CO2 Notification
                    int CO2_Raw = ((charValue[1] & 0xFF) << 8) + (charValue[0] & 0xFF);
                    Data_Buffer_CO2.add((double) CO2_Raw);

                    if (activeMode == 0) {
                        GraphSeries.appendData(new DataPoint(d1, CO2_Raw), true, 5000);
                        Viewport viewport = graph.getViewport();
                        viewport.setYAxisBoundsManual(true);

                        viewport.setMinY(Collections.min(Data_Buffer_CO2) * 0.1);
                        viewport.setMaxY(Collections.max(Data_Buffer_CO2) * 2);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UpdateRadioButton(0);   // Update CO2 RadioButton TextView
                        }
                    });
                } else if (characteristic.getUuid().toString().equalsIgnoreCase(PRESS_DATA_CHARACTERISTIC_UUID.toString())) {      // Pressure Data Notification
                    byte[] charValue = characteristic.getValue();
                    int Press_Raw = ((charValue[3] & 0xFF) << 24) + ((charValue[2] & 0xFF) << 16) + ((charValue[1] & 0xFF) << 8) + (charValue[0] & 0xFF);
                    Data_Buffer_Press.add((double) Press_Raw);

                    if (activeMode == 1) {
                        GraphSeries.appendData(new DataPoint(d1, Press_Raw), true, 5000);
                        Viewport viewport = graph.getViewport();
                        viewport.setYAxisBoundsManual(true);

                        viewport.setMinY(Collections.min(Data_Buffer_Press) * 0.99);
                        viewport.setMaxY(Collections.max(Data_Buffer_Press) * 1.01);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UpdateRadioButton(1);   // Update Pressure RadioButton TextView
                        }
                    });
                } else if (characteristic.getUuid().toString().equalsIgnoreCase(TEMP_DATA_CHARACTERISTIC_UUID.toString())) {      // Temperature Data Notification
                    byte[] charValue = characteristic.getValue();
                    int Temp_Raw_Low = ((charValue[3] & 0xFF) << 8) + (charValue[2] & 0xFF);
                    int Temp_Raw_High = ((charValue[1] & 0xFF) << 8) + (charValue[0] & 0xFF);
                    double Temp_Raw = Temp_Raw_High + ((double) Temp_Raw_Low / 1000);
                    Data_Buffer_Temp.add(Temp_Raw);

                    if (activeMode == 2) {
                        GraphSeries.appendData(new DataPoint(d1, Temp_Raw), true, 5000);
                        Viewport viewport = graph.getViewport();
                        viewport.setYAxisBoundsManual(true);

                        viewport.setMinY(Collections.min(Data_Buffer_Temp) * 0.8);
                        viewport.setMaxY(Collections.max(Data_Buffer_Temp) * 1.2);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UpdateRadioButton(2);   // Update Temperature RadioButton TextView
                        }
                    });
                } else if (characteristic.getUuid().toString().equalsIgnoreCase(HUM_DATA_CHARACTERISTIC_UUID.toString())) {      // Temperature Data Notification
                    byte[] charValue = characteristic.getValue();
                    int Hum_Raw_Low = ((charValue[3] & 0xFF) << 8) + (charValue[2] & 0xFF);
                    int Hum_Raw_High = ((charValue[1] & 0xFF) << 8) + (charValue[0] & 0xFF);
                    double Hum_Raw = Hum_Raw_High + ((double) Hum_Raw_Low / 1000);
                    Data_Buffer_Hum.add(Hum_Raw);

                    if (activeMode == 3) {
                        GraphSeries.appendData(new DataPoint(d1, Hum_Raw), true, 5000);
                        Viewport viewport = graph.getViewport();
                        viewport.setYAxisBoundsManual(true);

                        viewport.setMinY(0);
                        viewport.setMaxY(100);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            UpdateRadioButton(3);   // Update Temperature RadioButton TextView
                        }
                    });
                } else {
                    int[] charValue = new int[4];
                    for (int i = 0; i < characteristic.getValue().length; i++)
                        charValue[i] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, i);

                    if (characteristic.getUuid().toString().equalsIgnoreCase(SAMPLE_RATE_CHARACTERISTIC_UUID.toString())) {             // Sample Rate Setting Read
                        Setting_Buffer_SampleRate = (charValue[1] << 8) + charValue[0];
                        Log.i(TAG, "SampleRate: " + Setting_Buffer_SampleRate);
                    }
                    else if (characteristic.getUuid().toString().equalsIgnoreCase(ALARM_THRESHOLD_CHARACTERISTIC_UUID.toString())) {              // Alarm Threshold Setting Read
                        Setting_Buffer_AlarmThreshold = (charValue[1] << 8) + charValue[0];
                        Log.i(TAG, "AlarmThreshold: " + Setting_Buffer_AlarmThreshold);
                    }
                    else if (characteristic.getUuid().toString().equalsIgnoreCase(SOLDERING_COMPENSATION_CHARACTERISTIC_UUID.toString())) {      // Soldering Compensation Setting Read
                        Setting_Buffer_SolderingCompensation = (charValue[3] << 24) + (charValue[2] << 16) + (charValue[1] << 8) + charValue[0];
                        Log.i(TAG, "SolderingCompensation: " + Setting_Buffer_SolderingCompensation);
                    }
                }

                Log.i(TAG, "CurrentSize: " + Characteristics_RequestQueue.size());
                Characteristics_RequestQueue.remove(Characteristics_RequestQueue.get(Characteristics_RequestQueue.size() - 1));
                if (Characteristics_RequestQueue.size() > 0)
                    requestCharacteristics(gatt);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Characteristics_WriteQueue.remove(Characteristics_WriteQueue.get(Characteristics_WriteQueue.size() - 1));

                if (Characteristics_WriteQueue.size() > 0)
                    writeCharacteristics(gatt);
            }
        }

        @Override
        // Characteristic notification
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] charValue = characteristic.getValue();
            Calendar calendar = Calendar.getInstance();
            Date d1 = calendar.getTime();

            if (characteristic.getUuid().toString().equalsIgnoreCase(CO2_DATA_CHARACTERISTIC_UUID.toString())) {       // CO2 Data Notification
                Data_Buffer_Calendar.add((Date) d1);        // Update Time/Calendar Buffer only on CO2 Notification
                int CO2_Raw = ((charValue[1] & 0xFF) << 8) + (charValue[0] & 0xFF);
                Data_Buffer_CO2.add((double) CO2_Raw);

                if (activeMode == 0) {
                    GraphSeries.appendData(new DataPoint(d1, CO2_Raw), true, 5000);
                    Viewport viewport = graph.getViewport();
                    viewport.setYAxisBoundsManual(true);

                    viewport.setMinY(Collections.min(Data_Buffer_CO2) * 0.1);
                    viewport.setMaxY(Collections.max(Data_Buffer_CO2) * 2);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UpdateRadioButton(0);   // Update CO2 RadioButton TextView
                    }
                });
            } else if (characteristic.getUuid().toString().equalsIgnoreCase(PRESS_DATA_CHARACTERISTIC_UUID.toString())) {      // Pressure Data Notification
                int Press_Raw = ((charValue[3] & 0xFF) << 24) + ((charValue[2] & 0xFF) << 16) + ((charValue[1] & 0xFF) << 8) + (charValue[0] & 0xFF);
                Data_Buffer_Press.add((double) Press_Raw);

                if (activeMode == 1) {
                    GraphSeries.appendData(new DataPoint(d1, Press_Raw), true, 5000);
                    Viewport viewport = graph.getViewport();
                    viewport.setYAxisBoundsManual(true);

                    viewport.setMinY(Collections.min(Data_Buffer_Press) * 0.99);
                    viewport.setMaxY(Collections.max(Data_Buffer_Press) * 1.01);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UpdateRadioButton(1);   // Update Pressure RadioButton TextView
                    }
                });
            } else if (characteristic.getUuid().toString().equalsIgnoreCase(TEMP_DATA_CHARACTERISTIC_UUID.toString())) {      // Temperature Data Notification
                int Temp_Raw_Low = ((charValue[3] & 0xFF) << 8) + (charValue[2] & 0xFF);
                int Temp_Raw_High = ((charValue[1] & 0xFF) << 8) + (charValue[0] & 0xFF);

                double Temp_Raw = Temp_Raw_High + ((double) Temp_Raw_Low / 1000);
                Data_Buffer_Temp.add(Temp_Raw);

                if (activeMode == 2) {
                    GraphSeries.appendData(new DataPoint(d1, Temp_Raw), true, 5000);
                    Viewport viewport = graph.getViewport();
                    viewport.setYAxisBoundsManual(true);

                    viewport.setMinY(Collections.min(Data_Buffer_Temp) * 0.8);
                    viewport.setMaxY(Collections.max(Data_Buffer_Temp) * 1.2);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UpdateRadioButton(2);   // Update Temperature RadioButton TextView
                    }
                });
            } else if (characteristic.getUuid().toString().equalsIgnoreCase(HUM_DATA_CHARACTERISTIC_UUID.toString())) {      // Temperature Data Notification
                int Hum_Raw_Low = ((charValue[3] & 0xFF) << 8) + (charValue[2] & 0xFF);
                int Hum_Raw_High = ((charValue[1] & 0xFF) << 8) + (charValue[0] & 0xFF);
                double Hum_Raw = Hum_Raw_High + ((double) Hum_Raw_Low / 1000);
                Data_Buffer_Hum.add(Hum_Raw);

                if (activeMode == 3) {
                    GraphSeries.appendData(new DataPoint(d1, Hum_Raw), true, 5000);
                    Viewport viewport = graph.getViewport();
                    viewport.setYAxisBoundsManual(true);

                    viewport.setMinY(0);
                    viewport.setMaxY(100);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UpdateRadioButton(3);   // Update Temperature RadioButton TextView
                    }
                });
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.i(TAG, "onDescriptorWrite");
        }
    };

    //  This start is initiated by the MainActivity.java
    //  It will initialize the displayed elements and starts
    //  the connection with the sensor.
    //	input:  savedInstanceState (handles by the Android System)
    //
    //	output: -
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagram);

        GraphSeries = new LineGraphSeries<DataPoint>();
        GraphSeries.setColor(getColors(0));
        filename = Calendar.getInstance().getTime().toString() + ".csv";

        SensorNames = new ArrayList<>();
        SensorNames.add("CO2");
        SensorNames.add("Pressure");
        SensorNames.add("Temperature");
        SensorNames.add("Humidity");

        SensorUnits = new ArrayList<>();
        SensorUnits.add("ppm");
        SensorUnits.add("Pa");
        SensorUnits.add("°C");
        SensorUnits.add("%");

        DataSetsCount = SensorNames.size();
        InitUI();

        for (int k = 0; k < SensorNames.size(); k++)
            CreateRadioButtons(k);

        checkRadioButton(activeMode);
        UpdateGraphLabels();

        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String filePath = baseDir + File.separator + filename;
        file = new File(filePath);

        macaddr = getIntent().getExtras().getString("macaddr");
        ConnectSensorHub(macaddr);
    }

    private void ConnectSensorHub(String address) {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        BluetoothDevice deviceBT;
        deviceBT = bluetoothAdapter.getRemoteDevice(address);
        bluetoothGatt = deviceBT.connectGatt(this, false, gattCallback);
    }

    private void DisconnectSensorHub() {
        bluetoothGatt.close();
        bluetoothGatt = null;
    }


    //	this function is called when the screen is rotated.
    //  It takes care of the recreation and rescaling of
    //  the elements on the screen.
    //	input:  newConfig (handled by the Android System)
    //
    //	output: rotation of the screen
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_diagram);

        InitUI();

        for (int k = 0; k < SensorNames.size(); k++)
            CreateRadioButtons(k);

        checkRadioButton(activeMode);
        UpdateGraphLabels();
        updateColorOfSaveBtn();

        if (connectionState == BluetoothProfile.STATE_CONNECTED)
            Statusview.setText("BT: Connected to " + macaddr);
        else if (connectionState == BluetoothProfile.STATE_DISCONNECTED)
            Statusview.setText("BT: Disconnected");
    }

    //	Is called when the activity should be destroyed.
    //  This e.g. happens when the back button is pressed.
    //  The function will stop BT communication and closes
    //  the activity.
    //	input:  -
    //
    //	output: go back to the previous activity (handled by the Android System)
    @Override
    protected void onDestroy()
    {
        if (isSaveing)
            writeToStorage();

        DisconnectSensorHub();
        super.onDestroy();
    }

    //  Is called when the app should be paused. Then the
    //  csv file will be saved.
    //  input:  -
    //
    //  output: saved csv file and paused app
    @Override
    protected void onPause()
    {
        if (isSaveing)
            writeToStorage();

        super.onPause();
    }

    //	Is called when the App is created or the screen
    //  rotation is changed. It will initialize the UI
    //  elements and configures the graph view.
    //	input:  -
    //
    //	output: -
    public void InitUI()
    {
        Statusview = findViewById(R.id.BTstatusText);
        swipeLayout = findViewById(R.id.swipeContainer);

        // Scheme colors for animation
        swipeLayout.setColorSchemeColors(
                getResources().getColor(R.color.color4),
                getResources().getColor(R.color.color1),
                getResources().getColor(R.color.color2),
                getResources().getColor(R.color.color3)
        );

        // using the GraphView library which can be found here: http://www.android-graphview.org/
        graph = findViewById(R.id.graph);
        graph.addSeries(GraphSeries);

        graph.getLegendRenderer().resetStyles();
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        graph.getLegendRenderer().setVisible(true);

        graph.getGridLabelRenderer().setVerticalLabelsAlign(Paint.Align.LEFT);
        graph.getGridLabelRenderer().setLabelVerticalWidth(160);
        graph.getGridLabelRenderer().setHumanRounding(true);
        graph.onDataChanged(true, true);

        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                    return formatter.format(value);
                } else
                    return super.formatLabel(value, false);       // show normal y values
            }
        });

        graph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 because of the space
        graph.getGridLabelRenderer().setHumanRounding(false, true);

        Viewport viewport = graph.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(300000);        // display the dataToDisplay of the last 30 seconds

        viewport.setScrollable(true);
        RelativeLayoutInScrollView = findViewById(R.id.layoutBottom);

        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    DisconnectSensorHub();
                    ConnectSensorHub(macaddr);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeLayout.setRefreshing(false);
                    }
                }, 4000);
            }
        });
    }

    //	Is called when on of the radio buttons
    //  is pressed or Bt is initialized. It changes
    //  the states of the radio buttons and edits the
    //  graph view labels.
    //	input:  -
    //
    //	output: changed graph
    public void UpdateGraphLabels() {
        graph.getLegendRenderer().resetStyles();
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        try {
            GraphSeries.setTitle(SensorNames.get(activeMode));
            checkRadioButton(activeMode);
            graph.getGridLabelRenderer().setVerticalAxisTitle(SensorNames.get(activeMode) + " / " + SensorUnits.get(activeMode));
            GraphSeries.setColor(getColors(activeMode));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //	Updates the RadioButtons to show the last measured
    //  values received from the sensor.
    //	input:  -
    //
    //	output: updated RadioButton including TextView
    public void UpdateRadioButton(final int k) {
        int idTV = basicIdTV + k;
        TextView tv = findViewById(idTV);
        DecimalFormat FloatFormatter = new DecimalFormat("#.##");
        DecimalFormat IntFormatter = new DecimalFormat("#");

        switch (k) {
            case 0:
                tv.setText(IntFormatter.format(Data_Buffer_CO2.get(Data_Buffer_CO2.size() - 1)) + " " + SensorUnits.get(k));
                break;
            case 1:
                tv.setText(IntFormatter.format(Data_Buffer_Press.get(Data_Buffer_Press.size() - 1)) + " " + SensorUnits.get(k));
                break;
            case 2:
                tv.setText(FloatFormatter.format(Data_Buffer_Temp.get(Data_Buffer_Temp.size() - 1)) + " " + SensorUnits.get(k));
                break;
            case 3:
                tv.setText(FloatFormatter.format(Data_Buffer_Hum.get(Data_Buffer_Hum.size() - 1)) + " " + SensorUnits.get(k));
                break;
            default:
                tv.setText("TV Set Text Error");
                break;
        }
    }

    //	Creates the RadioButtons dynamically according to the
    //  values received from the sensor.
    //	input:  number of sensor Id
    //
    //	output: created RadioButton including name and TextView below
    public void CreateRadioButtons(final int k) {
        RadioButton rb = new RadioButton(this);
        int idRB = basicIdRB + k;
        rb.setId(idRB);
        rb.setText(SensorNames.get(k));

        TextView tv = new TextView(this);
        int idTV = basicIdTV + k;
        tv.setId(idTV);

        final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        final RelativeLayout.LayoutParams paramsTV = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        paramsTV.addRule(BELOW, idRB);
        Resources r = this.getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, r.getDisplayMetrics());
        paramsTV.setMarginStart(px);

        if (k > 0) {        // Change Position of all Buttons after the first one
            idRB = basicIdRB + k - 1;
            params.addRule(END_OF, idRB);

            idRB = basicIdRB + k;
            paramsTV.addRule(ALIGN_LEFT, idRB);
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        params.width = (displayMetrics.widthPixels / 4);
        rb.setLayoutParams(params);
        tv.setLayoutParams(paramsTV);

        int color = getColors(k);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            rb.setButtonTintList(ColorStateList.valueOf(color));

        rb.setHighlightColor(color);
        RelativeLayoutInScrollView.addView(rb);
        RelativeLayoutInScrollView.addView(tv);

        rb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                activeMode = k;
                GraphSeries.resetData(new DataPoint[0]);
                GraphSeries.setColor(getColors(activeMode));
                UpdateGraphLabels();

                Viewport viewport = graph.getViewport();
                viewport.setYAxisBoundsManual(true);

                switch (k) {
                    case 0:
                        for (int i = 0; i < Data_Buffer_CO2.size(); i++)
                            GraphSeries.appendData(new DataPoint(Data_Buffer_Calendar.get(i), Data_Buffer_CO2.get(i)), true, 5000);

                        viewport.setMinY(Collections.min(Data_Buffer_CO2) * 0.1);
                        viewport.setMaxY(Collections.max(Data_Buffer_CO2) * 2);
                        break;
                    case 1:
                        for (int i = 0; i < Data_Buffer_Press.size(); i++)
                            GraphSeries.appendData(new DataPoint(Data_Buffer_Calendar.get(i), Data_Buffer_Press.get(i)), true, 5000);

                        viewport.setMinY(Collections.min(Data_Buffer_Press) * 0.99);
                        viewport.setMaxY(Collections.max(Data_Buffer_Press) * 1.01);
                        break;
                    case 2:
                        for (int i = 0; i < Data_Buffer_Temp.size(); i++)
                            GraphSeries.appendData(new DataPoint(Data_Buffer_Calendar.get(i), Data_Buffer_Temp.get(i)), true, 5000);


                        viewport.setMinY(Collections.min(Data_Buffer_Temp) * 0.8);
                        viewport.setMaxY(Collections.max(Data_Buffer_Temp) * 1.2);
                        break;
                    case 3:
                        for (int i = 0; i < Data_Buffer_Hum.size(); i++)
                            GraphSeries.appendData(new DataPoint(Data_Buffer_Calendar.get(i), Data_Buffer_Hum.get(i)), true, 5000);


                        viewport.setMinY(0);
                        viewport.setMaxY(100);
                        break;
                    default:
                        GraphSeries.resetData(new DataPoint[0]);
                        break;
                }
            }
        });
    }

    //	Updates the RadioButtons when one of them is clicked and
    //  un-checks the others.
    //	input:  number of checked RadioButton
    //
    //	output: checks and un-checks the RBs on the screen
    public void checkRadioButton(int x) {
        for (int i = 0; i < DataSetsCount; i++) {
            int id = basicIdRB + i;
            RadioButton rb = findViewById(id);
            if (i == x)
                rb.setChecked(true);
            else
                rb.setChecked(false);
        }
    }

    //	Returns the color of the according RadioButton
    //	input:  number of RadioButton / Color wanted
    //
    //	output: color code as int variable
    public int getColors(int y) {
        int color;
        switch (y % 4) {
            case 0:
                color = getResources().getColor(R.color.color1);
                break;
            case 1:
                color = getResources().getColor(R.color.color2);
                break;
            case 2:
                color = getResources().getColor(R.color.color3);
                break;
            case 3:
                color = getResources().getColor(R.color.color4);
                break;
            default:
                color = getResources().getColor(R.color.color3);
                break;
        }
        return color;
    }

    //	Is called whenever the save button is clicked
    //	input:  click event
    //
    //	output: saving turned on or off
    public void saveBtnClicked(View view) {
        // check if you have permission to write to storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                Log.v("permission", "Permission is granted");
            else {
                Log.v("permission", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
        } else            //permission is automatically granted on sdk<23 upon installation
            Log.v("permission", "Permission is granted");

        // save the file before you deactivate saveing mode
        if (isSaveing)
            writeToStorage();

        isSaveing = !isSaveing;
        updateColorOfSaveBtn();
    }

    //	Is called when the button is clicked or
    //  when the device is rotated. It takes care
    //  that the right image is displayed.
    //	input:  -
    //
    //	output: updated image of the btn
    public void updateColorOfSaveBtn() {
        ImageView img = findViewById(R.id.saveBtn);

        if (isSaveing) {
            img.setImageResource(R.drawable.outline_save_24);
            Toast.makeText(getApplicationContext(), getString(R.string.file_saved) + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d("SAVE", getString(R.string.file_saved) + file.getAbsolutePath());
            writeToStorage();
        } else
            img.setImageResource(R.drawable.outline_save_black_24);
    }

    //  Whenever this function is called, the received dataToDisplay
    //  will be stored to the csv file. The file can be found
    //  in the default storage directory (e.g. /storage/emulated/0/)
    //	input:  dataToDisplay to store
    //
    //	output: csv file in the system memory
    public void writeToStorage() {
        if (!file.exists()) {       // Create the File
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {                      // File already exists
            try {
                FileWriter fileWriter = new FileWriter(file, true);
                BufferedWriter bfWriter = new BufferedWriter(fileWriter);
                bfWriter.write(dataForCSV);
                dataForCSV = "";
                bfWriter.close();

                Log.i("file", "csv file saved");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //  Called when the settings button is clicked. Opens the
    //  Settings Activity.
    //	input:  -
    //
    //	output: started settings Activity
    public void settingsBtnClicked(View view) {
        Intent intent = new Intent(diagram.this, settings.class);
        // Log.i(TAG, "CurrentSampleRate:  " + Setting_Buffer_SampleRate + "CurrentPressureCompensation:  " + Setting_Buffer_PressureCompensation + "CurrentAlarmThreshold:  " + Setting_Buffer_AlarmThreshold + "CurrentSolderingCompensation:  " + Setting_Buffer_SolderingCompensation);
        List<BluetoothGattService> services = bluetoothGatt.getServices();

        for (BluetoothGattService service : services) {
            if (service.getUuid().toString().equalsIgnoreCase(MEASUREMENTS_SERVICE_UUID.toString())) {      // Measurement Service Found
            try {
                BluetoothGattCharacteristic SampleRate_Characteristic = bluetoothGatt.getService(SETTINGS_SERVICE_UUID).getCharacteristic(SAMPLE_RATE_CHARACTERISTIC_UUID);
                BluetoothGattCharacteristic AlarmThreshold_Characteristic = bluetoothGatt.getService(SETTINGS_SERVICE_UUID).getCharacteristic(ALARM_THRESHOLD_CHARACTERISTIC_UUID);
                BluetoothGattCharacteristic SolderingCompensation_Characteristic = bluetoothGatt.getService(SETTINGS_SERVICE_UUID).getCharacteristic(SOLDERING_COMPENSATION_CHARACTERISTIC_UUID);

                Characteristics_RequestQueue.add(SampleRate_Characteristic);
                Characteristics_RequestQueue.add(AlarmThreshold_Characteristic);
                Characteristics_RequestQueue.add(SolderingCompensation_Characteristic);
            }
            catch(Exception e){};
        }
}
        requestCharacteristics(bluetoothGatt);
        while(Characteristics_RequestQueue.size() != 0);

        intent.putExtra("CurrentSampleRate", Setting_Buffer_SampleRate / 1000);
        intent.putExtra("CurrentAlarmThreshold", Setting_Buffer_AlarmThreshold);
        intent.putExtra("CurrentSolderingCompensation", Setting_Buffer_SolderingCompensation);
        startActivityForResult(intent, 1);
    }

    //  This function is called, when the settings Activity is closed.
    //	input:  Intent with Data from Activity
    //
    //	output: Transmission of changed modes to Sensor
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result)
    {
        super.onActivityResult(requestCode, resultCode, result);
        Log.d("gotAction", "onActivityResult");

        try {
            Bundle ActivityBundle = result.getExtras();
            Setting_Buffer_SampleRate = 1000 * ActivityBundle.getInt("SampleRate", 99);
            Setting_Buffer_AlarmThreshold = ActivityBundle.getInt("AlarmThreshold", 99);
            Setting_Buffer_SolderingCompensation = ActivityBundle.getInt("SolderingCompensation", 99);

            BluetoothGattCharacteristic SampleRate_Characteristic = bluetoothGatt.getService(SETTINGS_SERVICE_UUID).getCharacteristic(SAMPLE_RATE_CHARACTERISTIC_UUID);
            Characteristics_WriteQueue.add(SampleRate_Characteristic);

            BluetoothGattCharacteristic AlarmThreshold_Characteristic = bluetoothGatt.getService(SETTINGS_SERVICE_UUID).getCharacteristic(ALARM_THRESHOLD_CHARACTERISTIC_UUID);
            Characteristics_WriteQueue.add(AlarmThreshold_Characteristic);

            BluetoothGattCharacteristic SolderingCompensation_Characteristic = bluetoothGatt.getService(SETTINGS_SERVICE_UUID).getCharacteristic(SOLDERING_COMPENSATION_CHARACTERISTIC_UUID);
            Characteristics_WriteQueue.add(SolderingCompensation_Characteristic);

            writeCharacteristics(bluetoothGatt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
