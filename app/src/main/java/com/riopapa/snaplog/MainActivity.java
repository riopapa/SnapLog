package com.riopapa.snaplog;

import static com.riopapa.snaplog.GPSTracker.oAltitude;
import static com.riopapa.snaplog.GPSTracker.oLatitude;
import static com.riopapa.snaplog.GPSTracker.oLongitude;
import static com.riopapa.snaplog.Vars.NO_MORE_PAGE;
import static com.riopapa.snaplog.Vars.REQUEST_CAMERA_PERMISSION;
import static com.riopapa.snaplog.Vars.SAVE_MAP;
import static com.riopapa.snaplog.Vars.byPlaceName;
import static com.riopapa.snaplog.Vars.cameraOrientation;
import static com.riopapa.snaplog.Vars.cameraSub;
import static com.riopapa.snaplog.Vars.currActivity;
import static com.riopapa.snaplog.Vars.deviceOrientation;
import static com.riopapa.snaplog.Vars.exitFlag;
import static com.riopapa.snaplog.Vars.googleShot;
import static com.riopapa.snaplog.Vars.mActivity;
import static com.riopapa.snaplog.Vars.mBackgroundHandler;
import static com.riopapa.snaplog.Vars.mBackgroundThread;
import static com.riopapa.snaplog.Vars.mContext;
import static com.riopapa.snaplog.Vars.mHeight;
import static com.riopapa.snaplog.Vars.mTextureView;
import static com.riopapa.snaplog.Vars.mWidth;
import static com.riopapa.snaplog.Vars.map_api_key;
import static com.riopapa.snaplog.Vars.now_time;
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
import static com.riopapa.snaplog.Vars.takePicture;
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
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.TextureView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private final static int VOICE_RECOGNISE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
            finish();
        }

        new FullScreen().set(this, Objects.requireNonNull(getSupportActionBar()));
        mTextureView = findViewById(R.id.textureView);
        utils = new Utils(this);

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
//        if (sharedAutoLoad) {
//            new PlaceRetrieve(mContext, oLatitude, oLongitude, placeType, pageToken, sharedRadius, byPlaceName);
//            new Timer().schedule(new TimerTask() {
//                public void run() {
//                    selectPlace();
//                }
//            }, 15000);
//        }

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
            cameraSub.close();
            cameraSub.open(mWidth, mHeight);
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
            if (cameraSub != null) {
                cameraSub.close();
                cameraSub = new CameraSub();
            }
            cameraSub.open(mTextureView.getWidth(), mTextureView.getHeight());
            takePicture = new TakePicture();
            deviceOrientation = new DeviceOrientation();
            showTypeAdaptor();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        cameraSub.close();
        stopBackgroundThread();
        super.onPause();
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mWidth = width;
            mHeight = height;
            cameraSub = new CameraSub();
            cameraSub.open(mWidth, mHeight);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            new ConfigureTransform().set(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };
    private void start_Setting() {
        Intent intent = new Intent(MainActivity.this, SetActivity.class);
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

    private static void showTypeAdaptor() {
        RecyclerView typeRecyclerView = mActivity.findViewById(R.id.type_recycler);
        int layoutOrientation = (cameraOrientation == 1) ?
                RecyclerView.VERTICAL : RecyclerView.HORIZONTAL;
        LinearLayoutManager mLinearLayoutManager
                = new LinearLayoutManager(mContext, layoutOrientation, false);
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
            if (exitFlag)
                exitHandler.sendEmptyMessage(0);
        } else {
            Toast.makeText(mContext, "Request Code:" + requestCode + ", Result Code:" + resultCode + " not as expected", Toast.LENGTH_LONG).show();
        }
    }
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    static void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cM = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo aNI = cM.getActiveNetworkInfo();
        return aNI != null && aNI.isConnected();
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

    public final static Handler orientationHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            cameraOrientation = msg.what;
            if (cameraOrientation == 1) {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            cameraSub.close();
            cameraSub.open(mWidth,mHeight);
//            showTypeAdaptor();
        }
    };

    private void take_Picture() {

        cameraOrientation = deviceOrientation.orientation;

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
            takePicture.shot(this);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "ERROR: CameraSub permissions not granted", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    private static void save_GoogleMap(Bitmap googleShot) {
        BuildBitMap buildBitMap = new BuildBitMap(googleShot, oLatitude, oLongitude, oAltitude, mActivity, mContext, cameraOrientation);
        buildBitMap.makeOutMap(strVoice, strPlace, strAddress, sharedWithPhoto, now_time, "Map");
    }

    public final static Handler exitHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            cameraSub.close();
            stopBackgroundThread();
            mActivity.finish();
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/gallery");
            intent.setAction(Intent.ACTION_PICK);
            mActivity.startActivity(intent);
            new Timer().schedule(new TimerTask() {
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }, 3000);   // wait while photo generated
        }
    };


}