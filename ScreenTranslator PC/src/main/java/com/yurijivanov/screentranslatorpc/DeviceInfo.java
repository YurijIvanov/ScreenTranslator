package com.yurijivanov.screentranslatorpc;

public class DeviceInfo {
    private static String ip="", ipJoin="";
    private static int commandPort = 8001, videoPort = 8002, audioPort = 8003;
    public static String getIp() {
        return ip;
    }

    public static void setIp(String ip) {
        DeviceInfo.ip = ip;
    }

    public static String getIpJoin() {
        return ipJoin;
    }

    public static void setIpJoin(String ipJoin) {
        DeviceInfo.ipJoin = ipJoin;
    }

    public static int getVideoPort() {
        return videoPort;
    }
    public static void setVideoPort(int videoPort) {
        DeviceInfo.videoPort = videoPort;
    }

    public static int getAudioPort() {
        return audioPort;
    }

    public static void setAudioPort(int audioPort) {
        DeviceInfo.audioPort = audioPort;
    }

    public static int getCommandPort() {
        return commandPort;
    }

    public static void setCommandPort(int commandPort) {
        DeviceInfo.commandPort = commandPort;
    }
}
