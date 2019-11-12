package com.example.visionapi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.models.ImageModel;
import com.example.models.SegmentResponseModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.network.ApiClient;
import com.network.ApiInterface;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 1;
    private CameraManager cameraManager;
    private int cameraFace;
    private TextureView surfaceView;
    private TextureView.SurfaceTextureListener surfaceTextureListner;
    private String TAG ="ma";
    private String cameraId;
    private Size imageDimension;
    private CameraDevice.StateCallback stateCallback;
    protected CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    protected CameraCaptureSession cameraCaptureSessions;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private ImageReader imageReader;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private FirebaseVisionObjectDetector objectDetector;
    private FirebaseVisionObjectDetectorOptions options;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);

        surfaceView = findViewById(R.id.camera);

        String bs64 ="iVBORw0KGgoAAAANSUhEUgAAAMgAAADICAYAAACtWK6eAAAABmJLR0QA/wD/AP+gvaeTAAAN9ElEQVR4nO3de7CVVRnH8e/hIiCgiMDkBdCw0oOWZak5ImAJEurQODaNpZjKlIAcr6WOUzFm2s2mKa9NSeRkjZWjljpjFycYLS/ljRA0DUwpSrkqIhx2fzxn637ftfY5Z+93ve/al99n5p1psPO865y9nv1e1lrPAhERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERHpW0fsBgQ2ATgFmA1MAvbt+fdXgOeBe4G7gbVRWicSyf7AzcAOoNTHsQNYChwYpaUiBZsFbKTvxEgfm4E5EdorUpiLgG5qT47y0d0TQ6TlXIq/0z8DLAI6geE9x2SgC1hR5WcuK7jtIrm6CLeTbwcWAAN6+bmBwMKe/6+SRFpSF27n3gJMqyHGtJ6fUZJIS5kH7CLZqV8HptcR6xjsQT2dJFcGaalIwc7FnxzHZ4ipJJGW4LtyvAF8LEDsY9HtljSx2bgDgG/2/HsoupJIUzoMu42q7LTbgJk5nEtXEmkqQ4AncKeJhLxypClJpGlcidtRzy7gvEoSaXhjgE0kO+iSGmMsAx4DxtVxfj2TSEO7imTHXA/sWWOMR3p+9mnqSxJdSaQhDcLWb1R2yq464owFnur5+ZXAPnXEUJJIwzmZZGfciE04rIeSRFrObSQ74o0Z443DbrNK2G1XPZQk0hCG4j6cHxsg7jjsgX1Zhhh6cJfoTiPZ+V6i9+nrRVOSSFR3kOx418Vtjle12y2tTJRcjcAmIFZ2uqNyPF+WcRJfknQDJwVrnUjK6SQ73AvkW5Yoj3GSTcB+oRooUulukp3tmpzPF+IV8FTc5bu3hGqgSNkobAp7ZUc7vIDzhkiShbgTKseHaqAI2CTEyk72bIHnzpokA7FKKpXtXxiygSL3k+xgiws+f9YkSReS+G3Q1klbG4u7YrAzUjvqTZLJJNu/OnjrpG2dR7JzPRGxLfUmyQiSv8OWXFonbelBkp3r8qitqS9JRpL8HTbn1jppK/vi1tadFLVFptYkSd9ircq1ddI20g+3j8ZtTkItSXIByd/jntxbJ23hYZId6+K4zXH0J0kG4hbFnl9UA6V1TSRZDG4XtkNUo+krSRaRTI630EChBJDevmB53Ob0qlqSTMMSovL3uClC+6QF/YFkxzo/bnP6lE6SObiTFTeiyYoSyGskO1cj3l6lVSZJ+u1bN/kWtZM2kx49HxS3Of02B/+2b1owJUGtI9nBDo3bnH6ZBmzFTY4vRmyTtKi7SHaya+M2p0/T8SfHpTEbJa1rLsmO9m9gcNQWVTcF/zr0K2I2SlrbMGADyQ7XiPuWH4c/OWLPGZM2cAONPUXjOPy3VSoYJ4U4gmTH20njjEKfgFtlRckhhXucZAf8StzmADADNzl2UV8RbZFM5pPsiP/DFiDFUi05FkVsk7Sxkbij6rE644nYHojp5FARBonqayQ75Vpgt4LbMBN/cjT6HDFpA3vjvi2aW+D5deWQhvd9kh10JcVUdldySFMYj7u2Iu+Bw1n4k2NBzucVqctSkp31KfK7ilRLDi2XlYbViTuV/IwcznMybtHpbmBeDucSCeqnJDvuP4EhAePPxp8c5wY8h0huDsCt9B5yMdI/cJPjnIDxRXL3XdzR9VGBYq9Jxb49UFyRwozBCiBUduRQG+qkq6m8Tn37gohEdQXJjryNMDN9h2K751bG/kGAuCKF2h14mWRHvi1Q7HRF+e3AgYFiixRmHu56jOMDxB0MPJ+KuyRAXJFCDQT+RrIj/50wExk/m4q7E6vSLtJUPoI7eBhiVd8AbLOeyri/ChBXpHC34L55mhgg7pxU3F3A0QHiihRqNLCeZGe+M1Ds9BYMDwMdgWKLFCa9VXQJm1OV1TRP3E8FiCtSqA5si4TKjvwiMDxA7DtTcV8g7PwvkUK8H7fo9fUB4r4HdwKjyvtIU/oO7oP1xwPETc//2krj1OgS6bfdsV1kKzvzGmDPjHFHA/9Nxb0jY0yRKI7GBvYqO/OPA8T1jdyfGCCuSOGuJfxbrQHAX1IxV2GFtkWayhDe2Q6tfKwHxmWMewTu1anR9y4R8foQbiWUnweIm646vwNLHJGm82XcW63PZIy5B+7Kwydp3A1+RKoaBDxGsjNvAQ7OGPdE3MS7KmNMkSg6cSuyP429Es5iSSrmTmBqxpgiUZyD+42f9dXvaOBfqZhre/5dpOmkKzOWgLMyxpyOux5F60akKQ0HVpDszNuAD2SM+3XcxLskY0yRKCbjbqWwCtuop16DgIdSMbuBT2RqqUgkc3G/8bNWRHk38Goq5mvYTGCRpvMj3CTJurXbDNxR9hWEq/ooUpihuBVRdgInZYx7MW7i/Zm4G4+K1OVgYBPJzryJ7OV9fG/L7qP4fRXzNgHbaes+YDX2bLe153/f2/PfJkRrnQQxC/e26AVgbIaYQ4EHcZPkF1gtr2Y3Hvgh7upN37EDqzqzf5SWShBduB/scrKtO98DeNQT904sgZrVLNzC4f05NpP/lnmSoxtxP9SlGWOOxTYbTce9nzDFJIp2Ee6gaC1HN2H3cpECDQIewP1QL88Ydzy2C1Y67kPAXhljF+lL+Dv9M9jbv04s6Ydjz3BduIOy5UMFL5rUXrjf+N3AqRnjjvfELWF1hN+bMXYR0numlLAqLwvofRPVgdiDeroijJKkiR2E7VhV+WG+AUzJGHcs7rT7ErCBxl7Xfglum7dQ26zlaT0/oyRpEVNw90HcBHwwY9wR+G/jdmK3MI3mQty2vo51+Fodgz2op+NdGaKhUrzPYTW1Kj/MV7ApJVkMBX6C21FKwK+BvTPGD8WXHFuB4zLEPBZdSVrKZbgf5nPAuwLEvhD/OMJL1PcNHdIF+JOjltuqapQkLeY6/G9usgwklp2AO8GxfMt1NXHWuHfhXjnrva2qRknSQjrwTx15gjArBw/E3WKhfDwFfDjAOfprEf7kmJ7DuZQkLWQwcDfuh/ko2Uuago3BXI1/EG4H8E3yL063EH9yhNjzsRolSQsZgo2Apz/MhwiTJGC3MektqMvHamw6fR4WUHxylClJWsgw4Pf4ryShCjXsBdzqOUf5+CVhZ8jOx02ON6i9Iv4ybJynnsqVSpIWMhzrDL5nkhAP7mUzcYvTVX67X0H2SY/n4U+OE+qI9UjPzz+NkqTtjcSfJM8Q5hVw5Xmup/oEwReBT1PfnolfIFxygH05lGshrwT2qSOGkqSFDMd/u/Uc2QcT047ArSpfeTwMfLSGeJ/HTY5t2FUrCyWJJAzB/3ZrHXB44HN1AGcC//Gcr3w80I/zno17RdpO9qXGZeOw26wSdttVD01LaSHVkmQD2aZlVDMa2w7ON0O2hHX+pdj4Stq5uMmxjfCTJcdhD+zLMsTQlaSFDMY/mPgG2afKV3MQVsGx2tVkO/b8Ur7NOQc3Od6ksWt3KUlaSAf+aSm7sDdOeZmCW7Qu/cbrdvzJMTvHdoVSLUm0MrFJXYb7AFzCrjB57q1+Cu6OWtWOGMkRepykm+ZIcPE4C/8zwnLCjpWkDcA2B3rec+7KW6+sezXWI49xkk3AfqEaKMWairsysYSVFMpad6svg7Fxjpdxk+OUnM9dTYhXwFNxv3huDtVAKd5B+Neib8LK5+RtGLaefA12VYn9QB4iSRaS/FvuwNb8S5MaRfVltl0R2xVL1iQZiFstZUHIBkrxBuGvu1XCqsq3W93erEmSXvH4m6Ctk2gW4ZY5LXeSQyO2K4YsSTKZ5N9vdfDWSTQz8Jfu3Eb73XLVmyQjSP7ttuTSOonmfVQfs7iV7LvvNpN6kmQkyb/Z5txaJ9EMw7+JTwmbNn9IvKYVrtYkSd9ircq1dRLVmbh7JpZvG86I2K6i1ZIk6Yf0e3JvnUR1MNVvue4AxsRrWqH6kyS+17zzi2qgxDOc6hUX1xFvBLxofSXJIpJ/m7fQQGFbORubgetLlCWEq6DSyKolyTQsISr/JjdFaJ9EdgjvTO5LH2upf914M0knySdxJytuRJMV29YgbImpb1bwLuAGmnOXqlpUJkl6PYumuwtga8yfxH81eZE4U9eLdCr+qi5aMCVvG4ztH5K+/658zXlArMblaAa2bDn9++a5QlOa2JH4p8+XsI70VfJdtVikmfiTI+uekdLihgHfovrVZCXF1NTN02xsblr6uavd5qpJBocBf8KfJCXgZzTn+MA83I2FdmELpURq0oGtf19P9duub9Ac202PwtbG+N7YnRexXdICRmMDZtVq+L6K7Vabtdh1HkZgb6R86/d3YlcUkSCOBB6n+m3XGmAuve9tXoQBWD2vG7H1+b62biVcOVSRtw3EpqtU24inhA2+nUqxidIBHIUV1+utbSXgr0BngW2TNjQMGzvZQPWO+Cy23fVuObbjcOAarNxRb0lRnjpyec7tEUkYDXwbq6ZYrWOuxdZYhJq60gksxhKwr6QoYfW7FtM4e8RLG5qIlT+t9iBfwh6UF1Pf6+FJ2Ah3tWkx6WMz9tbqZGzemUhDmAR8j96vKN1YLa/T6H2v9v2xwbvl+OsRp49t2LSYM2m/UkfSZCZgieJb7lt5rAOuxcoSDcb2HTmf/ifFm9j+KaejpJAmNAabw/Uq/bs16s+xE0ugLvIt1i1SmJFYwevHqC8puoE/9sRolzX00qY6sdsq3+h2+liBvU7eN0pLRSLaHXugvgt4BZtBvB74HVY1fmK8pomIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIhI8/g/cnGrF887qsEAAAAASUVORK5CYII=";
        uploadImage(bs64);

       /* new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//               takePicture();
                String bs64 ="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";
                uploadImage(bs64);

            }
        },3000);*/

        surfaceTextureListner = new TextureView.SurfaceTextureListener(){
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                    requestPermission();
//                    return;
//                }else{
                    openCamera();
//                }

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        };
        surfaceView.setSurfaceTextureListener(surfaceTextureListner);



        stateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                createCameraPreview();

            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                cameraDevice.close();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                cameraDevice.close();
                cameraDevice = null;
            }
        };

        options =
                new FirebaseVisionObjectDetectorOptions.Builder()
                        .setDetectorMode(FirebaseVisionObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()  // Optional
                        .build();

         objectDetector =
                FirebaseVision.getInstance().getOnDeviceObjectDetector(options);
    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = surfaceView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                    openCamera();
                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }

    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void openCamera() {
        cameraManager= (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraFace =CameraCharacteristics.LENS_FACING_FRONT;
        Log.e(TAG, "is camera open");
        try {

            cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);
                requestPermission();
                return;
            }


            cameraManager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (surfaceView.isAvailable()) {
            openCamera();
        } else {
            surfaceView.setSurfaceTextureListener(surfaceTextureListner);
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(surfaceView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            final int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    FirebaseVisionImage firebaseVisionImage = null;
                    try {
                        image = reader.acquireLatestImage();
                        String encodedImage ;
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        encodedImage = Base64.encodeToString(bytes, Base64.DEFAULT);
                        save(bytes);
                        Log.d(TAG, "onImageAvailable: "+ encodedImage);
                        uploadImage(encodedImage);
//                        firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, rotation);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }

                   /* FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                            .setWidth(480)   // 480x360 is typically sufficient for
                            .setHeight(360)  // image recognition
                            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                            .setRotation(rotation)
                            .build();
                    
                    objectDetector.processImage(firebaseVisionImage)
                            .addOnSuccessListener(
                                    new OnSuccessListener<List<FirebaseVisionObject>>() {
                                        @Override
                                        public void onSuccess(List<FirebaseVisionObject> detectedObjects) {
                                            // Task completed successfully

                                            for (FirebaseVisionObject obj : detectedObjects) {
                                                Integer id = obj.getTrackingId();
                                                Rect bounds = obj.getBoundingBox();
                                                Log.d(TAG, "onSuccess: Camera bound"+detectedObjects.size());

                                            }
                                            // ...
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d(TAG, "onFailure: "
                                            +e.getMessage());
                                            // Task failed with an exception
                                            // ...
                                        }
                                    });*/
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);

                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void uploadImage(String encodedImage) {

        ImageModel.ImageDetails imageDetails = new ImageModel.ImageDetails(encodedImage,"","");
        ImageModel.ImageRotation imageRotation = new ImageModel.ImageRotation("1.4658129805029452","6.027456183070403","0.8008281904610115");

        ImageModel imageModel = new ImageModel(imageDetails,imageRotation);
        CompositeDisposable disposable = new CompositeDisposable();
        Map<String, Object> jsonValues = new HashMap<String, Object>();
        jsonValues.put("image", imageDetails);
        jsonValues.put("rotation", imageRotation);
        JSONObject json = new JSONObject(jsonValues);


        ApiInterface apiInterface  = ApiClient.getClient(MainActivity.this).create(ApiInterface.class);
        disposable.add(apiInterface.uploadImage(imageModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<SegmentResponseModel>() {

                    @Override
                    public void onNext(SegmentResponseModel segmentResponseModel) {
                        Log.d(TAG, "onSuccess: "+segmentResponseModel.getSupport_info().getInternal_trace_id());
                       /* @Override
                        public void onSuccess(SegmentResponseModel response) {
                            Log.d(TAG, "onSuccess: "+response.getSupport_info().getInternal_trace_id());
                        }

                        @Override
                        public void onError(Throwable e) {

                        }*/
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("TAG", "onError: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                }));



    }
}
