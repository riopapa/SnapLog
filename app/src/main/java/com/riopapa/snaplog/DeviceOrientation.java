package com.riopapa.snaplog;

import static com.riopapa.snaplog.MainActivity.orientationHandler;
import static com.riopapa.snaplog.Vars.mContext;

import android.hardware.SensorManager;
import android.util.Log;
import android.view.OrientationEventListener;

public class DeviceOrientation {

    OrientationEventListener orientEventListener;
    public int orientation;
    public DeviceOrientation()  {

        orientEventListener = new OrientationEventListener(mContext,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int arg0) {
                int newOrientation = -1;
                if (arg0 > 300 || arg0 < 60)
                    newOrientation = 6;
                else if (arg0 > 210 && arg0 < 300) {
                    newOrientation = 1;
                } else
                    return;
                if (newOrientation > 0 && newOrientation != orientation) {
                    orientation = newOrientation;
                    orientationHandler.sendEmptyMessage(orientation);
                }
            }
        };
        if (orientEventListener.canDetectOrientation()) {
            orientEventListener.enable();
            Log.w("onOrientationChanged", "enabled");
        }
    }
}