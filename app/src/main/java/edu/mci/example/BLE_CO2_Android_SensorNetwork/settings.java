// *******************************************************************
//
//	File Name:		settings.java
//
//	Description:	Activity that handles the Settings
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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.mci.example.BLE_CO2_Android_SensorNetwork.R;

public class settings extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private final int basicIdTextInput = 20000;
    private final int basicIdTvSettings = 10000;
    private ArrayList<Integer> editTextPosArray;
    private String TAG = "LOG";

    private Integer Current_SampleRate;
    private Integer Current_AlarmThreshold;
    private Integer Current_SolderingCompensation;

    public static final int LONG_PRESS_DELAY_MILLIS = 5000;
    public boolean LONG_PRESS_DONE = false;

    //  Called when the settings Activity is created. The UI
    //  is created dynamically.
    //	input:  Data from diagram Activity
    //
    //	output: Settings view visible
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editTextPosArray = new ArrayList<>();
        Current_SampleRate = getIntent().getIntExtra("CurrentSampleRate", 0);
        Current_AlarmThreshold = getIntent().getIntExtra("CurrentAlarmThreshold", 0);
        Current_SolderingCompensation = getIntent().getIntExtra("CurrentSolderingCompensation", 0);

        LinearLayout LinearLayout = findViewById(R.id.LayoutForContent);
        AddManualEditText(LinearLayout, "Sample Rate / s", Current_SampleRate.toString(), true, true,0);
        AddManualEditText(LinearLayout, "Alarm Threshold / ppm", Current_AlarmThreshold.toString(), true, true, 1);
        AddManualEditText(LinearLayout, "Generic Offset Compensation / ppm", Current_SolderingCompensation.toString(), true, false, 2);

        LinearLayout LayoutButton = findViewById(R.id.LayoutButton);
        Button Save_bt = new Button(this);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));

        Save_bt.setGravity(Gravity.CENTER);
        Save_bt.setLayoutParams(buttonParams);
        Save_bt.setText(getResources().getText(R.string.save));

        Save_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSavePressed();
            }
        });

        Save_bt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                revealCalibrationButton(v, System.currentTimeMillis());
                return true;
            }
        });

        LayoutButton.addView(Save_bt);
    }

    //  This function is called, when the Spinner (drop-down menu) is clicked and
    //  an item is selected. This selected item / mode will be caught here.
    //	input:  selected mode
    //
    //	output: -
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    //  This function is called, when there is no selection at the Spinner.
    //  This is not possible due to the UI, but needs to be there for
    //  complete implementation.
    //	input:  -
    //
    //	output: -
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    //  Called when the Back-Button is pressed
    //	input:  -
    //
    //	output: closes the settings Activity and pushes
    //          the chosen modes to the diagram Activity.
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void onSavePressed() {
        Intent resultIntent = getIntent();
        EditText TextField_SampleRate = findViewById(basicIdTextInput + editTextPosArray.get(0));
        resultIntent.putExtra("SampleRate", Integer.parseInt(TextField_SampleRate.getText().toString()));

        EditText TextField_AlarmThreshold = findViewById(basicIdTextInput + editTextPosArray.get(1));
        resultIntent.putExtra("AlarmThreshold", Integer.parseInt(TextField_AlarmThreshold.getText().toString()));

        EditText TextField_SolderingCompensation = findViewById(basicIdTextInput + editTextPosArray.get(2));
        resultIntent.putExtra("SolderingCompensation", Integer.parseInt(TextField_SolderingCompensation.getText().toString()));
        setResult(1, resultIntent);
        finish();
    }

    private void revealCalibrationButton(final View v, final long startTime) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (v.isPressed() && (System.currentTimeMillis() - startTime >= LONG_PRESS_DELAY_MILLIS) && (!LONG_PRESS_DONE)) {
                    Button Calib_bt = new Button(getApplicationContext());
                    Button Reset_bt = new Button(getApplicationContext());
                    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));

                    Calib_bt.setGravity(Gravity.CENTER);
                    Calib_bt.setLayoutParams(buttonParams);
                    Calib_bt.setText(getResources().getText(R.string.calib_bt_text));

                    Reset_bt.setGravity(Gravity.CENTER);
                    Reset_bt.setLayoutParams(buttonParams);
                    Reset_bt.setText(getResources().getText(R.string.reset_bt_text));

                    Calib_bt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onCalibrationPressed();
                        }
                    });
                    Reset_bt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onResetPressed();
                        }
                    });

                    LinearLayout LayoutButton = findViewById(R.id.LayoutButton);
                    TextView tv = new TextView(getApplicationContext());
                    tv.setText("Warning: Only use the functions below if you are absolutely sure what you are doing. Failure to do so might cause non-reversible damage to your XENSIV PAS CO2 sensor.\n" +
                                "\nReference Limits: 350 to 900ppm");
                    tv.setTextSize(20);
                    tv.setGravity(Gravity.LEFT);
                    tv.setTextColor(Color.parseColor("#FF0000"));
                    tv.setLayoutParams(buttonParams);
                    tv.setPadding(10,0,10,0);
                    tv.setTypeface(null, Typeface.BOLD);

                    LayoutButton.addView(tv);
                    LayoutButton.addView(Calib_bt);
                    LayoutButton.addView(Reset_bt);

                    LONG_PRESS_DONE = true;
                    return;
                } else if (!v.isPressed()) {
                    return;
                }
            }
        }, LONG_PRESS_DELAY_MILLIS);

    }

    public void onCalibrationPressed() {
        BluetoothGatt bluetoothGatt = diagram.getBT();
        List<BluetoothGattCharacteristic> Characteristics_WriteQueue = diagram.getWQ();

        byte[] StartCalibration_MSG = new byte[1];
        StartCalibration_MSG[0] = (byte) 1;     // Value to Trigger the XENSIV PAS CO2 Calibration Process

        BluetoothGattCharacteristic Calibration_Characteristic = bluetoothGatt.getService(diagram.SETTINGS_SERVICE_UUID).getCharacteristic(diagram.ENABLE_CALIBRATION_CHARACTERISTIC_UUID);
        Calibration_Characteristic.setValue(StartCalibration_MSG);
        Characteristics_WriteQueue.add(Calibration_Characteristic);

        BluetoothGattCharacteristic SolderingCompensation_Characteristic = bluetoothGatt.getService(diagram.SETTINGS_SERVICE_UUID).getCharacteristic(diagram.SOLDERING_COMPENSATION_CHARACTERISTIC_UUID);
        EditText TextField_SolderingCompensation = findViewById(basicIdTextInput + editTextPosArray.get(2));

        byte[] SolderingCompensationTest = new byte[4];
        int Testing = Integer.parseInt(TextField_SolderingCompensation.getText().toString());
        SolderingCompensationTest[0] = (byte) (Testing & 0xFF);      // Low Byte
        SolderingCompensationTest[1] = (byte) (Testing >> 8);        // High Byte
        SolderingCompensationTest[2] = (byte) (Testing >> 16);       // High Byte
        SolderingCompensationTest[3] = (byte) (Testing >> 24);       // High Byte

        SolderingCompensation_Characteristic.setValue(SolderingCompensationTest);
        Characteristics_WriteQueue.add(SolderingCompensation_Characteristic);
        bluetoothGatt.writeCharacteristic(SolderingCompensation_Characteristic);
    }

    public void onResetPressed() {
        BluetoothGatt bluetoothGatt = diagram.getBT();

        byte[] ResetCalibration_MSG = new byte[1];
        ResetCalibration_MSG[0] = (byte) 255;   // Value to Trigger the Calibration Reset on XENSIV PAS CO2

        BluetoothGattCharacteristic Calibration_Characteristic = bluetoothGatt.getService(diagram.SETTINGS_SERVICE_UUID).getCharacteristic(diagram.ENABLE_CALIBRATION_CHARACTERISTIC_UUID);
        Calibration_Characteristic.setValue(ResetCalibration_MSG);

        bluetoothGatt.writeCharacteristic(Calibration_Characteristic);  // This Characteristic is the only one that is directly written to prevent Errors
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

    public void AddManualEditText(LinearLayout Layout, String TextView_Text, String EditText_InitialText, boolean DecimalOnly, boolean UnsignedOnly, int EditText_ID) {
        TextView tv = new TextView(this);
        tv.setId(basicIdTvSettings + EditText_ID);
        tv.setText(TextView_Text);
        tv.setTextSize(20);
        tv.setTextColor(Color.parseColor("#000000"));

        final LinearLayout.LayoutParams paramsTv = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
        Resources r = this.getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, r.getDisplayMetrics());
        paramsTv.setMarginStart(px);
        tv.setLayoutParams(paramsTv);

        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));
        px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, r.getDisplayMetrics());
        params.setMarginStart(px);
        Layout.addView(tv);

        try {
            editTextPosArray.add(EditText_ID);
            EditText TextInput = new AutoCompleteTextView(this);
            TextInput.setId(basicIdTextInput + EditText_ID);

            ((AutoCompleteTextView) TextInput).setText(EditText_InitialText);
            TextInput.setLayoutParams(params);
            TextInput.setSingleLine();

            if (DecimalOnly) {
                if (UnsignedOnly) {
                    TextInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    TextInput.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                } else {
                    TextInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    TextInput.setKeyListener(DigitsKeyListener.getInstance("-0123456789"));
                }
            }

            Layout.addView(TextInput);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


