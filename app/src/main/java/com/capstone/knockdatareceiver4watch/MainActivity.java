package com.capstone.knockdatareceiver4watch;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.capstone.knockdatareceiver4watch.databinding.MainActivityBinding;

import org.conscrypt.Conscrypt;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;
import okio.BufferedSink;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends Activity {

    private MainActivityBinding binding;

    private SensorManager sensorManager;
    private Sensor accSensor;
    private Sensor gyroSensor;
    private MySensorEventListener accEvent, gyroEvent;


    private final int TYPE_ACCEL = Sensor.TYPE_ACCELEROMETER;
    private final int TYPE_GYRO = Sensor.TYPE_GYROSCOPE_UNCALIBRATED;

    private static final String URL = "https://nas.splo2t.com";
    private static final int PORT = 9999;
    private OkHttpClient okHttpClient;

    private boolean permissionToUseAccepted = false;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private static final int RCODE = 10000;

    private static final int RECEIVING_TIME = 1000;

    private Recorder recorder;

    class Recorder {
        private AudioManager audioManager;
        private AudioRecord recorder;
        private final int SAMP_RATE = 48000;
        private int bufferShortSize = SAMP_RATE * (RECEIVING_TIME/1000);
        private short[] bufferRecord;
        private int bufferRecordSize;
        private ShortBuffer shortBuffer = ShortBuffer.allocate(SAMP_RATE);

        public Recorder() {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
            bufferRecordSize = AudioRecord.getMinBufferSize(SAMP_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT) * 2;
            bufferRecord = new short[bufferRecordSize];

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMP_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferRecord.length);

            Log.i("AUDIO_INFO", "SampleRate: " + audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
            Log.i("AUDIO_INFO", "Buffer Size: " + audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));

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

        private short[] getData(){
            return shortBuffer.array();
        }

        public String getDataAsCSV(){
            StringBuilder builder = new StringBuilder();
            int i = 0;
            for(short val : getData()){
                builder.append(i).append(",").append(val).append("\n");
                i++;
                if(i >= SAMP_RATE)
                    break;
            }
            shortBuffer.clear();
            return builder.toString();
        }

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

        recorder = new Recorder();

        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        okHttpClient = TrustOkHttpClientUtil.getUnsafeOkHttpClient().build();

        Thread netTh = new Thread(() -> {
            try {
                Log.i("NETWORK_TEST", "START");
//                ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//                        .tlsVersions(TlsVersion.TLS_1_3)
//                        .cipherSuites(
//                                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
//                                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
//                                CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
//                        .build();
//                SelfSigningHelper helper = SelfSigningHelper.getInstance();
//                OkHttpClient okHttpClient = helper.setSSLOkHttp(new OkHttpClient.Builder())
//                        .connectionSpecs(Arrays.asList(spec))
//                        .build();

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("id", "hi");

                RequestBody rBody = RequestBody.create(
                        jsonInput.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Log.i("NETWORK_TEST", "Request Build");

                Request request = new Request.Builder()
                        .post(rBody)
                        .url(URL + ":" + PORT)
                        .build();

                Log.i("NETWORK_TEST", "Wait for response");

                okHttpClient.newCall(request)
                        .enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                Log.i("NETWORK_TEST", "Network Failed\n");
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                try {
                                    Log.i("NETWORK_TEST", (new JSONObject(response.body().string())).getString("id"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                response.body().close();
                            }
                        });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        netTh.start();

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.setTime("");
        binding.setActivity(this);
//
//        myContext = this.getApplicationContext();

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
            recorder.onRecord(true);
            sensorManager.registerListener(accEvent, accSensor, accSensor.getMinDelay());
            sensorManager.registerListener(gyroEvent, gyroSensor, gyroSensor.getMinDelay());
            try {
                Thread.sleep(RECEIVING_TIME);
                recorder.onRecord(false);
                sensorManager.unregisterListener(accEvent);
                sensorManager.unregisterListener(gyroEvent);

                Log.i("ACC_INFO", String.format("%d_%d", accEvent.getDataSize()[0], accEvent.getDataSize()[1]));
                Log.i("GYRO_INFO", String.format("%d_%d", gyroEvent.getDataSize()[0], gyroEvent.getDataSize()[1]));

                JSONObject dataJson = new JSONObject();
                dataJson.put("acc", accEvent.getDataAsCSV());
                dataJson.put("gyro", gyroEvent.getDataAsCSV());
                dataJson.put("sound", recorder.getDataAsCSV());

//                RequestBody requestBody = new MultipartBody.Builder()
//                        .setType(MultipartBody.FORM)
////                        .addFormDataPart(
////                                "image"
////                                , audioName.substring(audioName.lastIndexOf("/"))
////                                , RequestBody.create(
////                                        new File(audioName)
////                                        , MultipartBody.FORM
////                                )
////                        )
//                        .addPart(RequestBody.create(
//                                imuJson.toString()
//                                ,MediaType.parse("application/json")
//                                )
//                        )
//                        .build();
                RequestBody requestBody = RequestBody.create(
                        dataJson.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .post(requestBody)
                        .url(URL + ":" + PORT)
                        .build();

                okHttpClient.newCall(request)
                        .enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                Log.i("NETWORK_TEST", "Network Failed\n");
                                e.printStackTrace();
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                Log.i("NETWORK_TEST", "File Transfer SUCCESS");
                                response.body().close();
                            }

                        });


            } catch (InterruptedException | JSONException e) {
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
        public String getDataAsCSV (){
            StringBuilder ret = new StringBuilder("#timestamp, x, y, z\n");
            long start = timestamps.peek();
            while(!timestamps.isEmpty()){
                ret.append(timestamps.poll() - start);

                for(float val : dataQueue.poll()){
                    ret.append(",").append(val);
                }
                ret.append("\n");
            }
            return ret.toString();
        }
    }

//    private static Context myContext = null;
//
//    public static Context getAppContext(){
//        return myContext;
//    }
}
//
//class SelfSigningHelper {
//    private SSLContext sslContext;
//    private TrustManagerFactory tmf;
//    private SelfSigningHelper() {
//        setUp();
//    }
//    // 싱글턴으로 생성
//    private static class SelfSigningClientBuilderHolder{
//        public static final SelfSigningHelper INSTANCE = new SelfSigningHelper();
//    }
//    public static SelfSigningHelper getInstance() {
//        return SelfSigningClientBuilderHolder.INSTANCE;
//    }
//    public void setUp() {
//        CertificateFactory cf;
//        Certificate ca;
//        InputStream caInput;
//        try {
//            cf = CertificateFactory.getInstance("X.509");
//// Application을 상속받는 클래스에
//// Context 호출하는 메서드 ( getAppContext() )를
//// 생성해 놓았음
//            caInput = MainActivity.getAppContext().getResources()
//                    .openRawResource(R.raw.fullchain);
//            ca = cf.generateCertificate(caInput);
//            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
//// Create a KeyStore containing our trusted CAs
//            String keyStoreType = KeyStore.getDefaultType();
//            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
//            keyStore.load(null,null);
//            keyStore.setCertificateEntry("ca", ca);
//// Create a TrustManager that trusts the CAs in our KeyStore
//            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
//            tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
//            tmf.init(keyStore);
//// Create an SSLContext that uses our TrustManager
//            sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
//            caInput.close();
//        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | KeyManagementException e) {
//            e.printStackTrace();
//        }
//    }
//    public OkHttpClient.Builder setSSLOkHttp(OkHttpClient.Builder builder){
//        builder.sslSocketFactory(getInstance().sslContext.getSocketFactory(),
//                (X509TrustManager)getInstance().tmf.getTrustManagers()[0]);
//        return builder;
//    }
//}

//class RetrofitFactory {
//    public static Retrofit createRetrofit(Context context,String baseUrl) {
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(baseUrl)
//                .client(createOkHttpClient())
//                .build();
//        return retrofit;
//    }
//    private static OkHttpClient createOkHttpClient() {
//        SelfSigningHelper helper = SelfSigningHelper.getInstance();
//        OkHttpClient.Builder builder = new OkHttpClient.Builder();
//        helper.setSSLOkHttp( builder);
//        return builder.build();
//    }
//}
