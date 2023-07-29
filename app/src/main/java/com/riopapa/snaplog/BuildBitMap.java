package com.riopapa.snaplog;

import static com.riopapa.snaplog.Vars.directory;
import static com.riopapa.snaplog.Vars.photo_time;
import static com.riopapa.snaplog.Vars.sharedAlpha;
import static com.riopapa.snaplog.Vars.sharedLandscape;
import static com.riopapa.snaplog.Vars.sigMap;
import static com.riopapa.snaplog.Vars.snap_time;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

class BuildBitMap {

    private static final SimpleDateFormat sdfExif = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.KOREA);
    private final SimpleDateFormat sdfFileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
    String phonePrefix = "";
    String sFood, sPlace, sAddress;
    double latitude, longitude, altitude;
    Bitmap outBitmap;
    Activity activity;
    Context context;

    public BuildBitMap(Bitmap outBitmap, double latitude, double longitude, double altitude, Activity activity, Context context) {
        this.latitude = latitude; this.longitude = longitude; this.altitude = altitude;
        this.outBitmap = outBitmap;
        this.activity = activity;this.context = context;
    }

    void makeOutMap(String sFood, String sName, String sAddress, boolean withPhoto, String suffix) {
        this.sFood = sFood; this.sPlace = sName; this.sAddress = sAddress;
        int width = outBitmap.getWidth();
        int height = outBitmap.getHeight();
        if (!sharedLandscape && width > height)
            outBitmap = rotateBitMap(outBitmap, 90);
        if (sharedLandscape && width < height)
            outBitmap = rotateBitMap(outBitmap, 90);

        if (withPhoto && suffix.length() == 0) {    // no suffix
            String outFileName = sdfFileName.format(photo_time) + suffix;
            File newFile = new File(directory, phonePrefix + outFileName + ".jpg");
            writeCameraFile(outBitmap, newFile);
            setNewFileExif(newFile);
//            activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile)));
        }

        Bitmap mergedMap = markDateLocSignature(outBitmap, photo_time, suffix);
        String foodName = sFood.trim();
        if (foodName.length() > 2)
            foodName = "(" + foodName +")";
        String outFileName2 = sdfFileName.format(snap_time) + "_" + sPlace + foodName;
        File newFile2 = new File(directory, phonePrefix + outFileName2 + suffix + "_ha.jpg");
        writeCameraFile(mergedMap, newFile2);
        setNewFileExif(newFile2);
//        activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(newFile2)));
    }

    private void setNewFileExif(File fileHa) {
        ExifInterface exifHa;

        try {
            exifHa = new ExifInterface(fileHa.getAbsolutePath());
            exifHa.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER);
            exifHa.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL);
            exifHa.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertGPS2DMS(latitude));
            exifHa.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitudeGPS2DMS(latitude));
            exifHa.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertGPS2DMS(longitude));
            exifHa.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitudeGPS2DMS(longitude));
            exifHa.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, convertALT2DMS(altitude));
            exifHa.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, (altitude> 0)? "0":"1");
            exifHa.setAttribute(ExifInterface.TAG_ORIENTATION, "1");
            exifHa.setAttribute(ExifInterface.TAG_DATETIME, sdfExif.format(photo_time));
            exifHa.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "Save Photo by riopapa");
            exifHa.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String latitudeGPS2DMS(double latitude) {
        return latitude < 0.0d ? "S" : "N";
    }

    private String longitudeGPS2DMS(double longitude) {
        return longitude < 0.0d ? "W" : "E";
    }

    private static String convertGPS2DMS(double latitude) {
        latitude = Math.abs(latitude);
        int degree = (int) latitude;
        latitude *= 60;
        latitude -= (degree * 60.0d);
        int minute = (int) latitude;
        latitude *= 60;
        latitude -= (minute * 60.0d);
        int second = (int) (latitude * 10000.d);
        return degree + "/1," + minute + "/1," + second + "/10000";
    }

    private static String convertALT2DMS(double altitude) {
        return ""+((altitude > 0) ? altitude:-altitude);
    }

    Bitmap markDateLocSignature(Bitmap photoMap, long timeStamp, String suffix) {
        int photoWidth = photoMap.getWidth();
        int photoHeight = photoMap.getHeight();
        Bitmap newMap = Bitmap.createBitmap(photoWidth, photoHeight, photoMap.getConfig());
        Canvas canvas = new Canvas(newMap);
        canvas.drawBitmap(photoMap, 0f, 0f, null);
        markDateTime(timeStamp, photoWidth, photoHeight, canvas);
        if (suffix.length() == 0)
            markSignature(photoWidth, photoHeight, canvas);
        markFoodPlaceAddress(photoWidth, photoHeight, canvas);
        return newMap;
    }

    private void markFoodPlaceAddress(int width, int height, Canvas canvas) {

        int xPos = width / 2;
        int fontSize = (height + width) / 80;  // gps
        int yPos = height - fontSize;
        if (width < height)
            yPos -= fontSize;
        yPos = drawTextOnCanvas(canvas, sAddress, fontSize, xPos, yPos);
        fontSize = fontSize * 12 / 10;  // Place
        yPos -= fontSize + fontSize / 4;
        yPos = drawTextOnCanvas(canvas, sPlace, fontSize, xPos, yPos);
        yPos -= fontSize + fontSize / 4; // food
        drawTextOnCanvas(canvas, sFood, fontSize, xPos, yPos);
    }

    private void markDateTime(long timeStamp, int width, int height, Canvas canvas) {
        final SimpleDateFormat sdfDate = new SimpleDateFormat("`yy/MM/dd", Locale.KOREA);
        final SimpleDateFormat sdfHourMin = new SimpleDateFormat("HH:mm(EEE)", Locale.KOREA);
        int fontSize = (width>height) ? (width+height)/65 : (width+height)/80;  // date time
        String s = sdfDate.format(timeStamp);
        int xPos = (width>height) ? width/9+fontSize: width/6+fontSize;
        int yPos = (width>height) ? height/10: height/11;
        drawTextOnCanvas(canvas, s, fontSize, xPos, yPos);
        yPos += fontSize * 13 / 10;
        s = sdfHourMin.format(timeStamp);
        drawTextOnCanvas(canvas, s, fontSize, xPos, yPos);
    }

    private  void markSignature(int width, int height, Canvas canvas) {
        int sigSizeX = sigMap.getWidth();
        int xPos = width - sigSizeX - ((width > height)? sigSizeX / 2 : 0);
        int yPos = sigSizeX / 2;
        Paint paint = new Paint(); paint.setAlpha(Integer.parseInt(sharedAlpha));
        canvas.drawBitmap(sigMap, xPos, yPos, null);
    }

    private int drawTextOnCanvas(Canvas canvas, String text, int fontSize, int xPos, int yPos) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(fontSize);
        paint.setTextAlign(Paint.Align.CENTER);
        int cWidth = canvas.getWidth() * 3 / 4;
        float tWidth = paint.measureText(text);
        int pos;
        if (tWidth > cWidth) {
            int length = text.length() / 2;
            for (pos = length; pos < text.length(); pos++)
                if (text.startsWith(" ", pos))
                    break;
            String text1 = text.substring(pos);
            drawOutLinedText(canvas, text1, xPos, yPos, fontSize);
            yPos -= fontSize + fontSize / 4;
            text1 = text.substring(0, pos);
            drawOutLinedText(canvas, text1, xPos, yPos, fontSize);
            return yPos;
        }
        else
            drawOutLinedText(canvas, text, xPos, yPos, fontSize);
        return yPos;
    }

    private void drawOutLinedText(Canvas canvas, String text, int xPos, int yPos, int textSize) {

        int color = ContextCompat.getColor(context, R.color.infoColor);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setTextSize(textSize);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth((float)textSize/5+3);
        paint.setTypeface(context.getResources().getFont(R.font.the_jamsil));
        canvas.drawText(text, xPos, yPos, paint);

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(text, xPos, yPos, paint);
    }

    private void writeCameraFile(Bitmap bitmap, File file) {

        FileOutputStream os;
        try {
            os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.close();
            activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        } catch (IOException e) {
            Log.e("ioException", e.toString());
            Toast.makeText(context, e.toString(),Toast.LENGTH_LONG).show();
        }
    }


    Bitmap rotateBitMap(Bitmap bitmap, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

}
