package com.yurijivanov.screentranslatorpc;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketTask {
    private static final String Tag = "SocketTask";
    private static ServerSocket videoServer, /*audioServer,*/ commandServer;
    private static Socket videoSocket, /*audioSocket,*/ commandSocket;
    private static boolean stop, task, agree;

    private static ServerSocket createServer(int port){
        try {
            return new ServerSocket(port, 1, InetAddress.getByName(DeviceInfo.getIp()));
        } catch (IOException e) {
            System.out.println(Tag + " commandServer create " + e.getMessage());
        }
        return null;
    }
    private static Socket createClient(ServerSocket serverSocket){
        try {
            return serverSocket.accept();
        } catch (IOException ignore) {
            //System.out.println(Tag + " "socket connection error " + e.getMessage());
        }
        return null;
    }
    private static Socket createSocket(int port){
        try {
            return new Socket(DeviceInfo.getIpJoin(),port);
        } catch (IOException ignore) {
        }
        return null;
    }
    public static void startServer(){
        System.out.println(Tag + " startServer");
        task=true;
        stop=true;
        agree=false;
        MainApplication.setClientWork(true);
        commandServer = createServer(DeviceInfo.getCommandPort());
        videoServer = createServer(DeviceInfo.getVideoPort());
        //audioServer = createServer(DeviceInfo.getAudioPort());
        if (commandServer == null || videoServer == null /*|| audioServer == null*/) {
            stop();
        } else {
            new Thread(()->{
                System.out.println(Tag + " startServer commandSocket thread start");
                while(MainApplication.isWork()&&MainApplication.isClientWork()&&commandSocket==null){
                    commandSocket = createClient(commandServer);
                    if (commandSocket != null) {
                        System.out.println(Tag + " startServer commandSocket created");
                        MainController intense = MainController.getInstance();
                        if (intense != null) {
                            Platform.runLater(() -> intense.setJoin_ip_tfText(commandSocket.getInetAddress().getHostAddress()));
                        }
                        checkDialog(()-> agree=true, SocketTask::stop);
                        //passPassword();
                    }
                }
                System.out.println(Tag + " startServer commandSocket thread end");
            }).start();
            new Thread(()->{
                System.out.println(Tag + " startServer videoSocket thread start");
                while(MainApplication.isWork()&&MainApplication.isClientWork()&&videoSocket==null){
                    if(agree)
                        videoSocket = createClient(videoServer);
                    if (videoSocket != null) {
                        System.out.println(Tag + " startServer videoSocket created");
                        changeUi(true);
                    }
                }
                System.out.println(Tag + " startServer videoSocket thread end");
            }).start();
            /*new Thread(()->{
                System.out.println(Tag + " startServer audioSocket thread start");
                while(MainApplication.isWork()&&MainApplication.isClientWork()&&audioSocket==null){
                    audioSocket = createClient(audioServer);
                    if (audioSocket != null) {
                        System.out.println(Tag + " startServer audioSocket created");
                    }
                    changeUi(true);
                }
                System.out.println(Tag + " startServer audioSocket thread end");
            }).start();*/
            checkStop();
        }
    }
    private static void checkDialog(Runnable yesAction, Runnable noAction){
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Server connect");
            alert.setHeaderText(null);
            alert.setContentText("Connected ip: " + commandSocket.getInetAddress().getHostAddress());
            ButtonType buttonYes = new ButtonType("Да");
            ButtonType buttonNo = new ButtonType("Нет");
            alert.getButtonTypes().setAll(buttonYes, buttonNo);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == buttonYes) {
                yesAction.run();
            } else {
                noAction.run();
            }
        });
    }
    /*private static boolean checkPassword(){
        System.out.println(Tag + " checkPasswor()");
        InputStream is = getCommandInput();
        if(is!=null){
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            //while (MainApplication.isWork()&&MainApplication.isClientWork()) {
            try {
                if(!buf.ready()){
                    System.out.println(Tag + " buf unready");
                    return false;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String command="";
            try {
                    command = buf.readLine();
                    System.out.println(Tag + " command " + command);
                } catch (IOException e) {
                    System.out.println(Tag + " checkPassword " + " error: " + e.getMessage());
                    //break;
                }
            return command != null && command.equals("ScreenTranslator -" + DeviceInfo.getThisPassword());
            //}
        }
        return false;
    }
    private static void passPassword(){
        OutputStream out = getCommandOutputStream();
        if(out!=null) {
            BufferedWriter buf = new BufferedWriter(new OutputStreamWriter(out));
            //for (int i = 0; i<10;i++) {
                try {
                    buf.write("ScreenTranslator -" + DeviceInfo.getJoinPassword());
                    buf.flush();
                } catch (IOException e) {
                    System.out.println(Tag + " passPassword " + e.getMessage());
                    stop();
                }
            //}
        }else stop();
    }*/
    public static void startSocket(){
        System.out.println(Tag + " startSocket");
        task=false;
        stop=true;
        MainApplication.setSocketWork(true);
        new Thread(()->{
            System.out.println(Tag + " startSocket commandSocket thread start");
            while(MainApplication.isWork()&&MainApplication.isSocketWork()&&commandSocket==null){
                commandSocket = createSocket(DeviceInfo.getCommandPort());
                if (commandSocket != null) {
                    System.out.println(Tag + " startServer commandSocket created");
                }
            }
            System.out.println(Tag + " startSocket commandSocket thread end");
        }).start();
        new Thread(()->{
            System.out.println(Tag + " startSocket videoSocket thread start");
            while(MainApplication.isWork()&&MainApplication.isSocketWork()&&videoSocket==null){
                videoSocket = createSocket(DeviceInfo.getVideoPort());
                if (videoSocket != null) {
                    System.out.println(Tag + " startSocket videoSocket created");
                    changeUi(true);
                }
            }
            System.out.println(Tag + " startSocket videoSocket thread end");
        }).start();
        /*new Thread(()->{
            System.out.println(Tag + " startSocket audioSocket thread start");
            while(MainApplication.isWork()&&MainApplication.isSocketWork()&&audioSocket==null){
                audioSocket = createSocket(DeviceInfo.getAudioPort());
                if (audioSocket != null) {
                    System.out.println(Tag + " startSocket audioSocket created");
                }
                changeUi(true);
            }
            System.out.println(Tag + " startSocket audioSocket thread end");
        }).start();*/
    }
    private static void checkStop() {
        new Thread(()->{
            InputStream input;
            BufferedReader buf=null;
            while (MainApplication.isWork()&&(MainApplication.isClientWork()||MainApplication.isSocketWork())){
                if(commandSocket!=null){
                    input = getCommandInput();
                    if(input!=null) {
                        if(buf==null) {
                            buf = new BufferedReader(new InputStreamReader(input));
                        }
                        String command = null;
                        try {
                            command = buf.readLine();
                        } catch (IOException e) {
                            System.out.println(Tag + " checkStop Command read " + task + " error: " + e.getMessage());
                            stop();
                        }
                        if (command != null && command.equals("ScreenTranslator -stop")) {
                            try {
                                buf.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            stop=false;
                            stop();
                        }
                    }else stop();
                }
                if(!MainApplication.isWork()){
                    break;
                }
            }
            System.out.println(Tag + " checkStop thread stop");
        }).start();
    }
    public static void stop(){
        System.out.println(Tag + " stop");
        MainApplication.setClientWork(false);
        MainApplication.setSocketWork(false);
        if(stop) {
            Thread t = new Thread(() -> {
                if(commandSocket!=null){
                    OutputStream cos = getCommandOutputStream();
                    BufferedWriter buf;
                    if (cos != null) {
                        buf = new BufferedWriter(new OutputStreamWriter(cos));
                        System.out.println(Tag + " stop BufferedWriter created");
                        try {
                            buf.write(" ScreenTranslator -stop");
                        } catch (IOException e) {
                            System.out.println(Tag + " BufferedWriter write error" + e.getMessage());
                        }
                        try {
                            buf.close();
                        } catch (IOException e) {
                            System.out.println(Tag + " BufferedWriter close error" + e.getMessage());
                        }
                    }
                    try {
                        commandSocket.close();
                    } catch (IOException e) {
                        System.out.println(Tag + " commandSocket close error" + e.getMessage());
                    }
                    commandSocket = null;
                }
                if(commandServer!=null){
                    try {
                        commandServer.close();
                    } catch (IOException e) {
                        System.out.println(Tag + " commandServer close error" + e.getMessage());
                    }
                    commandServer=null;
                }
                System.out.println(Tag + " t end");
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println(Tag + " t.join error" + e.getMessage());
            }
        }
        if(videoSocket!=null){
            try {
                videoSocket.close();
            } catch (IOException e) {
                System.out.println(Tag + " videoSocket close error" + e.getMessage());
            }
            videoSocket=null;
        }
        if(videoServer!=null){
            try {
                videoServer.close();
            } catch (IOException e) {
                System.out.println(Tag + " videoServer close error" + e.getMessage());
            }
            videoServer=null;
        }
        /*if(audioSocket!=null){
            try {
                audioSocket.close();
            } catch (IOException e) {
                System.out.println(Tag + " audioSocket close error" + e.getMessage());
            }
            audioSocket=null;
        }
        if(audioServer!=null){
            try {
                audioServer.close();
            } catch (IOException e) {
                System.out.println(Tag + " videoServer close error" + e.getMessage());
            }
            audioServer=null;
        }*/
        changeUi(false);
    }
    public static OutputStream getCommandOutputStream(){
        if(commandSocket!=null && commandSocket.isConnected() && !commandSocket.isClosed()) {
            try {
                return commandSocket.getOutputStream();
            } catch (IOException e) {
                System.out.println(Tag + " getCommandOutputStream error " + e.getMessage());
                if (commandSocket == null || !commandSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }
    public static OutputStream getVideoOutputStream(){
        if(videoSocket!=null && videoSocket.isConnected() && !videoSocket.isClosed()) {
            try {
                return videoSocket.getOutputStream();
            } catch (IOException e) {
                System.out.println(Tag + " getVideoOutputStream error " + e.getMessage());
                if (videoSocket == null || !videoSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }
    /*public static OutputStream getAudioOutputStream(){
        if(audioSocket!=null && audioSocket.isConnected() && !audioSocket.isClosed()) {
            try {
                return audioSocket.getOutputStream();
            } catch (IOException e) {
                System.out.println(Tag + " getAudioOutputStream error " + e.getMessage());
                if (audioSocket == null || !audioSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }*/
    public static InputStream getCommandInput(){
        if(commandSocket!=null && commandSocket.isConnected() && !commandSocket.isClosed()) {
            try {
                return commandSocket.getInputStream();
            } catch (IOException e) {
                System.out.println(Tag + " getCommandInput error " + e.getMessage());
                if (commandSocket == null || !commandSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }
    public static InputStream getVideoInput(){
        if(videoSocket!=null && videoSocket.isConnected() && !videoSocket.isClosed()) {
            try {
                return videoSocket.getInputStream();
            } catch (IOException e) {
                System.out.println(Tag + " getVideoInput error " + e.getMessage());
                if (videoSocket == null || !videoSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }
    /*public static InputStream getAudioInput(){
        if(audioSocket!=null && audioSocket.isConnected() && !audioSocket.isClosed()) {
            try {
                return audioSocket.getInputStream();
            } catch (IOException e) {
                System.out.println(Tag + " getAudioInput error " + e.getMessage());
                if (audioSocket == null || !audioSocket.isConnected()) {
                    stop();
                }
            }
        }
        return null;
    }*/
    public static void changeUi(boolean bol) {
        boolean vision = bol && ((videoSocket!=null && videoSocket.isConnected()&& !videoSocket.isClosed())
                /*&& (audioSocket!=null && audioSocket.isConnected()&& !audioSocket.isClosed())*/);
        MainController intense = MainController.getInstance();
        if(intense!=null) {
            Platform.runLater(()->intense.changeUi(vision,task));
        }
        if(!vision){
            if(task) {
                if (ReceivingController.isReceiving()) {
                    Platform.runLater(ReceivingController::stopReceiving);
                }
                if (intense != null) {
                    Platform.runLater(intense::setBack);
                }
            }else{
                if(MainApplication.isSharing()){
                    MainApplication.stopSharing();
                }
            }

        }
    }
}
