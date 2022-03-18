package com.capstone.knockdatareceiver4watch;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.databinding.DataBindingUtil;

import com.capstone.knockdatareceiver4watch.databinding.MainActivityBinding;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private MainActivityBinding binding;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> accSensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        List<Sensor> gyroSensors = sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);

        for(Sensor s : accSensors){
            Log.i("SENSOR_INFO", "ACC_"+ s.getVendor() + "_" + s.getMinDelay());
        }
        for(Sensor s : gyroSensors){
            Log.i("SENSOR_INFO", "GYRO_"+ s.getVendor() + "_" + s.getMinDelay());
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.setTime("");
        binding.setActivity(this);
/*
        if(checkSelfPermission(Manifest.permission.HIGH_SAMPLING_RATE_SENSORS) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.HIGH_SAMPLING_RATE_SENSORS}, 1000);
            ActivityResultLauncher<String> requestPermissionLauncher = registerFor;
            requestPermissionLauncher.launch(
                    Manifest.permission.REQUESTED_PERMISSION);
        }*/

    }

    private Integer time;
    public void onButtonClick(){
        time = 3;

        Timer timer = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                (MainActivity.this).runOnUiThread(() -> binding.setTime(time.toString()));
                time--;
            }
        };
        timer.scheduleAtFixedRate(tt, 0, 1000);

        Thread th = new Thread(() -> {
            while(time != 0){;}

            timer.cancel();
        });

        th.start();
        try {
            th.join(3100);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



}