package com.capstone.knockdatareceiver4watch.activity;

import static com.capstone.knockdatareceiver4watch.Constants.*;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.capstone.knockdatareceiver4watch.R;
import com.capstone.knockdatareceiver4watch.TrustOkHttpClientUtil;
import com.capstone.knockdatareceiver4watch.databinding.MainActivityBinding;
import com.capstone.knockdatareceiver4watch.listener.IMUListener;
import com.capstone.knockdatareceiver4watch.listener.IMUListenerFactory;
import com.capstone.knockdatareceiver4watch.listener.AudioListener;

import org.conscrypt.Conscrypt;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Security;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPOutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {

    private MainActivityBinding binding;

    private SensorManager sensorManager;
    private Sensor accSensor;
    private Sensor gyroSensor;
    private IMUListener accListener, gyroListener;
    private AudioListener audioListener;
    private OkHttpClient okHttpClient;

    private boolean permissionToUseAccepted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, permissions, RCODE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.i("PERMISSION_INFO", "" + this.getPackageManager().getBackgroundPermissionOptionLabel());
        }

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(TYPE_ACCEL);
        gyroSensor = sensorManager.getDefaultSensor(TYPE_GYRO);

        Log.i("SENSOR_INFO", "ACC_"+ accSensor.getVendor() + "_" + accSensor.getMinDelay());
        Log.i("SENSOR_INFO", "GYRO_"+ gyroSensor.getVendor() + "_" + gyroSensor.getMinDelay());

        accListener = IMUListenerFactory.getListener(accSensor.getType());
        gyroListener = IMUListenerFactory.getListener(gyroSensor.getType());

        audioListener = new AudioListener(getApplicationContext());

        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        okHttpClient = TrustOkHttpClientUtil.getUnsafeOkHttpClient().build();

        Thread netTh = new Thread(() -> {
            try {
                Log.i("NETWORK_TEST", "START");
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
                                Log.i("NETWORK_TEST", "CNT: "+okHttpClient.connectionPool().connectionCount());
                                okHttpClient.connectionPool().evictAll();
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                response.body().close();
                            }
                        });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        netTh.start();

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.tvStatus.setText("I");
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
        if(!permissionToUseAccepted){
//            if(this.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                }
//            }
//            else{

                finish();
//            }
        }

    }

    public void onButtonClick(){

        Thread getDataTh = new Thread(() -> {
            audioListener.onRecord(true);

            sensorManager.registerListener(accListener, accSensor, accSensor.getMinDelay());
            sensorManager.registerListener(gyroListener, gyroSensor, gyroSensor.getMinDelay());
            try {
                Thread.sleep((long)(RECEIVING_TIME));
                audioListener.onRecord(false);
                sensorManager.unregisterListener(accListener);
                sensorManager.unregisterListener(gyroListener);

                (MainActivity.this).runOnUiThread(() ->
//                        Toast.makeText(MainActivity.this.getApplicationContext(), time.toString(), Toast.LENGTH_SHORT).show()
                                binding.tvStatus.setText("W")
                );

                short[] soundData = audioListener.getData();
                Log.i("SOUND_INFO", String.format("%d", soundData.length));
                Log.i("ACC_INFO", String.format("%d", accListener.getDataSize()));
                Log.i("GYRO_INFO", String.format("%d", gyroListener.getDataSize()));

                JSONObject dataJson = new JSONObject();

                dataJson.put("label", binding.etLabel.getText());
                if(!binding.checkIsFake.isChecked()) {
                    dataJson.put("status", "real");
                    dataJson.put("acc", accListener.getDataAsCSV());
                    dataJson.put("gyro", gyroListener.getDataAsCSV());
                }
                else{
                    dataJson.put("status", "fake");
                }
                dataJson.put("sound", audioListener.getDataAsCSV());
//                Log.i("SOUND_DATA", dataJson.get("sound").toString().substring(0, 500));
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
                        compress(dataJson.toString() + "\n"),
                        MediaType.parse("application/json; charset=utf-8")
                );

//                Iterator<String> keys = dataJson.keys();
//                while(keys.hasNext()){
//                    Log.i("NETWORK_TEST", "KEYS: " + keys.next());
//                }

                Request request = new Request.Builder()
                        .post(requestBody)
                        .url(URL + ":" + PORT)
                        .build();

                long start = System.currentTimeMillis();

                float i = 0;
                while(i < 10000000){
                    i += 1.0;
                }

                long end = System.currentTimeMillis();

                Log.i("Latency_info", "computing latency: " + (end-start));

//                Response response = okHttpClient.newCall(request).execute();
//                end = System.currentTimeMillis();
//                MainActivity.this.runOnUiThread(()->binding.tvStatus.setText("S"));


                Call call = okHttpClient.newCall(request);

                final long cstart = System.currentTimeMillis();
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.i("NETWORK_TEST", "Network Failed\n");
                        MainActivity.this.runOnUiThread(()->binding.tvStatus.setText("F"));

                        e.printStackTrace();
                        Log.i("NETWORK_TEST", "CNT: "+okHttpClient.connectionPool().connectionCount());
                        okHttpClient.connectionPool().evictAll();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Log.i("NETWORK_TEST", "File Transfer SUCCESS");
                        MainActivity.this.runOnUiThread(()->binding.tvStatus.setText("S"));
//                                Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_SHORT).show();
                        response.body().close();
                        Log.i("Latency_info", "communication latency: " + (System.currentTimeMillis()-cstart));
                    }

                });


            } catch (InterruptedException | JSONException | IOException e) {
                e.printStackTrace();
            }
        });

        Timer timer = new Timer();
        TimerTask tt = new TimerTask() {
            int time = 3;
            @Override
            public void run() {
                (MainActivity.this).runOnUiThread(() ->
//                        Toast.makeText(MainActivity.this.getApplicationContext(), time.toString(), Toast.LENGTH_SHORT).show()
                        binding.tvStatus.setText(""+time)
                );
                if(time == 0) {
                    timer.cancel();
                    Log.i("THREAD_INFO", "TimerThread Canceled");
                    getDataTh.start();
                    (MainActivity.this).runOnUiThread(() ->
//                        Toast.makeText(MainActivity.this.getApplicationContext(), time.toString(), Toast.LENGTH_SHORT).show()
                        binding.tvStatus.setText("R")
                    );
                }
                time--;
            }
        };
        timer.scheduleAtFixedRate(tt, 0, 1000);
    }

    private String compress(String str) throws IOException {

        if (str == null || str.length() == 0) {
            return str;
        }

        long start = System.currentTimeMillis();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(out);
        gzip.write(str.getBytes());
        gzip.close();
        String outStr = out.toString("UTF-8");
        long end = System.currentTimeMillis();

        Log.i("Latency_info", "compress latency: " + (end-start));
        Log.i("Latency_info", "compress ratio: " + ((float)outStr.length() / (float)str.length()));

        return outStr;
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
