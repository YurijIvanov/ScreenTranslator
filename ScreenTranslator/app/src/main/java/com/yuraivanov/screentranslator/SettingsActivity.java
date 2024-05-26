package com.yuraivanov.screentranslator;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Context context = this;
        EditText commandPort, videoPort, audioPort, commandPortJoin, videoPortJoin, audioPortJoin;
        commandPort= findViewById(R.id.editThisCommandPort);
        videoPort = findViewById(R.id.editThisVideoPort);
        audioPort = findViewById(R.id.editThisAudioPort);
        /*commandPortJoin=findViewById(R.id.editJoinCommandPort);
        videoPortJoin = findViewById(R.id.editJoinVideoPort);
        audioPortJoin = findViewById(R.id.editJoinAudioPort);*/
        commandPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(commandPort.getText().toString().isEmpty()){
                    new AlertDialog.Builder(context).setTitle("Empty input line").setMessage("The Port entry line must not be empty.").create();
                }else{
                    try {
                        int port = Integer.parseInt(commandPort.getText().toString());
                        if(port<1023 || port>49151){
                            new AlertDialog.Builder(context).setTitle("Wrong port").setMessage("The port value must be between 1024 and 49151.").create();
                        }else{
                            DeviceInfo.setCommandPort(port);
                        }
                    }
                    catch (NumberFormatException e){
                        new AlertDialog.Builder(context).setTitle("Input line is not a numeric value").setMessage("The Port entry line must have numeric value.").create();
                    }
                }
            }
        });
        videoPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(videoPort.getText().toString().isEmpty()){
                    new AlertDialog.Builder(context).setTitle("Empty input line").setMessage("The Port entry line must not be empty.").create();
                }else{
                    try {
                        int port = Integer.parseInt(videoPort.getText().toString());
                        if(port<1023 || port>49151){
                            new AlertDialog.Builder(context).setTitle("Wrong port").setMessage("The port value must be between 1024 and 49151.").create();
                        }else{
                            DeviceInfo.setVideoPort(port);
                        }
                    }
                    catch (NumberFormatException e){
                        new AlertDialog.Builder(context).setTitle("Input line is not a numeric value").setMessage("The Port entry line must have numeric value.").create();
                    }
                }
            }
        });

        audioPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(audioPort.getText().toString().isEmpty()){
                    new AlertDialog.Builder(context).setTitle("Empty input line").setMessage("The Port entry line must not be empty.").create();
                }else{
                    try {
                        int port = Integer.parseInt(audioPort.getText().toString());
                        if(port<1023 || port>49151){
                            new AlertDialog.Builder(context).setTitle("Wrong port").setMessage("The port value must be between 1024 and 49151.").create();
                        }else{
                            DeviceInfo.setAudioPort(port);
                        }
                    }
                    catch (NumberFormatException e){
                        new AlertDialog.Builder(context).setTitle("Input line is not a numeric value").setMessage("The Port entry line must have numeric value.").create();
                    }
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}