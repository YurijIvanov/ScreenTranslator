package com.yurijivanov.screentranslatorpc;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

public class SettingsController{
    @FXML
    private TextField command_port_tf;
    @FXML
    private TextField video_port_tf;
    @FXML
    private TextField audio_port_tf;
    private final String Tag = "SettingsController";

    @FXML
    private void goMain(){
        UI.showMainScene();
    }

    public void initialize(){
        System.out.println(Tag + " initialize");
        Alert alert=new Alert(Alert.AlertType.INFORMATION);
        command_port_tf.textProperty().addListener(((observable, oldValue, newValue) -> {
            alert.setHeaderText("Command port");
            if(newValue.isEmpty()){
                alert.setContentText("Field is empty");
                alert.show();
            }else if(!oldValue.equals(newValue)){
                if(MainApplication.isClientWork()) {
                    SocketTask.stop();
                }
                if(MainApplication.isSocketWork()){
                    SocketTask.stop();
                }
                try {
                    int value = Integer.parseInt(newValue);
                    if(value<1023 || value>49151) {
                        alert.setContentText("The port value must be between 1024 and 49151");
                        alert.show();
                    }else{
                        DeviceInfo.setCommandPort(value);
                    }
                }catch (NumberFormatException e){
                    alert.setHeaderText("Command port");
                    alert.setContentText("Field is contain not number");
                    alert.show();
                }
            }
        }));
        video_port_tf.textProperty().addListener(((observable, oldValue, newValue) -> {
            alert.setHeaderText("Video port");
            if(newValue.isEmpty()){
                alert.setContentText("Field is empty");
                alert.show();
            }else if(!oldValue.equals(newValue)){
                if(MainApplication.isClientWork()) {
                    SocketTask.stop();
                }
                if(MainApplication.isSocketWork()){
                    SocketTask.stop();
                }
                try {
                    int value = Integer.parseInt(newValue);
                    if(value<1023 || value>49151) {
                        alert.setContentText("The port value must be between 1024 and 49151");
                        alert.show();
                    }else{
                        DeviceInfo.setVideoPort(value);
                    }
                }catch (NumberFormatException e){
                    alert.setHeaderText("Video port");
                    alert.setContentText("Field is contain not number");
                    alert.show();
                }
            }
        }));
        audio_port_tf.textProperty().addListener(((observable, oldValue, newValue) -> {
            alert.setHeaderText("Audio port");
            if(newValue.isEmpty()){
                alert.setContentText("Field is empty");
                alert.show();
            }else if(!oldValue.equals(newValue)){
                if(MainApplication.isClientWork()) {
                    SocketTask.stop();
                }
                if(MainApplication.isSocketWork()){
                    SocketTask.stop();
                }
                try {
                    int value = Integer.parseInt(newValue);
                    if(value<1023 || value>49151) {
                        alert.setContentText("The port value must be between 1024 and 49151");
                        alert.show();
                    }else{
                        DeviceInfo.setAudioPort(value);
                    }
                }catch (NumberFormatException e){
                    alert.setHeaderText("Audio port");
                    alert.setContentText("Field is contain not number");
                    alert.show();
                }
            }
        }));
    }
}
