package com.yurijivanov.screentranslatorpc;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReceivingController {
    private static final String Tag = "ReceivingController";
    private static ReceivingController instance;
    private boolean initialize=false;
    @FXML
    private ImageView imageView;
    @FXML
    private VBox receiveView;
    private static boolean receiving;
    private InputStream videoInput;
    private void setImage(Image image) {
        imageView.setImage(image);
        image.cancel();
    }
    public static ReceivingController getInstance(){
        return instance;
    }
    public static void setInstance(ReceivingController instance) {
        ReceivingController.instance = instance;
    }
    @FXML
    private void goMain(){
        stopReceiving();
        UI.showMainScene();
    }
    public void initialize(){
        initialize=true;
        receiveView.widthProperty().addListener((observable, oldValue, newValue) -> imageView.setFitWidth(newValue.doubleValue()-20));
        receiveView.heightProperty().addListener((observable, oldValue, newValue) -> imageView.setFitHeight(newValue.doubleValue()-35));
    }
    public void startReceiving(){
        System.out.println(Tag + " startReceiving");
        if(!instance.initialize){
            return;
        }
        receiving=true;
        videoInput = SocketTask.getVideoInput();
        new Thread(() -> {
            if (videoInput == null) {
                SocketTask.stop();
                Platform.runLater(ReceivingController::stopReceiving);
            } else {
                while (receiving && MainApplication.isWork()) {
                    if(!MainApplication.isClientWork()){
                        Platform.runLater(ReceivingController::stopReceiving);
                        return;
                    }
                    readObj();
                }
            }
            System.out.println(Tag + " receiving thread stop");
        }).start();
    }
    private void readObj(){
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(videoInput);
        } catch (IOException e) {
            System.out.println(Tag + " readObj new ObjectInputStream " + e.getMessage());
            Platform.runLater(ReceivingController::stopReceiving);
            return;
        }
        Object obj = null;
        try {
            obj = ois.readObject();
        } catch (IOException e) {
            System.out.println(Tag + " readObj ois.readObject IOException " + e.getMessage());
            if(e.getMessage().contains("Socket closed")||e.getMessage().contains("Broken pipe")){
                Platform.runLater(ReceivingController::stopReceiving);
                return;
            }
        } catch (ClassNotFoundException e) {
            System.out.println(Tag + " readObj ois.readObject ClassNotFoundException " + e.getMessage());
        }
        if(obj!=null){
            byte[] imageBytes = (byte[]) obj;
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            Image image = new Image(bais);
            Platform.runLater(() -> setImage(image));
            try {
                bais.close();
            } catch (IOException e) {
                System.out.println(Tag + " readObj bais.close " + e.getMessage());
            }
        }
    }
    public static void stopReceiving() {
        System.out.println(Tag + " stopReceiving");
        receiving = false;
        if (MainApplication.isWork()) UI.showMainScene();
    }

    public static boolean isReceiving() {
        return receiving;
    }
}
