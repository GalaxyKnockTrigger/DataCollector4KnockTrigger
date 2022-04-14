package com.capstone.knockdatareceiver4watch.listener;

import static com.capstone.knockdatareceiver4watch.Constants.RECEIVING_TIME;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.ShortBuffer;

public class AudioListener implements IGetDataAsCSV, IGetDataSize {
    private final AudioManager audioManager;
    private AudioRecord recorder;
    private final int SAMP_RATE = 48000;
    private final int bufferShortSize = SAMP_RATE * (RECEIVING_TIME/1000);
    private short[] bufferRecord;
    private int bufferRecordSize;
    private final ShortBuffer shortBuffer = ShortBuffer.allocate(SAMP_RATE * RECEIVING_TIME / 1000);
    private final Context context;

    public AudioListener(Context context) {
        this.context = context;
        audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        Log.i("AUDIO_INFO", audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
        recorder = null;
    }

    public void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        bufferRecordSize = bufferShortSize;
        bufferRecord = new short[bufferRecordSize];

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMP_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferRecord.length);

        Log.i("AUDIO_INFO", "SampleRate: " + audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
        Log.i("AUDIO_INFO", "Buffer Size: " + audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
        Log.i("AUDIO_INFO", "Buffer Record Size: " + bufferRecordSize);

        recorder.startRecording();
        shortBuffer.rewind();
        while(shortBuffer.position()+bufferRecordSize<bufferShortSize){
            shortBuffer.put(bufferRecord,0,recorder.read(bufferRecord,0,bufferRecordSize));
        }
    }

    private void stopRecording() {
        int Index=0;
        shortBuffer.position(0);

        recorder.stop();
        recorder.release();
        recorder = null;
    }

    public short[] getData(){
        return bufferRecord;
    }

    @Override
    public int getDataSize(){
        return bufferRecord.length;
    }
    @Override
    public String getDataAsCSV(){
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for(short val : getData()){
            builder.append(i).append(",").append(val).append("\n");
            i++;
            if(i >=( SAMP_RATE * RECEIVING_TIME / 1000) ||i >= 4096)
                break;
        }
        shortBuffer.clear();
        return builder.toString();
    }

}