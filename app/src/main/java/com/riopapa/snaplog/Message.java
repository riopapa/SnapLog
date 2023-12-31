package com.riopapa.snaplog;

import static com.riopapa.snaplog.Vars.mActivity;
import static com.riopapa.snaplog.Vars.mContext;

import android.widget.Toast;

public class Message {
    void show(final String text) {
        mActivity.runOnUiThread(() -> Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show());
    }
}
