package com.capstone.knockdatareceiver4watch;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.capstone.knockdatareceiver4watch.databinding.MainActivityBinding;

import java.io.IOException;
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

    private AudioManager audioManager;
    private String audioName = null;
    private MediaRecorder recorder;

    private final int TYPE_ACCEL = Sensor.TYPE_ACCELEROMETER;
    private final int TYPE_GYRO = Sensor.TYPE_GYROSCOPE_UNCALIBRATED;

    private boolean permissionToUseAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private static final int RCODE = 10000;

    private void onRecord(boolean start){
        if(start){
            startRecording();
        }else{
            stopRecording();
        }
    }

    private void startRecording() {
        audioName = getExternalCacheDir().getAbsolutePath();
        audioName += "/audiorecordtest.mpeg4";

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(audioName);
        Log.i("AUDIO_INFO", "NAME: " + audioName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("AUDIO_INFO", "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, permissions, RCODE);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(TYPE_ACCEL);
        gyroSensor = sensorManager.getDefaultSensor(TYPE_GYRO);

        Log.i("SENSOR_INFO", "ACC_"+ accSensor.getVendor() + "_" + accSensor.getMinDelay());
        Log.i("SENSOR_INFO", "GYRO_"+ gyroSensor.getVendor() + "_" + gyroSensor.getMinDelay());

        accEvent = new MySensorEventListener(accSensor.getType());
        gyroEvent = new MySensorEventListener(gyroSensor.getType());

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        Log.i("AUDIO_INFO", audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
        // Record to the external cache directory for visibility

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.setTime("");
        binding.setActivity(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case RCODE:
                permissionToUseAccepted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                break;
        }
        if(!permissionToUseAccepted) finish();
    }

    private Integer time;
    public void onButtonClick(){
        time = 3;

        Thread getDataTh = new Thread(() -> {
            onRecord(true);
            sensorManager.registerListener(accEvent, accSensor, accSensor.getMinDelay());
            sensorManager.registerListener(gyroEvent, gyroSensor, gyroSensor.getMinDelay());
            try {
                Thread.sleep(1000);
                onRecord(false);
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

        private final Queue<float[]> dataQueue;
        private final Queue<Long> timestamps;

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