package com.riopapa.snaplog;

import static com.riopapa.snaplog.GPSTracker.oAltitude;
import static com.riopapa.snaplog.GPSTracker.oLatitude;
import static com.riopapa.snaplog.GPSTracker.oLongitude;
import static com.riopapa.snaplog.MainActivity.abortHandler;
import static com.riopapa.snaplog.MainActivity.galleryHandler;
import static com.riopapa.snaplog.Vars.SAVE_MAP;
import static com.riopapa.snaplog.Vars.exitFlag;
import static com.riopapa.snaplog.Vars.googleShot;
import static com.riopapa.snaplog.Vars.mActivity;
import static com.riopapa.snaplog.Vars.mBackgroundHandler;
import static com.riopapa.snaplog.Vars.mCameraDevice;
import static com.riopapa.snaplog.Vars.mContext;
import static com.riopapa.snaplog.Vars.mTextureView;
import static com.riopapa.snaplog.Vars.now_time;
import static com.riopapa.snaplog.Vars.sharedFace;
import static com.riopapa.snaplog.Vars.sharedLandscape;
import static com.riopapa.snaplog.Vars.sharedMap;
import static com.riopapa.snaplog.Vars.sharedWithPhoto;
import static com.riopapa.snaplog.Vars.strAddress;
import static com.riopapa.snaplog.Vars.strPlace;
import static com.riopapa.snaplog.Vars.strVoice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class TakePicture {

    Context tContext;
    void shot(Context context) throws CameraAccessException {
        if(mCameraDevice==null)
            return;
        tContext = context;

        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
        Size[] jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

        int width = 640;
        int height = 480;

        for (Size sz: jpegSizes) {  // find out max size with 16:9
            float ratio = (float) sz.getHeight() / (float) sz.getWidth();
            if (ratio > 1.7 && ratio < 1.8) {
                width = sz.getWidth();
                height = sz.getHeight();
                break;
            } else if (ratio < 1/1.7 && ratio > 1/1.8) {
                width = sz.getWidth();
                height = sz.getHeight();
                break;
            }
        }
//
//        cameraOrientation = deviceOrientation.orientation;

        ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        List<Surface> outputSurfaces = new ArrayList<>(2);
        outputSurfaces.add(reader.getSurface());

        outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

        final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        ImageReader.OnImageAvailableListener readerListener = reader1 -> {
            Image image = reader1.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray( bytes, 0, bytes.length ) ;
            image.close();
            now_time = System.currentTimeMillis();
            if (sharedFace == CameraCharacteristics.LENS_FACING_BACK && !sharedLandscape) {
                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(180);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, false);
            }

            BuildBitMap buildBitMap = new BuildBitMap(bitmap, oLatitude, oLongitude, oAltitude, mActivity, mContext);

            buildBitMap.makeOutMap(strVoice, strPlace, strAddress, sharedWithPhoto, now_time,"");
            strVoice = "";
            if (sharedMap) {
                googleShot = null;
                Intent intent = new Intent(mContext, LandActivity.class);
                intent.putExtra("lan", oLatitude);
                intent.putExtra("lon", oLongitude);
                intent.putExtra("alt", oAltitude);
                intent.putExtra("zoom", 15);
                ((Activity) tContext).startActivityForResult(intent, SAVE_MAP);
            } else if (exitFlag)
                galleryHandler.sendEmptyMessage(0);
        };

        reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

        final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
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
                abortHandler.sendEmptyMessage(33);
            }
        }, mBackgroundHandler);
    }
}
