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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

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
    @SuppressLint("StaticFieldLeak")
    private static EditText ipJoinEditText;
    private static MediaProjectionManager mediaProjectionManager;
    private static MediaProjection mediaProjection;
    private static VirtualDisplay virtualDisplay;
    private static Intent mediaService;
    private static AudioRecord audioRecord;
    private static ImageReader imageReader;
    private static boolean sharing =false, clientWork, socketWork, work;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    @SuppressLint("StaticFieldLeak")
    private static Button serverButton, joinButton, shareButton,receiveButton;
    private DisplayMetrics metrics;
    private static Intent surfaceActivity;
    private static HandlerThread handlerThread;
    private static Handler h;
    private OutputStream videoOutputStream/*, audioOutputStream*/;
    @SuppressLint("StaticFieldLeak")
    private static Spinner spinner;
    private ArrayAdapter<String> spinAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaService.setMainIntent(getIntent());
        mediaService = new Intent(MainActivity.this, MediaService.class);
        metrics=getResources().getDisplayMetrics();
        setContentView(R.layout.activity_main);
        ipJoinEditText=findViewById(R.id.editJoinIp);
        joinButton = findViewById(R.id.joinButton);
        serverButton = findViewById(R.id.serverButton);
        shareButton=findViewById(R.id.sharingButton);
        receiveButton=findViewById(R.id.receivingButton);
        surfaceActivity = new Intent(this, SurfaceActivity.class);
        context = this;
        spinner = findViewById(R.id.spinner);
        spinAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                stopSharing();
                SocketTask.stop();
                Log.d(TAG,"spinner onItemSelected: " + parent.getSelectedItem());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

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
            if(sharing){
                Log.e(TAG, "STOP SHARING");
                stopSharing();
            }else {
                Log.e(TAG, "START SHARING");
                sharing=true;
                startScreenCapture();
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
                }else if (isValidIP(ipJoinString)) {
                    DeviceInfo.setIpJoin(ipJoinString);
                } else {
                    new AlertDialog.Builder(context).setTitle("Incorrect IP address").setMessage("IP address entered incorrectly, please check IP address.").create();
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
        spinner.setEnabled(enable);
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
            startForegroundService(mediaService);
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
    private Handler handler = new Handler(Looper.getMainLooper());
    private void initRecording() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Manifest permission record audio DENIED");
            finish();
        } else if (mediaProjection == null) {
            Log.e(TAG, "mediaProjection null");
        } else {
            videoOutputStream = SocketTask.getVideoOutputStream();
            //audioOutputStream = SocketTask.getAudioOutputStream();
            if (videoOutputStream == null /*&& audioOutputStream == null*/) {
                SocketTask.stop();
                stopSharing();
                return;
            }
            handlerThread = new HandlerThread("ImageHandlerThread");
            handlerThread.start();
            h = new Handler(handlerThread.getLooper());
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    if (imageReader != null) {
                        imageReader.close();
                        imageReader = null;
                    }
                    if (virtualDisplay != null) {
                        virtualDisplay.release();
                        virtualDisplay = null;
                    }
                }
                @Override
                public void onCapturedContentResize(int width, int height) {
                    super.onCapturedContentResize(width, height);
                }
                @Override
                public void onCapturedContentVisibilityChanged(boolean isVisible) {
                    super.onCapturedContentVisibilityChanged(isVisible);
                }
            }, h);
            imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 1);
            virtualDisplay = mediaProjection.createVirtualDisplay("screen", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), new VirtualDisplay.Callback() {
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
                    }, h);
            imageReader.setOnImageAvailableListener(reader -> {
                Log.d(TAG + " imageReader","Image available");
                Image image = reader.acquireLatestImage();
                if (image!=null) {
                    Log.d(TAG,"image width = " + (image.getWidth()+8) + "; height = " + image.getHeight() + ";\nmetrics width = " + metrics.widthPixels + "; height = " + metrics.heightPixels + ";");
                    Bitmap bitmap = Bitmap.createBitmap(image.getWidth()+8,image.getHeight(), Bitmap.Config.ARGB_8888);
                    Log.d(TAG,"math width =" + (metrics.widthPixels + (image.getPlanes()[0].getRowStride() - image.getPlanes()[0].getPixelStride() * metrics.widthPixels) / image.getPlanes()[0].getPixelStride()));
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
            }, h);
        }
    }
    public static void stopSharing() {
        Log.d(TAG,"stopSharing");
        sharing=false;
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
        h=null;
        if(handlerThread!=null) {
            handlerThread.quit();
            handlerThread=null;
        }
        context.stopService(mediaService);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
    }
    @Override
    protected void onStart() {
        super.onStart();
        work = true;
        new Thread(() -> {
            while (work) {
                try {
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = interfaces.nextElement();
                        if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                            while (addresses.hasMoreElements()) {
                                InetAddress address = addresses.nextElement();
                                if (address instanceof Inet4Address) {
                                    String addres = address.getHostAddress();
                                    if (addres != null) {
                                        handler.post(() -> {
                                            if (!spincontain(addres)) {
                                                spinAdapter.add(addres);
                                                if (DeviceInfo.getIp().isEmpty()) {
                                                    DeviceInfo.setIp(addres);
                                                }
                                            }
                                        });
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
    private boolean spincontain(String item){
        if(!item.isEmpty()){
            for(int i=0;i<spinAdapter.getCount();i++){
                String text = spinAdapter.getItem(i);
                if (text.contains(item)){
                    return true;
                }
            }
        }
        return false;
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
        System.exit(0);
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