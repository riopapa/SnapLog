package com.riopapa.snaplog;

import static com.riopapa.snaplog.GPSTracker.oLatitude;
import static com.riopapa.snaplog.GPSTracker.oLongitude;
import static com.riopapa.snaplog.Vars.NO_MORE_PAGE;
import static com.riopapa.snaplog.Vars.byPlaceName;
import static com.riopapa.snaplog.Vars.mContext;
import static com.riopapa.snaplog.Vars.nowDownLoading;
import static com.riopapa.snaplog.Vars.pageToken;
import static com.riopapa.snaplog.Vars.placeInfos;
import static com.riopapa.snaplog.Vars.placeType;
import static com.riopapa.snaplog.Vars.selectActivity;
import static com.riopapa.snaplog.Vars.sharedRadius;
import static com.riopapa.snaplog.Vars.sharedSortType;
import static com.riopapa.snaplog.Vars.utils;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

public class SelectActivity extends AppCompatActivity {

    static CountDownTimer waitTimer = null;
    RecyclerView placeRecycleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place);
        selectActivity = this;
        placeRecycleView = findViewById(R.id.place_recycler);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        placeRecycleView.setLayoutManager(mLinearLayoutManager);

        waitTimer = new CountDownTimer(20000, 200) {
            public void onTick(long millisUntilFinished) {
                if (!nowDownLoading) {
                    waitTimer.cancel();
                    if (!pageToken.equals(NO_MORE_PAGE)) {
                        new PlaceRetrieve(mContext, oLatitude, oLongitude, placeType, pageToken, sharedRadius, byPlaceName);
                        new Timer().schedule(new TimerTask() {
                            public void run() {
                            waitTimer.start();
                            }
                        }, 1000);
                    } else {
                        waitTimer.cancel();
                        if (sharedSortType.equals("name") && placeInfos.size() > 0)
                            placeInfos.sort(Comparator.comparing(arg0 -> arg0.oName));
                        else if (sharedSortType.equals("distance") && placeInfos.size() > 0)
                            placeInfos.sort(Comparator.comparing(arg0 -> arg0.distance));
                        String s = "Total "+placeInfos.size()+" places retrieved";
                        utils.log("LIST", s);
                        Toast.makeText(mContext,s, Toast.LENGTH_SHORT).show();
                        PlaceAdapter placeAdapter = new PlaceAdapter();
                        placeRecycleView.setAdapter(placeAdapter);
                    }
                }
            }
            public void onFinish() { waitTimer.cancel();}
        }.start();
    }
}
