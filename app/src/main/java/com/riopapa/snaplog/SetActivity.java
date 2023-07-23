package com.riopapa.snaplog;

import static com.riopapa.snaplog.Vars.mContext;
import static com.riopapa.snaplog.Vars.sharedAlpha;
import static com.riopapa.snaplog.Vars.sharedPref;
import static com.riopapa.snaplog.Vars.sharedRadius;
import static com.riopapa.snaplog.Vars.sharedSortType;
import static com.riopapa.snaplog.Vars.sharedZoomValue;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class SetActivity extends Activity {

    String[] radiusArray, sortArray, zoomArray;
    int radiusIdx, sortIdx, zoomIdx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);

        sharedPref = mContext.getSharedPreferences("snap", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        sortArray = getResources().getStringArray(R.array.sort_values);
        for (int i = 0; i < sortArray.length; i++) {
            if (sortArray[i].equals(sharedSortType)) {
                sortIdx = i;
                break;
            }
        }
        Spinner sortSpinner = findViewById(R.id.sort_spinner);
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.sort_values, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setSelection(sortIdx);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                sharedSortType = sortArray[position];
                editor.putString("sort", sharedSortType);
                editor.apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}
        });

        radiusArray = getResources().getStringArray(R.array.radius_values);
        for (int i = 0; i < radiusArray.length; i++) {
            if (radiusArray[i].equals(sharedRadius)) {
                radiusIdx = i;
                break;
            }
        }
        Spinner radiusSpinner = findViewById(R.id.radius_spinner);
        ArrayAdapter<CharSequence> radiusAdaptor = ArrayAdapter.createFromResource(this,
                R.array.radius_values, android.R.layout.simple_spinner_item);
        radiusAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        radiusSpinner.setAdapter(radiusAdaptor);
        radiusSpinner.setSelection(radiusIdx);
        radiusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                sharedRadius = radiusArray[position];
                editor.putString("radius", sharedRadius);
                editor.apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}
        });

        zoomArray = getResources().getStringArray(R.array.zoom_values);
        for (int i = 0; i < zoomArray.length; i++) {
            if (zoomArray[i].equals("" + sharedZoomValue)) {
                zoomIdx = i;
                break;
            }
        }
        Spinner zoomSpinner = findViewById(R.id.zoom_spinner);
        ArrayAdapter<CharSequence> zoomAdaptor = ArrayAdapter.createFromResource(this,
                R.array.zoom_values, android.R.layout.simple_spinner_item);
        zoomAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zoomSpinner.setAdapter(zoomAdaptor);
        zoomSpinner.setSelection(zoomIdx);
        zoomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int position, long arg3) {
                sharedZoomValue = Integer.parseInt(zoomArray[position]);
                editor.putInt("zoomValue", sharedZoomValue);
                editor.apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}
        });

        EditText etOpacity = findViewById(R.id.opacity);
        etOpacity.setText(sharedAlpha);
        etOpacity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(final Editable s) {
                sharedAlpha = s.toString();
                editor.putString("alpha", sharedAlpha);
                editor.apply();
            }
        });
    }
}