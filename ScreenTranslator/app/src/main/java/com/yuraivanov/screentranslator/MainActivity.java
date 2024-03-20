package com.yuraivanov.screentranslator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioRecord;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionConfig;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity{
    private static final int AUDIO_REQUEST_CODE = 103;
    private static final int RECORD_REQUEST_CODE = 101;
    private static final int PERMISSION_REQUEST_CODE = 102;
    private static final int NOTIFICATION_REQUEST_CODE = 104;
    private static final int NOTIFICATION_ID = 101;
    private static final String TAG="MainActivity";
    private final Handler handler = new Handler(Looper.getMainLooper());
    @SuppressLint("StaticFieldLeak")
    private static TextView ipText;
    @SuppressLint("StaticFieldLeak")
    private static EditText ipEditText, ipJoinEditText;
    private static MediaProjectionManager mediaProjectionManager;
    private static MediaProjection mediaProjection;
    private static VirtualDisplay virtualDisplay;
    private static Intent mediaService;
    private static AudioRecord audioRecord;
    private static ImageReader imageReader;
    private static boolean recording =false, clientWork, socketWork, work;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private boolean ipBol=true;
    @SuppressLint("StaticFieldLeak")
    private static Button serverButton, joinButton, shareButton,receiveButton;
    private DisplayMetrics metrics;
    private static Intent surfaceActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        metrics=getResources().getDisplayMetrics();
        setContentView(R.layout.activity_main);
        ipText=findViewById(R.id.thisIp);
        ipEditText=findViewById(R.id.editThisIp);
        ipJoinEditText=findViewById(R.id.editJoinIp);
        joinButton = findViewById(R.id.joinButton);
        serverButton = findViewById(R.id.serverButton);
        shareButton=findViewById(R.id.sharingButton);
        receiveButton=findViewById(R.id.receivingButton);
        surfaceActivity = new Intent(this, SurfaceActivity.class);
        context = this;
        ipText.setOnClickListener(view -> {
            if(!ipText.getText().toString().isEmpty()){
                if(!ipEditText.getText().toString().equals(ipText.getText().toString())){
                    insertIPDialog();
                }
            }
        });
        clientWork=false;
        socketWork=false;
        serverButton.setOnClickListener(view -> {
            Log.d(TAG,"serverButton click");
            if (DeviceInfo.getIp() == null|| DeviceInfo.getIp().isEmpty()) {
                new AlertDialog.Builder(context).setTitle("Empty input line").setMessage("The IP address entry line must not be empty, please enter IP address.").create();
            }else{
                if(clientWork){
                    SocketTask.stop();
                    enableUI(clientWork,true);
                }else{
                    SocketTask.startServer();
                    enableUI(clientWork,true);
                }
            }
        });
        receiveButton.setOnClickListener(view -> {
            Log.d(TAG,"receiveButton");
            if (SurfaceActivity.isReceiving()){
                SurfaceActivity.stopReceiving();
            }else{
                startActivity(surfaceActivity);
                SurfaceActivity.startReceiving();
            }
        });
        joinButton.setOnClickListener(view -> {
            Log.d(TAG,"joinButton click");
            if (DeviceInfo.getIpJoin() == null || DeviceInfo.getIpJoin().isEmpty()) {
                new AlertDialog.Builder(context).setTitle("Empty input line").setMessage("The IP address entry line must not be empty, please enter IP address.").create();
            } else {
                if(socketWork){
                    SocketTask.stop();
                    enableUI(socketWork,false);
                }else{
                    SocketTask.startSocket();
                    enableUI(socketWork,false);
                }
            }
        });
        shareButton.setOnClickListener(view -> {
            Log.d(TAG, "shareButton");
            if (shareButton.getVisibility() == View.VISIBLE) {
                if(recording){
                    Log.e(TAG, "STOP SHARING");
                    stopSharing();
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                    notificationManager.cancel(NOTIFICATION_ID);
                }else {
                    Log.e(TAG, "START SHARING");
                    recording=true;
                    startScreenCapture();
                }
            }
        });
        ipEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                ipBol=false;
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                ipBol=false;
            }
            @Override
            public void afterTextChanged(Editable editable) {
                String ipString=ipEditText.getText().toString();
                if(ipString.isEmpty()){
                    new AlertDialog.Builder(context)
                            .setTitle("Empty input line")
                            .setMessage("The IP address entry line must not be empty, please enter IP address.")
                            .create();
                }else if(!isValidIP(ipString)){
                    new AlertDialog.Builder(context).setTitle("Incorrect IP address").setMessage("IP address entered incorrectly, please check IP address.").create();
                }else{
                    DeviceInfo.setIp(ipString);
                }
                ipBol=true;
            }
        });
        ipJoinEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                String ipJoinString=ipJoinEditText.getText().toString();
                if(ipJoinString.isEmpty()){
                    new AlertDialog.Builder(context).setTitle("Empty input line").setMessage("The IP address entry line must not be empty, please enter IP address.").create();
                }else if(!isValidIP(ipJoinString)){
                    new AlertDialog.Builder(context).setTitle("Incorrect IP address").setMessage("IP address entered incorrectly, please check IP address.").create();
                }else{
                    DeviceInfo.setIpJoin(ipJoinString);
                }
                Log.d(TAG, "ipJoinString= " + ipJoinString);
            }
        });
        findViewById(R.id.settingsButton).setOnClickListener(view -> startActivity(new Intent(this,SettingsActivity.class)));
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, AUDIO_REQUEST_CODE);
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            Log.d(TAG,"Sdk int >=34");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION}, PERMISSION_REQUEST_CODE);
        }
    }
    public static boolean isClientWork() {
        return clientWork;
    }

    public static void setClientWork(boolean clientWork) {
        MainActivity.clientWork = clientWork;
    }

    public static boolean isSocketWork() {
        return socketWork;
    }

    public static void setSocketWork(boolean socketWork) {
        MainActivity.socketWork = socketWork;
    }

    public static boolean isWork() {
        return work;
    }
    public static void changeUi(boolean vision, boolean task) {
        Log.d(TAG,"changeUI vision " + vision + " task " + task);
        int visibility;
        if(vision) {
            visibility = View.VISIBLE;
            enableUI(false, task);
        }else{
            visibility = View.INVISIBLE;
            enableUI(true, task);
        }
        if(task) {
            receiveButton.setVisibility(visibility);
        }else{
            shareButton.setVisibility(visibility);
        }
    }
    private static void enableUI(boolean enable,boolean task){
        ipText.setEnabled(enable);
        ipEditText.setEnabled(enable);
        ipJoinEditText.setEnabled(enable);
        if(task){
            joinButton.setEnabled(enable);
            if (enable) serverButton.setText(R.string.start);
            else serverButton.setText(R.string.stop);
        }else{
            serverButton.setEnabled(enable);
            if(enable) joinButton.setText(R.string.connect);
            else joinButton.setText(R.string.disconnect);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            if (requestCode == AUDIO_REQUEST_CODE) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
            }
            if (requestCode == PERMISSION_REQUEST_CODE) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
            }
            if (requestCode == NOTIFICATION_REQUEST_CODE) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                }
            }
        }
    }
    private void startScreenCapture(){
        Log.d(TAG,"start screen capture");
        if(mediaProjection==null){
            mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent screenCaptureIntent;
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
                screenCaptureIntent=mediaProjectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay());
            }else screenCaptureIntent=mediaProjectionManager.createScreenCaptureIntent();
            if(mediaService==null) {
                mediaService = new Intent(MainActivity.this, MediaService.class);
                MediaService.setMainIntent(getIntent());
                startForegroundService(mediaService);
            }
            startActivityForResult(screenCaptureIntent,RECORD_REQUEST_CODE);
        }else initRecording();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RECORD_REQUEST_CODE){
            if(resultCode == RESULT_OK) {
                Log.d(TAG, "Manifest permission media record GRANTED");
                if (data != null) {
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                    startScreenCapture();
                }else Log.e(TAG,"onActivityResult data null");
            }
        }
    }
    private void initRecording(){
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Manifest permission record audio DENIED");
            finish();
        }else if(mediaProjection==null) {
            Log.e(TAG,"mediaProjection null");
        }else{
            OutputStream videoOutputStream = SocketTask.getVideoOutputStream();
            OutputStream audioOutputStream = SocketTask.getAudioOutputStream();
            if (videoOutputStream == null && audioOutputStream == null) {
                SocketTask.stop();
                stopSharing();
                return;
            }
            HandlerThread handlerThread = new HandlerThread("ImageHandlerThread");
            handlerThread.start();
            Handler h = new Handler(handlerThread.getLooper());
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    if(virtualDisplay!=null) {
                        virtualDisplay.release();
                        virtualDisplay = null;
                    }
                    if(imageReader!=null){
                        imageReader.close();
                        imageReader=null;
                    }
                }
                @Override
                public void onCapturedContentResize(int width, int height) {
                    super.onCapturedContentResize(width, height);
                    if(virtualDisplay!=null){
                        virtualDisplay.resize(width,height,metrics.densityDpi);
                    }
                }
                @Override
                public void onCapturedContentVisibilityChanged(boolean isVisible) {
                    super.onCapturedContentVisibilityChanged(isVisible);
                }
            },h);
            imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 1);
            VirtualDisplay.Callback vdCallback = new VirtualDisplay.Callback() {
                @Override
                public void onPaused() {
                    super.onPaused();
                }
                @Override
                public void onResumed() {
                    super.onResumed();
                }
                @Override
                public void onStopped() {
                    super.onStopped();
                    if(virtualDisplay!=null) {
                        virtualDisplay.release();
                        virtualDisplay = null;
                    }
                    if(imageReader!=null){
                        imageReader.close();
                        imageReader=null;
                    }
                }
            };
            virtualDisplay = mediaProjection.createVirtualDisplay("screen", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), vdCallback, h);
            imageReader.setOnImageAvailableListener(reader -> {
                Log.d(TAG + " imageReader","Image available");
                Image image = reader.acquireLatestImage();
                if (image!=null) {
                    Bitmap bitmap = Bitmap.createBitmap((metrics.widthPixels + (image.getPlanes()[0].getRowStride() - image.getPlanes()[0].getPixelStride() * metrics.widthPixels) / image.getPlanes()[0].getPixelStride()),
                            metrics.heightPixels, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] imageBytes = stream.toByteArray();
                    ObjectOutputStream bos;
                    try {
                        bos = new ObjectOutputStream(videoOutputStream);
                    } catch (IOException e) {
                        Log.e(TAG, "new ObjectOutputStream error " + e.getMessage());
                        stopSharing();
                        SocketTask.stop();
                        return;
                    }
                    try {
                        bos.writeObject(imageBytes);
                        bos.flush();
                    } catch (IOException e) {
                        Log.e(TAG, "reader bos error " + e.getMessage());
                    }
                    image.close();
                }
            },h);
            /*audioOutputStream = GetSocketService.getAudioOutputStream();
            if (audioOutputStream != null) {
                /*int bufferSize = AudioRecord.getMinBufferSize(44100,
                        AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                audioRecord.startRecording();
                audioThread = executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        while (recording) {
                            /*byte[] audioBuffer = new byte[bufferSize];
                            int read = audioRecord.read(audioBuffer, 0, bufferSize);
                            try {
                                audioOutputStream.write(audioBuffer, 0, read);
                                audioOutputStream.flush();
                            } catch (IOException e) {
                                Log.e(TAG, "Sharing audio error " + e.getMessage());
                                stopSharing();
                            }
                        }
                    }
                });
            }*/
        }
    }
    public static void stopSharing() {
        Log.d(TAG,"stopSharing");
        recording=false;
        if(virtualDisplay!=null){
            virtualDisplay.release();
            virtualDisplay=null;
        }
        if(imageReader!=null){
            imageReader.close();
            imageReader=null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if(mediaProjectionManager!=null){
            mediaProjectionManager=null;
        }
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if(mediaService!=null) {
            context.stopService(mediaService);
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    protected void onStart() {
        super.onStart();
        work=true;
        new Thread(() -> {
            while(work) {
                try {
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = interfaces.nextElement();
                        if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                            while (addresses.hasMoreElements()) {
                                InetAddress address = addresses.nextElement();
                                if (address instanceof Inet4Address) {
                                    if(!ipText.getText().equals(address.getHostAddress())){
                                        runOnUiThread(() -> ipText.setText(address.getHostAddress()));
                                    }
                                    if(ipEditText.getText()==null || ipEditText.getText().toString().isEmpty() && ipBol && !ipEditText.getText().toString().equals(address.getHostAddress())) {
                                        runOnUiThread(() -> ipEditText.setText(address.getHostAddress()));
                                    }
                                }
                            }
                        }
                    }
                } catch (SocketException e) {
                    Log.e(TAG, "getIp error" + e.getMessage());
                    break;
                }
            }
        }).start();
    }
    private void insertIPDialog() {
        Log.d(TAG, "insertIPDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Replace IP")
                .setMessage("Replace the IP address input line with your IP address?")
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    ipEditText.setText(ipText.getText().toString());
                    DeviceInfo.setIp(ipEditText.getText().toString());
                })
                .setNegativeButton("Cansel", null);
        builder.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        work=false;
        SocketTask.stop();
        stopSharing();
        SurfaceActivity.stopReceiving();
    }
    private boolean isValidIP(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        String[] groups = ipAddress.split("\\.");
        if (groups.length != 4) {
            return false;
        }
        for (String group : groups) {
            try {
                int value = Integer.parseInt(group);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return !ipAddress.endsWith(".");
    }
}