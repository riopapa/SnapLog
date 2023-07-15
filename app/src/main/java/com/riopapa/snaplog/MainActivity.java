package com.riopapa.snaplog;

import static com.riopapa.snaplog.GPSTracker.oAltitude;
import static com.riopapa.snaplog.GPSTracker.oLatitude;
import static com.riopapa.snaplog.GPSTracker.oLongitude;
import static com.riopapa.snaplog.Vars.NO_MORE_PAGE;
import static com.riopapa.snaplog.Vars.byPlaceName;
import static com.riopapa.snaplog.Vars.cameraOrientation;
import static com.riopapa.snaplog.Vars.currActivity;
import static com.riopapa.snaplog.Vars.mActivity;
import static com.riopapa.snaplog.Vars.mContext;
import static com.riopapa.snaplog.Vars.pageToken;
import static com.riopapa.snaplog.Vars.placeInfos;
import static com.riopapa.snaplog.Vars.placeType;
import static com.riopapa.snaplog.Vars.sharedAutoLoad;
import static com.riopapa.snaplog.Vars.sharedFace;
import static com.riopapa.snaplog.Vars.sharedLocation;
import static com.riopapa.snaplog.Vars.sharedLogo;
import static com.riopapa.snaplog.Vars.sharedMap;
import static com.riopapa.snaplog.Vars.sharedPref;
import static com.riopapa.snaplog.Vars.sharedRadius;
import static com.riopapa.snaplog.Vars.sharedVoice;
import static com.riopapa.snaplog.Vars.sharedWithPhoto;
import static com.riopapa.snaplog.Vars.sigMap;
import static com.riopapa.snaplog.Vars.strAddress;
import static com.riopapa.snaplog.Vars.strPlace;
import static com.riopapa.snaplog.Vars.strVoice;
import static com.riopapa.snaplog.Vars.tvAddress;
import static com.riopapa.snaplog.Vars.tvVoice;
import static com.riopapa.snaplog.Vars.typeAdapter;
import static com.riopapa.snaplog.Vars.typeIcons;
import static com.riopapa.snaplog.Vars.typeInfos;
import static com.riopapa.snaplog.Vars.typeNames;
import static com.riopapa.snaplog.Vars.typeNumber;
import static com.riopapa.snaplog.Vars.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private final static int VOICE_RECOGNISE = 1234;
    private final static int SAVE_MAP = 2345;

    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private SensorManager mSensorManager;
    private DeviceOrientation deviceOrientation;
    static String map_api_key;
    Bitmap cameraImage;
    public static Bitmap googleShot = null;
    public static int zoomValue = 15;

    private static long now_time;
    private boolean exitFlag = false;

    static TextureView mTextureView;
    static Size mPreviewSize;
    static CameraCaptureSession mCameraSession;
    static CaptureRequest.Builder mCaptureRequestBuilder;
    static HandlerThread mBackgroundThread;
    static Handler mBackgroundHandler;

    static ImageReader mImageReader;
    static CaptureRequest.Builder mPreviewRequestBuilder;
    static CaptureRequest mPreviewRequest;
    static Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private CameraCaptureSession mCaptureSession;
    private String mCameraId;
    private CameraDevice mCameraDevice;

    private Size imageDimensions;
    private File file;

    static int mWidth, mHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//        setFullScreen();
        currActivity = this.getClass().getSimpleName();
        mActivity = this;
        mContext = getApplicationContext();
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getApplicationContext()
                    .getPackageName(), PackageManager.GET_PERMISSIONS);
            Permission.ask(this, this, info);
        } catch (Exception e) {
            Log.e("Permission", "No Permission " + e);
        }
        mTextureView = findViewById(R.id.textureView);
        initiate_Variables();
        new FullScreen().set(this, Objects.requireNonNull(getSupportActionBar()));

        ImageView btnShot = findViewById(R.id.btnShot);
        btnShot.setOnClickListener(v -> {
            exitFlag = false;
            take_Picture();
        });
        ImageView btnShotExit = findViewById(R.id.btnShotExit);
        btnShotExit.setOnClickListener(v -> {
            exitFlag = true;
            take_Picture();
        });

        new GPSTracker().get();

        ImageView mSpeak = findViewById(R.id.btnSpeak);
        mSpeak.setOnClickListener(v -> startGetVoice());

        ImageView mPlace = findViewById(R.id.btnPlace);
        mPlace.setOnClickListener(v -> {
            pageToken = NO_MORE_PAGE;
            placeInfos = new ArrayList<>();
            mPlace.setImageResource(typeIcons[typeNumber]);
            EditText et = findViewById(R.id.placeAddress);
            String placeName = et.getText().toString();
            if (placeName.startsWith("?")) {
                String[] placeNames = placeName.split("\n");
                byPlaceName = placeNames[0].substring(1);
            } else
                byPlaceName = "";
            new PlaceRetrieve(mContext, oLatitude, oLongitude, placeType, pageToken, sharedRadius, byPlaceName);
            new Timer().schedule(new TimerTask() {
                public void run() {
                    selectPlace();
                    mPlace.setImageBitmap(utils.maskedIcon(typeIcons[typeNumber]));
                }
            }, 1500);
        });

        mPlace.setImageBitmap(utils.maskedIcon(typeIcons[typeNumber]));

        if (!isNetworkAvailable()) {
            Toast.makeText(mContext, "No Network Available", Toast.LENGTH_LONG).show();
        }
        tvVoice.setText(sharedVoice);
        tvAddress.setText(sharedLocation);
        mTextureView = findViewById(R.id.textureView);
        mTextureView.post(() -> utils.deleteOldLogFiles());
        if (sharedAutoLoad) {
            new PlaceRetrieve(mContext, oLatitude, oLongitude, placeType, pageToken, sharedRadius, byPlaceName);
            new Timer().schedule(new TimerTask() {
                public void run() {
                    selectPlace();
                }
            }, 15000);
        }

        show_logo();

        ImageView ivLogo = findViewById(R.id.logo);
        ivLogo.setOnClickListener(view -> {
            sharedLogo = (++sharedLogo) % 3;  // logo count
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("logo", sharedLogo);
            editor.apply();
            show_logo();
        });

        ImageView ivMap = findViewById(R.id.map);
        float opacity = (sharedMap) ? 1f : 0.3f;
        ivMap.setAlpha(opacity);
        ivMap.setOnClickListener(view -> {
            sharedMap = !sharedMap;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("map", sharedMap);
            editor.apply();
            float opacity1 = (sharedMap) ? 1f : 0.2f;
            ivMap.setAlpha(opacity1);
        });

        ImageView ivFace = findViewById(R.id.btnFacing);
        ivFace.setOnClickListener(view -> {

            sharedFace = (sharedFace == CameraCharacteristics.LENS_FACING_BACK) ?
                    CameraCharacteristics.LENS_FACING_FRONT: CameraCharacteristics.LENS_FACING_BACK;
            SharedPreferences.Editor editor = sharedPref.edit();
            closeCamera();
            openCamera(mWidth, mHeight);
            editor.putInt("face", sharedFace);
            editor.apply();
        });

        ImageView ivSet = findViewById(R.id.setting);
        ivSet.setOnClickListener(view -> start_Setting());

    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }


    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mWidth = width;
            mHeight = height;
            openCamera(mWidth, mHeight);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private void openCamera(int width, int height) {
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            imageDimensions = map.getOutputSizes(SurfaceTexture.class)[0];

            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            finish();
        }

    };
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
//                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
//                    continue;
//                }
                if (facing != null && facing == sharedFace) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        null, mBackgroundHandler);

                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (maxPreviewWidth > 1920) {
                    maxPreviewWidth = 1920;
                }

                if (maxPreviewHeight > 1080) {
                    maxPreviewHeight = 1080;
                }

                // Danger! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Toast.makeText(this, "Camera2 API not supported on this device", Toast.LENGTH_LONG).show();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void start_Setting() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
