package com.riopapa.snaplog;

import static com.riopapa.snaplog.Vars.exitFlag;
import static com.riopapa.snaplog.Vars.strVoice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.util.Timer;
import java.util.TimerTask;

public class ExitApp {

    private void check(Context context) {
//        startCamera();
        strVoice = "";
        if (exitFlag) {
            ((Activity) context).finish();
            Intent intent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/gallery");
            intent.setAction(Intent.ACTION_PICK);
            context.startActivity(intent);
            new Timer().schedule(new TimerTask() {
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }
            }, 3000);   // wait while photo generated
        }
    }

}
