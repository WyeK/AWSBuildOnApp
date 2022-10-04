package com.example.awsbuildonapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import CameraUtils.CameraXActivity;
import JsonUtils.ObstacleJson;
import SocketClients.TCPClient;

public class ObstacleActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private TextToSpeech mTTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle);
        // Back button
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        //CameraX Initialization
        previewView = findViewById(R.id.obstaclePreview);
        cameraProviderFuture = ProcessCameraProvider.getInstance(ObstacleActivity.this);
        cameraProviderFuture.addListener(() ->{
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                new CameraXActivity().startCameraX(cameraProvider, ObstacleActivity.this, previewView, getExecutor());
            } catch (InterruptedException | ExecutionException e){
                e.printStackTrace();
            }
        }, getExecutor());

        // Text to speech init
        mTTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = mTTS.setLanguage(Locale.ENGLISH);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                }
            } else {
                Log.e("TTS", "Initialization Failed");
            }
        });

        new ConnectTcp().execute("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Executor getExecutor(){
        return ContextCompat.getMainExecutor(this);
    }

    // Async call to connect tcp
    public class ConnectTcp extends AsyncTask<String, String, TCPClient> {
        TCPClient mTcpClient;

        // Listen to message
        @Override
        protected TCPClient doInBackground(String... message) {
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                public void messageReceived(String message) {
                    //Calls onProgressUpdate
                    publishProgress(message);
                }
            });
            mTcpClient.run();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.d("TCP", "response " + values[0]);
            Gson gson = new Gson();
            ObstacleJson obstacleData = gson.fromJson(values[0], ObstacleJson.class);
            warnAreaVis(obstacleData.getAreas());
        }
    }
    private void warnAreaVis(List<Boolean> areas){
        RelativeLayout area0 = findViewById(R.id.layout_area0);
        RelativeLayout area1 = findViewById(R.id.layout_area1);
        RelativeLayout area2 = findViewById(R.id.layout_area2);
        String speech = "";
        Boolean isObstacle = false;
        area0.setVisibility(View.INVISIBLE);
        area1.setVisibility(View.INVISIBLE);
        area2.setVisibility(View.INVISIBLE);
        if (areas.get(0)){
            area0.setVisibility(View.VISIBLE);
            speech += "left ";
            isObstacle = true;
        }
        if (areas.get(1)){
            area1.setVisibility(View.VISIBLE);
            speech += "middle ";
            isObstacle = true;
        }
        if (areas.get(2)){
            area2.setVisibility(View.VISIBLE);
            speech += "right ";
            isObstacle = true;
        }
        if (isObstacle){
            mTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
}