package com.yuraivanov.screentranslator;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class GiveDialog extends DialogFragment {


    //private ExecutorService executor;
    private Button canselButton;
    //private Handler timer;
    private View view;
    private ImageView imageView;
    private ProgressBar progressBar;
    private Button agreeButton;
    private Thread videoThread, audioThread;
    private DisplayMetrics metrics;
    private static final int RECORD_REQUEST_CODE = 101;
    private static OutputStream videoOutputStream, audioOutputStream;
    private static MediaProjectionManager mediaProjectionManager;
    private static MediaProjection mediaProjection;
    private static VirtualDisplay virtualDisplay;
    private static AudioRecord audioRecord;
    private static boolean isRecording;
    private Activity activity;
    private final String className ="GiveDialog";
    private Intent service;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d(className, "OnCreate");
        if(GetSocketService.isRecording){
            Log.e("MainActivity","GetSocketService don`t started");
            dismiss();
        }
        //metrics = DeviceInfo.getMetrics();
        activity=requireActivity();
        //timer =new Handler(Looper.getMainLooper());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = activity.getLayoutInflater();
        view = inflater.inflate(R.layout.connection_dialog, null);
        imageView = view.findViewById(R.id.imageView);
        progressBar = view.findViewById(R.id.progressBar);
        agreeButton = view.findViewById(R.id.agree);
        agreeButton.setVisibility(View.INVISIBLE);
        imageView.findViewById(R.id.imageView).setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        canselButton = view.findViewById(R.id.cansel);

        if(GetSocketService.isRecording) {
            GetSocketService.stopSocket();
        }
        service = new Intent(getActivity(),GetSocketService.class);
        GetSocketService.setObjects(getUI());
        getActivity().startService(service);

        canselButton.setOnClickListener(view1 -> {
            getActivity().stopService(service);
            stopRecording();
            dismiss();
        });
        agreeButton.setOnClickListener(view1 -> {
            if (agreeButton.getVisibility() == View.VISIBLE) {
                //Notification.showNotification(getContext());
                //Notification.showNotification(getContext());
                Log.e(className,"START SHARING");
                /*
                mediaProjectionManager = (MediaProjectionManager) requireContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), RECORD_REQUEST_CODE);*/
            }
        });
        builder.setView(view)
                .setTitle("Give");
        return builder.create();
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    public Object[] getUI(){
        if(progressBar==null && imageView==null && agreeButton==null){
            return null;
        }
        return new Object[]{activity,progressBar,imageView,agreeButton};
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.e(className,"Manifest permission record audio DENIED");
                dismiss();
            }
            videoOutputStream = GetSocketService.getVideoOutputStream();
            if (videoOutputStream == null) {
                dismiss();
            }else{
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                mediaProjection.registerCallback(new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        super.onStop();
                        stopRecording();
                    }
                }, null);
                isRecording = true;
                videoThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRecording) {
                            Bitmap bitmap = getBitmapFromScreen();
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                            byte[] byteArray = byteArrayOutputStream.toByteArray();
                            try {
                                videoOutputStream.write(byteArray);
                                videoOutputStream.flush();
                            } catch (IOException e) {
                                Log.e(className, "Sharing screen error " + e.getMessage());
                                break;
                            }
                        }
                    }
                });
            }
            audioOutputStream = GetSocketService.getAudioOutputStream();
            if(audioOutputStream==null){
                dismiss();
            }else {
                int bufferSize = AudioRecord.getMinBufferSize(44100,
                        AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize*10);
                isRecording = true;
                audioRecord.startRecording();
                audioThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRecording) {
                            byte[] audioBuffer = new byte[bufferSize];
                            int read = audioRecord.read(audioBuffer, 0, bufferSize);
                            try {
                                audioOutputStream.write(audioBuffer, 0, read);
                                audioOutputStream.flush();
                            } catch (IOException e) {
                                Log.e(className, "Sharing audio error " + e.getMessage());
                                break;
                            }
                        }
                    }
                });
                audioThread.start();
            }
        }
    }
    public static void stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (videoOutputStream != null) {
            try {
                videoOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            videoOutputStream = null;
        }
        if (audioOutputStream != null) {
            try {
                audioOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            audioOutputStream = null;
        }
        if(mediaProjectionManager!=null){
            mediaProjectionManager=null;
        }
    }
    private Bitmap getBitmapFromScreen(){
        ImageReader imageReader = ImageReader.newInstance(metrics.widthPixels,metrics.heightPixels, PixelFormat.RGBA_8888, 1);
        mediaProjection.createVirtualDisplay("screen",metrics.widthPixels,metrics.heightPixels,metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                imageReader.getSurface(), null, null);
        Image image = imageReader.acquireLatestImage();
        Bitmap bitmap = Bitmap.createBitmap(metrics.widthPixels,metrics.heightPixels, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(image.getPlanes()[0].getBuffer());
        image.close();
        imageReader.close();
        return bitmap;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(className, "onDestroy");
        /*if(executor!=null) {
            executor.shutdown();
        }*/
        GetSocketService.stopSocket();
        stopRecording();
        /*if(timer!=null){
            timer.removeCallbacks(null);
        }*/
        try {
            if(videoThread!=null) {
                videoThread.join();
                videoThread=null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if(audioThread!=null) {
                audioThread.join();
                audioThread=null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
