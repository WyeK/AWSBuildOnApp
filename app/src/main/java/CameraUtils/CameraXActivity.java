package CameraUtils;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import SocketClients.UDPClient;

public class CameraXActivity {
    public void startCameraX(ProcessCameraProvider cameraProvider, Context context, PreviewView previewView, Executor executor) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(720, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(executor, new AnalyzerUdp());
        cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageAnalysis, preview);
    }

    class AnalyzerUdp implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            // Retrieve image in raw (yuv_420_888) format
            ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = imageProxy.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = imageProxy.getPlanes()[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);

            // Convert yuv_420_888 image to JPEG byte stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 20, baos);
            byte[] imageBytes = baos.toByteArray();

            // Send UDP Packet
            new ConnectUdp().execute(imageBytes);

            //
            imageProxy.close();
        }
    }

    // Async call to UDP
    public static class ConnectUdp extends AsyncTask<byte[], Void, Void> {
        UDPClient mUdpClient;

        @Override
        protected Void doInBackground(byte[]... imageBytes) {
            mUdpClient = new UDPClient();
            mUdpClient.run(imageBytes[0]);
            return null;
        }
    }
}
