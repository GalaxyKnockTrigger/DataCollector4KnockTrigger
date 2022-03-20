package com.capstone.knockdatareceiver4watch;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import androidx.databinding.DataBindingUtil;

import com.capstone.knockdatareceiver4watch.databinding.MainActivityBinding;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private MainActivityBinding binding;
    private SensorManager sensorManager;
    private Sensor accSensor;
    private Sensor gyroSensor;
    private MySensorEventListener accEvent, gyroEvent;

    private final int TYPE_ACCEL = Sensor.TYPE_ACCELEROMETER;
    private final int TYPE_GYRO = Sensor.TYPE_GYROSCOPE_UNCALIBRATED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(TYPE_ACCEL);
        gyroSensor = sensorManager.getDefaultSensor(TYPE_GYRO);

        Log.i("SENSOR_INFO", "ACC_"+ accSensor.getVendor() + "_" + accSensor.getMinDelay());
        Log.i("SENSOR_INFO", "GYRO_"+ gyroSensor.getVendor() + "_" + gyroSensor.getMinDelay());

        accEvent = new MySensorEventListener(accSensor.getType());
        gyroEvent = new MySensorEventListener(gyroSensor.getType());

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



    @Override
    protected void onStart() {
        super.onStart();
    }

    private Integer time;
    public void onButtonClick(){
        time = 3;

        Thread getDataTh = new Thread(() -> {
            sensorManager.registerListener(accEvent, accSensor, accSensor.getMinDelay());
            sensorManager.registerListener(gyroEvent, gyroSensor, gyroSensor.getMinDelay());
            try {
                Thread.sleep(1000);
                sensorManager.unregisterListener(accEvent);
                sensorManager.unregisterListener(gyroEvent);

                Log.i("ACC_INFO", String.format("%d_%d", accEvent.getDataSize()[0], accEvent.getDataSize()[1]));
                Log.i("GYRO_INFO", String.format("%d_%d", gyroEvent.getDataSize()[0], gyroEvent.getDataSize()[1]));

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Timer timer = new Timer();
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                (MainActivity.this).runOnUiThread(() -> binding.setTime(time.toString()));
                if(time == 0) {
                    timer.cancel();
                    Log.i("THREAD_INFO", "TimerThread Canceled");
                    getDataTh.start();
                }
                time--;

            }
        };
        timer.scheduleAtFixedRate(tt, 0, 1000);


    }

    class MySensorEventListener implements SensorEventListener {

        private final int TYPE;
        private final String TAG;

        private Queue<float[]> dataQueue;
        private Queue<Long> timestamps;

        MySensorEventListener(int type) {
            TYPE = type;
            if (TYPE == TYPE_ACCEL) {
                TAG = "ACC_DATA";
            } else {
                TAG = "GYRO_DATA";
            }

            dataQueue = new LinkedList<>();
            timestamps = new LinkedList<>();
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            timestamps.add(sensorEvent.timestamp);
            dataQueue.add(sensorEvent.values);

            // 나중에 보정이 필요하다면 그때 적용.
            if(TYPE == TYPE_ACCEL){
//                Log.i(TAG, String.format("%ll_%f_%f_%f", timestamp, values[0], values[1], values[2]));
            }else if (TYPE == TYPE_GYRO){
//                Log.i("GYRO_DATA", String.format("%ll_%f_%f_%f", timestamp, values[0], values[1], values[2]));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        public int[] getDataSize(){
            return new int []{timestamps.size(), dataQueue.size()};
        }
    }

}