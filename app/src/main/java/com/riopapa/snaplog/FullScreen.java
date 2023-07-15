package com.riopapa.snaplog;

import android.app.Activity;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import androidx.appcompat.app.ActionBar;

public class FullScreen {
    public void set(Activity activity, ActionBar ab) {
        ab.hide();
        WindowInsetsController controller = activity.getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
//        }
//        controller.hide(WindowInsets.Type.statusBars() |
//                WindowInsets.Type.navigationBars());
    }

}
