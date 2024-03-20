package com.yurijivanov.screentranslatorpc;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.stage.Screen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Enumeration;

public class MainController {
    public static MainController getInstance(){
        return instance;
    }
    private static MainController instance;
    @FXML
    private ChoiceBox<String> ip, screens;
    @FXML
    private Button receiveButton, shareButton;
    @FXML
    private TextField join_ip_tf;
    @FXML
    private Button serverButton, joinButton;
    private static final String Tag="MainController";
    public static void setInstance(MainController instance) {
        MainController.instance = instance;
    }

    @FXML
    private void goOptions() {
        UI.showSettingsScene();
    }

    public void initialize(){
        System.out.println(Tag + " initialize");
        Alert alert=new Alert(Alert.AlertType.INFORMATION);
        join_ip_tf.textProperty().addListener(((observable, oldValue, newValue) -> {
            alert.setHeaderText("IP join device");
            if(newValue.isEmpty()){
                alert.setContentText("Field is empty");
                alert.show();
            }else if(!oldValue.equals(newValue)){
                if(MainApplication.isSocketWork()) {
                    SocketTask.stop();
                }
                if(!isInValidIP(newValue)){
                    alert.setContentText("IP address entered incorrectly, please check IP address.");
                }else{

                    DeviceInfo.setIpJoin(newValue);
                }
            }
        }));
        serverButton.setOnAction(event -> {
            System.out.println(Tag + " serverButton event");
            if(MainApplication.isClientWork()){
                SocketTask.stop();
                joinButton.setDisable(false);
                ip.setDisable(false);
                join_ip_tf.setDisable(false);
            }else{
                SocketTask.startServer();
                joinButton.setDisable(true);
                ip.setDisable(true);
                join_ip_tf.setDisable(true);
            }
        });
        receiveButton.setOnAction(event -> {
            System.out.println(Tag+" receive action");
            UI.showVideoScene();
            if(ReceivingController.isReceiving()){
                ReceivingController.stopReceiving();
            }else{
                ReceivingController intense = ReceivingController.getInstance();
                if(intense!=null) {
                    intense.startReceiving();
                }else System.out.println(Tag + " ReceivingController intense is null");
            }
        });
        joinButton.setOnAction(event -> {
            System.out.println(Tag + " joinButton event");
            if(DeviceInfo.getIpJoin().isEmpty()){
                System.out.println(Tag + " joinButton event DeviceInfo.getIpJoin().isEmpty");
            }else {
                if (MainApplication.isSocketWork()) {
                    SocketTask.stop();
                    serverButton.setDisable(false);
                    ip.setDisable(false);
                } else {
                    SocketTask.startSocket();
                    serverButton.setDisable(true);
                    ip.setDisable(true);
                }
            }
        });
        shareButton.setOnAction(event -> {
            System.out.println(Tag+" share action");
            if(MainApplication.isSharing()){
                MainApplication.stopSharing();
            }else {
                MainApplication.startSharing(screens.getSelectionModel().getSelectedIndex());
            }
        });
        getLocalIpAddress();
        ip.getSelectionModel().select(0);
        DeviceInfo.setIp(ip.getValue());
        ip.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(!oldValue.equals(newValue)) {
                SocketTask.stop();
                DeviceInfo.setIp(newValue);
            }
        });
        setScreens();
        screens.getSelectionModel().selectFirst();
    }

    private void setScreens(){
        for (Screen screen : Screen.getScreens()) {
            screens.getItems().add("Screen " + (Screen.getScreens().indexOf(screen) + 1));
        }
    }
    private void getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements() && MainApplication.isWork()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual())
                    continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements() && MainApplication.isWork()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        ip.getItems().add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
    private boolean isInValidIP(String ipAddress) {
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
    public void changeUi(boolean vision, boolean task) {
        System.out.println(Tag + " changeUI vision " + vision + " task " + task);
        if(task) {
            if (receiveButton != null) {
                receiveButton.setVisible(vision);
            }
        }else{
            if(shareButton!=null){
                shareButton.setVisible(vision);
                screens.setVisible(vision);
            }
        }
    }
    /*static OutputStream videoOutput;
    static ServerSocket serverSocket;
    static Socket client;
    private static boolean sharing;
    @FXML
    public void test() {
        //videoInput=SocketTask.getVideoInput();
        if(tryServer()){
            System.out.println(Tag + " test tryServer true ");
            try {
                videoOutput=client.getOutputStream();
            } catch (IOException e) {
                System.out.println(Tag + " test client.getInputStream " + e.getMessage());
            }
            if(videoOutput!=null){
                //startReceiving();
                startSharing();
            }else stopServer();
        }else System.out.println(Tag + " test tryServer false ");
    }

    private boolean tryServer(){
        System.out.println(Tag + " tryServer");
        InetAddress localhost=null;
        try {
            localhost = InetAddress.getByName(DeviceInfo.getIp());
        } catch (UnknownHostException e) {
            System.out.println(Tag + " tryServer InetAddress.getByName " + e.getMessage());
            return false;
        }
        if(localhost!=null){
            System.out.println(Tag + " localhost not null");
            try {
                serverSocket=new ServerSocket(DeviceInfo.getVideoPort(),0,localhost);
            } catch (IOException e) {
                System.out.println(Tag + " tryServer create ServerSocket " + e.getMessage());
            }
            if (serverSocket != null) {
                Thread t = new Thread(()->{
                    while (client==null && MainApplication.isWork()) {
                        try {
                            client = serverSocket.accept();
                        } catch (IOException e) {
                            System.out.println(Tag + " tryServer serverSocket.accept " + e.getMessage());
                        }
                        System.out.println(Tag + " tryServer client " + client);
                    }
                });
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    System.out.println(Tag + "tryServer t.join " + e.getMessage());
                    return false;
                }
                if(client!=null){
                    return client.isConnected();
                }
            }
        }
        return false;
    }
    public static void stopServer(){
        System.out.println(Tag + " stopServer");
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                System.out.println(Tag + " stopServer client.close " + e.getMessage());
            }
        }
        if(serverSocket!=null){
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println(Tag + " stopServer serverSocket.close " + e.getMessage());
            }
        }
        stopSharing();
        //stopReceiving();
    }
    public static void startSharing(){
        sharing=true;
        new Thread(()->{
            Robot robot;
            try {
                robot = new Robot();
            }  catch (AWTException e) {
                System.out.println(Tag + " startSharing AWTException " + e.getMessage());
                return;
            }
            ObjectOutputStream outstream=null;
            while(sharing){
                try{
                    outstream = new ObjectOutputStream(videoOutput);
                } catch (IOException e) {
                    System.out.println(Tag + " startSharing new ObjectOutputStream " + e.getMessage());
                    stopSharing();
                    break;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Rectangle rectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                BufferedImage img =  robot.createScreenCapture(rectangle);
                try {
                    ImageIO.write(img, "jpg", baos);
                } catch (IOException e) {
                    System.out.println(Tag + " startSharing ImageIO.write " + e.getMessage());
                    stopSharing();
                    break;
                }
                byte[] imageData = baos.toByteArray();
                try {
                    outstream.writeObject(imageData);
                    outstream.flush();
                } catch (IOException e) {
                    System.out.println(Tag + " startSharing outstream.writeObject/flush " + e.getMessage());
                    if(e.getMessage().contains("Socket closed")||e.getMessage().contains("Broken pipe")){
                        stopSharing();
                        break;
                    }
                }
            }
            if(outstream!=null) {
                try {
                    outstream.close();
                } catch (IOException e) {
                    System.out.println(Tag + " startSharing outstream.close " + e.getMessage());
                }
            }
        }).start();
    }
    public static void stopSharing(){
        sharing=false;
    }*/
}