package SocketClients;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPClient {
    public static final String TAG = TCPClient.class.getSimpleName();
    public static final String HOST = "3.1.217.251"; // IP Address
    public static final int PORT = 9999; // Port
    private String mServerMessage; // Message to send
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    private PrintWriter mBufferOut;
    private BufferedReader mBufferIn;

    public TCPClient(OnMessageReceived listener){
        mMessageListener = listener;
    }

    public void sendMessage(final String message){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mBufferOut != null){
                    Log.d(TAG, "Sending: " + message);
                    mBufferOut.println(message);
                    mBufferOut.flush();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void stopClient(){
        mRun = false;
        if (mBufferOut != null){
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
    }

    public void run(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                mRun = true;

                try {
                    InetAddress serverAddr = InetAddress.getByName(HOST);
                    Log.d("TCP Client", "C: Connecting...");

                    Socket socket = new Socket(serverAddr, PORT);
                    Log.d("TCP Client", "C: Connected");
                    try {
                        mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                        mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                        while (mRun){
                            mServerMessage = mBufferIn.readLine();
                            if (mServerMessage != null && mMessageListener != null){
                                mMessageListener.messageReceived(mServerMessage);
                            }
                        }
                        Log.d("RESPONSE FROM SERVER", "S: Received Message: " + mServerMessage);
                    } catch (Exception e){
                        Log.e("TCP", "S: Error", e);
                    } finally {
                        socket.close();
                    }
                } catch (Exception e){
                    Log.e("TCP", "C: Error", e);
                }
            }
        }).start();
    }

    public interface OnMessageReceived{
        public void messageReceived(String message);
    }
}
