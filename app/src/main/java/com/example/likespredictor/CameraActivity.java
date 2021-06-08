package com.example.likespredictor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static androidx.camera.core.CameraSelector.LENS_FACING_BACK;
import static androidx.camera.core.CameraSelector.LENS_FACING_FRONT;

public class CameraActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_PERMISSION = 123;
    private final static String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private final static int INPUT_TENSOR_WIDTH = 224;
    private final static int INPUT_TENSOR_HEIGHT = 224;
    private final static float means = 3915.875f;
    private final static float stds = 23552.26355037f;

    private ConstraintLayout constraintLayout;
    private TextView textview;
    private ImageButton captureButton;
    private ImageButton switchButton;
    private PreviewView previewView;
    private Executor executor;
    private CameraSelector cameraSelector;
    private ImageCapture imageCapture;

    private int lensFacing = LENS_FACING_BACK;
    private final Bitmap bitmap = null;
    private Module module = null;
    private Module aestetics = null;
    private Module regression_module = null;

    private final long[] tensor_shape = new long[] {1, 512};
    private final long[] tensor2_shape = new long[] {1, 1};
    private final float[] subscribers = {-0.1620f};
    private float[] scores = {};
    private float[] arr1 = {};
    private float res = 0;

    private Tensor inputTensor = null;
    private Tensor new_Tensor = null;
    private Tensor outputTensor = null;
    private Tensor outputTensor2 = null;
    private final FloatBuffer mInputTensorBuffer = Tensor.allocateFloatBuffer(3 * INPUT_TENSOR_WIDTH * INPUT_TENSOR_HEIGHT);
    private final Tensor subscribers_tensor = Tensor.fromBlob(subscribers, tensor2_shape);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_camera);

        constraintLayout = findViewById(R.id.camera_container);
        textview = findViewById(R.id.text_prediction);
        captureButton = findViewById(R.id.camera_capture_button);
        switchButton = findViewById(R.id.camera_switch_button);
        previewView = findViewById(R.id.view_finder);
        executor = Executors.newSingleThreadExecutor();

        Intent intent = getIntent();
        String text = intent.getStringExtra("sub");
        Integer subscribers_ = Integer.valueOf(text);

        switchButton.setOnClickListener(v -> {
            if (lensFacing == LENS_FACING_BACK)
                lensFacing = LENS_FACING_FRONT;
            else
                lensFacing = LENS_FACING_BACK;

            setUpCameraX();
        });

        captureButton.setOnClickListener(v ->
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    File file = new File(getExternalMediaDirs()[0], System.currentTimeMillis() + ".jpg");

                    ImageCapture.OutputFileOptions outputFileOptions =
                            new ImageCapture.OutputFileOptions.Builder(file).build();

                    imageCapture.takePicture(outputFileOptions, executor,
                            new ImageCapture.OnImageSavedCallback()
                            {
                                @Override
                                public void onImageSaved(ImageCapture.OutputFileResults outputFileResults)
                                {
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Toast.makeText(CameraActivity.this, "Photo saved as " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                @Override
                                public void onError(ImageCaptureException error)
                                {
                                    runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Toast.makeText(CameraActivity.this, "Couldn't save photo: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                    );
                }
            });
        });

        try
        {
            regression_module = Module.load(assetFilePath(this, "regression_v2.pt"));
            module = Module.load(assetFilePath(this, "resnet_v4.pt"));
            aestetics = Module.load(assetFilePath(this, "aestetics.pt"));
        }
        catch (IOException e)
        {
            finish();
        }

        if (checkPermission())
            setUpCameraX();
    }

    private boolean checkPermission()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                                                     PERMISSIONS,
                                                     REQUEST_CODE_PERMISSION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSION)
        {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(CameraActivity.this, "You can't use application without granting CAMERA permission", Toast.LENGTH_LONG).show();
                    }
                });
                finish();
            }
            else
                setUpCameraX();
        }
    }

    private void setUpCameraX()
    {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture
                = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                }
                catch (ExecutionException | InterruptedException e) { }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider)
    {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        executor = Executors.newSingleThreadExecutor();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(224, 224))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                int rotationDegrees = image.getImageInfo().getRotationDegrees();

                TensorImageUtils.imageYUV420CenterCropToFloatBuffer(image.getImage(),
                        rotationDegrees,
                        INPUT_TENSOR_WIDTH,
                        INPUT_TENSOR_HEIGHT,
                        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                        TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                        mInputTensorBuffer,
                        0);

                inputTensor = Tensor.fromBlob(mInputTensorBuffer, new long[]{1, 3, INPUT_TENSOR_HEIGHT, INPUT_TENSOR_WIDTH});

                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
                        scores = outputTensor.getDataAsFloatArray();
                        new_Tensor = Tensor.fromBlob(scores, tensor_shape);

                        outputTensor2 = regression_module.forward(IValue.from(new_Tensor), IValue.from(subscribers_tensor)).toTensor();

                        arr1 = outputTensor2.getDataAsFloatArray();
                        res = arr1[0];

                        textview.setText(String.valueOf(custom_inverse_transform(res)));
                    }
                });
                image.close();
            }
        });

        imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .build();

        cameraProvider.unbindAll();
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this,
                cameraSelector, imageAnalysis, preview, imageCapture);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    private static String assetFilePath(Context context, String assetName) throws IOException
    {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0)
            return file.getAbsolutePath();

        try (InputStream is = context.getAssets().open(assetName))
        {
            try (OutputStream os = new FileOutputStream(file))
            {
                byte[] buffer = new byte[4 * 1024];
                int read;

                while ((read = is.read(buffer)) != -1)
                    os.write(buffer, 0, read);

                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    private int custom_inverse_transform(float scaled_value)
    {
        float result = scaled_value * stds + means;
        return (int) result;
    }
}