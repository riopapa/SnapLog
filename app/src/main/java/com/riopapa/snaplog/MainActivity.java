package com.riopapa.snaplog;

import static com.riopapa.snaplog.GPSTracker.oAltitude;
import static com.riopapa.snaplog.GPSTracker.oLatitude;
import static com.riopapa.snaplog.GPSTracker.oLongitude;
import static com.riopapa.snaplog.Vars.NO_MORE_PAGE;
import static com.riopapa.snaplog.Vars.REQUEST_CAMERA_PERMISSION;
import static com.riopapa.snaplog.Vars.SAVE_MAP;
import static com.riopapa.snaplog.Vars.byPlaceName;
import static com.riopapa.snaplog.Vars.cameraSub;
import static com.riopapa.snaplog.Vars.currActivity;
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
import static com.riopapa.snaplog.Vars.pageToken;
import static com.riopapa.snaplog.Vars.placeInfos;
import static com.riopapa.snaplog.Vars.placeType;
import static com.riopapa.snaplog.Vars.sharedFace;
import static com.riopapa.snaplog.Vars.sharedLandscape;
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
import android.view.View;
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

        currActivity = this.getClass().getSimpleName();
        mActivity = this;
        mContext = getApplicationContext();
        utils = new Utils(this);
        utils.getPreference();
        setContentView(R.layout.activity_main);

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
        ImageView btnShotExit2 = findViewById(R.id.btnShotExit2);
        btnShotExit2.setOnClickListener(v -> {
            exitFlag = true;
            take_Picture();
        });

        new GPSTracker().get();

        ImageView mSpeak = findViewById(R.id.btnSpeak);
        mSpeak.setOnClickListener(v -> startGetVoice());

        ImageView mPlace = findViewById(R.id.btnPlace);
        mPlace.setOnClickListener(v -> placeHandler.sendEmptyMessage(0));

        mPlace.setImageBitmap(utils.maskedIcon(typeIcons[typeNumber]));

        if (!isNetworkAvailable()) {
            Toast.makeText(mContext, "No Network Available", Toast.LENGTH_LONG).show();
        }
        tvVoice.setText(sharedVoice);
        tvAddress.setText(sharedLocation);
        mTextureView = findViewById(R.id.textureView);
        mTextureView.post(() -> utils.deleteOldLogFiles());

        show_logo();

        ImageView ivLogo = findViewById(R.id.logo);
        ivLogo.setOnClickListener(view -> {
            sharedLogo = (++sharedLogo) % 3;  // logo count
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("logo", sharedLogo);
            editor.apply();
            show_logo();
        });

        ImageView ivMap = findViewById(R.id.include_map);
        float opacity = (sharedMap) ? 1f : 0.2f;
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
            editor.putInt("face",sharedFace);
            editor.apply();
            cameraSub.close();
            cameraSub.open(mWidth, mHeight);
        });

        ImageView ivLand = findViewById(R.id.rotate);
        ivLand.setImageResource((sharedLandscape) ? R.drawable.portrait: R.drawable.landscape);
        ivLand.setOnClickListener(view -> {
            sharedLandscape = !sharedLandscape;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("landscape",sharedLandscape);
            editor.apply();
            cameraSub.close();
            int temp = mHeight; mHeight = mWidth; mWidth = temp;
            cameraSub.open(mWidth, mHeight);
            orientationHandler.sendEmptyMessage(0);
            ivLand.setImageResource((sharedLandscape) ? R.drawable.portrait: R.drawable.landscape);
        });

        ImageView ivSet = findViewById(R.id.setting);
        ivSet.setOnClickListener(view -> start_Setting());
        orientationHandler.sendEmptyMessage(0);
    }

    static void getPlaceList() {
        ImageView mPlace = mActivity.findViewById(R.id.btnPlace);
        pageToken = NO_MORE_PAGE;
        placeInfos = new ArrayList<>();
        mPlace.setVisibility(View.INVISIBLE);
        String placeName = tvAddress.getText().toString();
        if (placeName.startsWith("?")) {
            String[] placeNames = placeName.split("\n");
            byPlaceName = placeNames[0].substring(1);
        } else
            byPlaceName = "";
        new PlaceRetrieve(mContext, oLatitude, oLongitude, placeType, pageToken, sharedRadius, byPlaceName);
        new Timer().schedule(new TimerTask() {
            public void run() {
                selectPlace();
                mActivity.runOnUiThread(() -> mPlace.setVisibility(View.VISIBLE));
            }
        }, 3000);
    }

    @Override
    protected void onResume() {
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            if (cameraSub != null) {
                cameraSub.close();
                cameraSub = new CameraSub();
            }
            cameraSub.open(mTextureView.getWidth(), mTextureView.getHeight());
            takePicture = new TakePicture();
//            deviceOrientation = new DeviceOrientation();
            showTypeAdaptor();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        cameraSub.close();
        stopBackgroundThread();
        super.onPause();
    }

    private static final TextureView.SurfaceTextureListener mSurfaceTextureListener
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

    static void selectPlace() {
        Intent intent = new Intent(mContext, SelectActivity.class);
        mActivity.startActivity(intent);
    }

    private void show_logo() {
        sigMap = utils.buildSignatureMap();
        ImageView iv = findViewById(R.id.logo);
        iv.setImageBitmap(sigMap);
    }

    private static void showTypeAdaptor() {
        RecyclerView typeRecyclerView = mActivity.findViewById(R.id.type_recycler);
        int layoutOrientation = (sharedLandscape) ?
                RecyclerView.VERTICAL : RecyclerView.HORIZONTAL;
        LinearLayoutManager mLinearLayoutManager
                = new LinearLayoutManager(mContext, layoutOrientation, false);
        typeRecyclerView.setLayoutManager(mLinearLayoutManager);
        typeAdapter = new TypeAdapter(typeInfos);
        typeRecyclerView.setAdapter(typeAdapter);
    }

    static void inflateAddress() {
        mActivity.runOnUiThread(() -> {
            tvAddress.setText(sharedLocation);
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
                galleryHandler.sendEmptyMessage(0);
        } else {
            Toast.makeText(mContext, "Request Code:" + requestCode + ", Result Code:" + resultCode + " not as expected", Toast.LENGTH_LONG).show();
        }
    }
    private void startBackgroundThread() {
        if (mBackgroundThread != null)
            stopBackgroundThread();
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

            mActivity.setRequestedOrientation((sharedLandscape) ?
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            cameraSub.close();
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
//
//            cameraSub.open(mWidth,mHeight);
        }
    };

    public final static Handler placeHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            getPlaceList();
        }
    };

    private void take_Picture() {

        ImageView btnShot = findViewById(R.id.btnShot);
        btnShot.setVisibility(View.INVISIBLE);
        ImageView btnShotE = findViewById(R.id.btnShotExit);
        btnShotE.setVisibility(View.INVISIBLE);
        ImageView btnShot2 = findViewById(R.id.btnShotExit2);
        btnShot2.setVisibility(View.INVISIBLE);

        new Timer().schedule(new TimerTask() {
            public void run() {
                mActivity.runOnUiThread(() -> {
                    btnShot.setVisibility(View.VISIBLE);
                    btnShotE.setVisibility(View.VISIBLE);
                    btnShot2.setVisibility(View.VISIBLE);
                });
            }
        }, 3000);   // wait while photo generated
        sharedLocation = tvAddress.getText().toString();

        String [] s = sharedLocation.split("\n");
        if (s.length > 1) {
            strPlace = s[0];
            strAddress = s[1];
        } else {
            strPlace = "";
            strAddress = s[0];
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
            abortHandler.sendEmptyMessage(44);
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
        BuildBitMap buildBitMap = new BuildBitMap(googleShot, oLatitude, oLongitude, oAltitude, mActivity, mContext);
        buildBitMap.makeOutMap(strVoice, strPlace, strAddress, sharedWithPhoto, "Map");
    }

    public final static Handler galleryHandler = new Handler(Looper.getMainLooper()) {
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

    public final static Handler abortHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            cameraSub.close();
            stopBackgroundThread();
            String abortMsg = "abort Snap Log " + msg.what;
            if (msg.what == 11)
                abortMsg = "CameraSub : createCameraPreviewSession failed";
            else if (msg.what == 22)
                abortMsg = "CameraSub : CameraAccessException";
            else if (msg.what == 33)
                abortMsg = "TakePicture : Configuration Change Failed";
            else if (msg.what == 44)
                abortMsg = "takePicture : not started, CameraAccessException";
            else if (msg.what == 55)
                abortMsg = "CameraSub : camera open Exception";

            utils.log("abort", abortMsg);
            Toast.makeText(mContext," abort # "+abortMsg, Toast.LENGTH_LONG).show();
            mActivity.finish();
            new Timer().schedule(new TimerTask() {
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }, 3000);   // wait while photo generated
        }
    };

}