//        startActivityForResult(intent);
        startActivity(intent);
    }

    private void selectPlace() {
        Intent intent = new Intent(MainActivity.this, SelectActivity.class);
        startActivity(intent);
    }

    private void show_logo() {
        sigMap = utils.buildSignatureMap();
        ImageView iv = findViewById(R.id.logo);
        iv.setImageBitmap(sigMap);
    }
    private void initiate_Variables() {

        utils = new Utils(this);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        deviceOrientation = new DeviceOrientation();

        utils.getPreference();
        map_api_key = getString(R.string.maps_api_key);
        pageToken = NO_MORE_PAGE;
        placeInfos = new ArrayList<>();
        tvVoice = findViewById(R.id.textVoice);
        tvAddress = findViewById(R.id.placeAddress);

        typeInfos = new ArrayList<>();
        for (int i = 0; i < typeNames.length; i++) {
            typeInfos.add(new TypeInfo(typeNames[i], typeIcons[i]));
        }
        RecyclerView typeRecyclerView = findViewById(R.id.type_recycler);
        LinearLayoutManager mLinearLayoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        typeRecyclerView.setLayoutManager(mLinearLayoutManager);
        typeAdapter = new TypeAdapter(typeInfos);
        typeRecyclerView.setAdapter(typeAdapter);

    }

    static void inflateAddress() {
        mActivity.runOnUiThread(() -> {
            String s = strPlace + "\n" + strAddress;
            tvAddress.setText(s);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_RECOGNISE) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                strVoice = (strVoice + " " + result.get(0)).trim();
                tvVoice.setText(strVoice);
            }
        } else if (requestCode == SAVE_MAP) {
            save_GoogleMap(googleShot);
            checkIfExit();

        } else {
            Toast.makeText(mContext, "Request Code:" + requestCode + ", Result Code:" + resultCode + " not as expected", Toast.LENGTH_LONG).show();
        }
    }
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void checkIfExit() {
//        startCamera();
        strVoice = "";
        if (exitFlag) {
            finish();
            Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/gallery");
            intent.setAction(Intent.ACTION_PICK);
            startActivity(intent);
            new Timer().schedule(new TimerTask() {
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }, 5000);   // wait while photo generated
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cM = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo aNI = cM.getActiveNetworkInfo();
        return aNI != null && aNI.isConnected();
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("Camera2", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
    private void startGetVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());    //데이터 설정
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);   //검색을 말한 결과를 보여주는 갯수

        try {
            startActivityForResult(intent, VOICE_RECOGNISE);
        } catch (ActivityNotFoundException a) {
            //
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(ExifInterface.ORIENTATION_NORMAL, 0);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_90, 90);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_180, 180);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_270, 270);
    }

    private void take_Picture() {

        int mDeviceRotation = ORIENTATIONS.get(deviceOrientation.getOrientation());
        if (mDeviceRotation == 0)
            cameraOrientation = 1;
        else if (mDeviceRotation == 180)
            cameraOrientation = 3;
        else if (mDeviceRotation == 90)
            cameraOrientation = 6;
        else
            cameraOrientation = 8;

        sharedLocation = tvAddress.getText().toString();

        try {
            strPlace = sharedLocation.substring(0, sharedLocation.indexOf("\n"));
            if (strPlace.equals("")) {
                strPlace = " ";
            }
            strAddress = sharedLocation.substring(sharedLocation.indexOf("\n") + 1);
        } catch (Exception e) {
            strPlace = strAddress;
            strAddress = "?";
        }
        strVoice = tvVoice.getText().toString();
        if (strVoice.length() < 1)
            strVoice = " ";
        tvVoice.setText("");
        sharedVoice = strVoice;
        utils.putPlacePreference();

        try {
            takePicture();
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }
    private void takePicture() throws CameraAccessException {
        if(mCameraDevice==null)
            return;

        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);

        CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
        Size[] jpegSizes = null;

        jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

        int width = 640;
        int height = 480;

        if(jpegSizes != null && jpegSizes.length>0){
            width = jpegSizes[0].getWidth();
            height = jpegSizes[0].getHeight();
        }

        ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        List<Surface> outputSurfaces = new ArrayList<>(2);
        outputSurfaces.add(reader.getSurface());

        outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

        final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

        Long tsLong = System.currentTimeMillis()/1000;
        String ts = tsLong.toString();

        file = new File(Environment.getExternalStorageDirectory() + "/"+ts+".jpg");

        ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;

                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                try {
                    save(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(image != null){
                        image.close();
                    }
                }
            }
        };

        reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

        final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                try {
                    createCameraPreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        };

        mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                try {
                    session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        }, mBackgroundHandler);

    }
    private void save(byte[] bytes) throws IOException {
        OutputStream outputStream = null;
        outputStream = new FileOutputStream(file);
        outputStream.write(bytes);
        outputStream.close();
    }
    private void createCameraPreview() throws CameraAccessException {
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(imageDimensions.getWidth(), imageDimensions.getHeight());
        Surface surface = new Surface(texture);

        mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mCaptureRequestBuilder.addTarget(surface);

        mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if (mCameraDevice == null) {
                    return;
                }

                mCameraSession = session;
//                try {
//                    updatePreview();
//                } catch (CameraAccessException e) {
//                    e.printStackTrace();
//                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Toast.makeText(getApplicationContext(), "Configuration Changed", Toast.LENGTH_LONG).show();
            }
        }, null);
    }
    private void updatePreview() throws CameraAccessException {
        if (mCameraDevice == null) {
            return;
        }

        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        mCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);

    }
    private class SaveImageTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... data) {
            stopBackgroundThread();
            now_time = System.currentTimeMillis();
            BuildBitMap buildBitMap = new BuildBitMap(cameraImage, oLatitude, oLongitude, oAltitude, mActivity, mContext, cameraOrientation);
            buildBitMap.makeOutMap(strVoice, strPlace, strAddress, sharedWithPhoto, now_time,"");
            return "";
        }

        @Override
        protected void onPostExecute(String none) {

            if (sharedMap) {
                googleShot = null;
                Intent intent = new Intent(mContext, LandActivity.class);
                intent.putExtra("lan", oLatitude);
                intent.putExtra("lon", oLongitude);
                intent.putExtra("alt", oAltitude);
                intent.putExtra("zoom", 15);
                startActivityForResult(intent, SAVE_MAP);
            } else {
                checkIfExit();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "ERROR: Camera permissions not granted", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private static void save_GoogleMap(Bitmap googleShot) {
        BuildBitMap buildBitMap = new BuildBitMap(googleShot, oLatitude, oLongitude, oAltitude, mActivity, mContext, cameraOrientation);
        buildBitMap.makeOutMap(strVoice, strPlace, strAddress, sharedWithPhoto, now_time, "Map");
    }
}