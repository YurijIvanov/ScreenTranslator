package com.yuraivanov.screentranslator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GetDialog extends DialogFragment {
    private ProgressBar progressBar;
    private Button agreeButton, cancelButton;
    private TextView textView;
    private ImageView imageView;
    private Activity activity;
    private Intent service;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d("GetDialog", "OnCreate");
        activity = requireActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        View view = inflater.inflate(R.layout.connection_dialog, null);
        progressBar= view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        agreeButton = view.findViewById(R.id.agree);
        agreeButton.setVisibility(View.INVISIBLE);
        cancelButton = view.findViewById(R.id.cansel);
        textView= view.findViewById(R.id.ip);
        imageView= view.findViewById(R.id.imageView);
        imageView.setVisibility(View.INVISIBLE);

        if(GetClientService.isReceiving) {
            GetClientService.stopServer();
        }
        service = new Intent(getActivity(),GetClientService.class);

        getActivity().startService(service);

        agreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(agreeButton.getVisibility() == View.VISIBLE){
                    startActivity(new Intent(requireContext(),SurfaceActivity.class));
                }
            }
        });

        cancelButton.setOnClickListener(view1 -> {
            if(service!=null){
                getActivity().stopService(service);
            }
            dismiss();
        });
        builder.setView(view)
                .setTitle("Get");
        return builder.create();
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("GetDialog", "onDestroy");
        GetClientService.stopServer();
    }

    public Object[] getUI(){
        if(progressBar==null && imageView==null && agreeButton==null){
            return null;
        }
        return new Object[]{activity,progressBar,imageView,agreeButton};
    }
}
