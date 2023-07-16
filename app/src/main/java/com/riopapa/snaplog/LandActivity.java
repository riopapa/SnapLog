package com.riopapa.snaplog;

import static com.riopapa.snaplog.GPSTracker.oLatitude;
import static com.riopapa.snaplog.GPSTracker.oLongitude;
import static com.riopapa.snaplog.Vars.googleShot;
import static com.riopapa.snaplog.Vars.zoomValue;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

    public class LandActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {

    GoogleMap mGoogleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_land);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.w("onMapReady ", "onMapReady");
        mGoogleMap = googleMap;
        LatLng here = new LatLng(oLatitude, oLongitude);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, zoomValue));
        mGoogleMap.addMarker(new MarkerOptions().position(here)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.my_face)));
//        int mapType = (terrain)? GoogleMap.MAP_TYPE_TERRAIN : GoogleMap.MAP_TYPE_NORMAL;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);  // 지형 포함
//        mGoogleMap.setTrafficEnabled(false);
        mGoogleMap.setOnMapLoadedCallback(this);  // wait till all map is displayed
    }

    @Override
    public void onMapLoaded() {     // if map is displayed then try snapshot
        mGoogleMap.snapshot(callback);
    }

    GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
        @Override
        public void onSnapshotReady(Bitmap snapshot) {
            googleShot = mergeScaleBitmap(snapshot, getScaleMap(zoomValue));
            finish();
        }
    };

    private Bitmap mergeScaleBitmap(Bitmap mapImage, Bitmap scaleMap) {

        Canvas canvas = new Canvas(mapImage);
        Paint paint = new Paint();
        canvas.drawBitmap(mapImage, 0, 0, paint);
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
        int xPos = mapImage.getWidth() - mapImage.getWidth() / 15 - scaleMap.getWidth();
        int yPos = mapImage.getHeight() - mapImage.getHeight() / 20 - scaleMap.getHeight();
        canvas.drawBitmap(scaleMap, xPos, yPos, paint);
        return mapImage;
    }


    private Bitmap getScaleMap(int zoom) {
        //      20,      19,     18,    17,     16,     15,     14,     13,     12,     11,     10,     9
        final int[] xWidths = {0, 113, 113, 90, 90, 90, 113, 113, 113, 90, 90, 90, 113,};
        final String[] xUnits = {"", "10 m", "20 m", "50 m", "100 m", "200 m", "500 m", "1 Km", "2 Km", "5 Km", "10 Km", "20 Km", "50 Km"};
        Bitmap bitmap = Bitmap.createBitmap(300, 80, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        int xWidth = (int) ((float) (xWidths[20 - zoom]) * 2.0f);   // if snapshot resolution changed 2.0f should be changed also
        int baseX = 10;
        int baseY = 60;
        int yHeight = 15;
        int[] xPos = {baseX, baseX, baseX + xWidth, baseX + xWidth, baseX + (xWidth / 2), baseX};
        int[] yPos = {baseY, baseY - yHeight, baseY - yHeight, baseY, baseY - yHeight, baseY};
        Path path = new Path();
        path.moveTo(baseX, baseY);
        for (int i = 0; i < xPos.length; i++) {
            path.lineTo(xPos[i], yPos[i]);
        }
        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(3f);
        paint.setAntiAlias(true);
        canvas.drawPath(path, paint);

        paint.setTextSize(36);
        paint.setStrokeWidth(20f);
        paint.setColor(Color.BLACK);
        String xUnit = xUnits[20 - zoom];
        canvas.drawText(xUnit, baseX + 20, baseY - yHeight - 10, paint);
        return bitmap;
    }
}
