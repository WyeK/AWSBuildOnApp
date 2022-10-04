package SocketClients;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient {
    public static final String TAG = UDPClient.class.getSimpleName();
    public static final String HOST = "3.1.217.251";
    public static final int PORT = 9999;
    DatagramSocket datagramSocket;
    public void run(byte[] imageBytes){
        new Thread(new Runnable(){
            @Override
            public void run() {
                try{
                    InetAddress serverAddr = InetAddress.getByName(HOST);
                    datagramSocket = new DatagramSocket(PORT);
                    DatagramPacket datagramPacket;
                    int totalBytes = imageBytes.length;
                    datagramPacket = new DatagramPacket(imageBytes, imageBytes.length, serverAddr, PORT);
                    datagramSocket.send(datagramPacket);
                    Log.d("UDP", "S: Sending UDP packet");
                } catch (Exception e){
                    Log.e("UDP", "S: Error", e);
                } finally {
                    if (datagramSocket != null){
                        datagramSocket.close();
                    }
                }
            }
        }).start();
    }
}
