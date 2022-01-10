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

import android.net.Uri;
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

import edu.mci.example.BLE_CO2_Android_SensorNetwork.R;


public class settings extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private final int basicIdTextInput = 20000;
    private final int basicIdTvSettings = 10000;
    private ArrayList<Integer> editTextPosArray;
    private String TAG = "LOG";

    private Integer Current_SampleRate;
    private Integer Current_AlarmThreshold;
    private Integer Current_SolderingCompensation;

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
        Button bt = new Button(this);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
        bt.setGravity(Gravity.CENTER);
        bt.setLayoutParams(buttonParams);
        bt.setText(getResources().getText(R.string.save));
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSavePressed();
            }
        });
        LayoutButton.addView(bt);
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
    public void onBackPressed()
    {
        finish();
        super.onBackPressed();
    }

    public void onSavePressed()
    {
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


