package com.riopapa.snaplog;

import static com.riopapa.snaplog.Vars.iconNames;
import static com.riopapa.snaplog.Vars.iconRaws;
import static com.riopapa.snaplog.Vars.mContext;
import static com.riopapa.snaplog.Vars.placeInfos;
import static com.riopapa.snaplog.Vars.selectActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static com.riopapa.snaplog.GPSTracker.oLatitude;
import static com.riopapa.snaplog.GPSTracker.oLongitude;
import static com.riopapa.snaplog.Vars.sharedLocation;
import static com.riopapa.snaplog.Vars.strAddress;
import static com.riopapa.snaplog.Vars.strPlace;
import static com.riopapa.snaplog.Vars.isPlaceNull;
import static com.riopapa.snaplog.Vars.utils;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceHolder>  {

    static class PlaceHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvAddress;
        ImageView ivIcon;
        View viewLine;

        PlaceHolder(View view) {
            super(view);
            this.viewLine = itemView.findViewById(R.id.place_layout);
            this.tvName = itemView.findViewById(R.id.recycler_PlaceName);
            this.tvAddress = itemView.findViewById(R.id.recycler_PlaceAddress);
            this.ivIcon = itemView.findViewById(R.id.recycler_icon);
            this.viewLine.setOnClickListener(view1 -> {
                int idx = getAbsoluteAdapterPosition();
                strPlace = placeInfos.get(idx).oName;
                strAddress = placeInfos.get(idx).oAddress;
                sharedLocation = strPlace + "\n" + strAddress;
                utils.putPlacePreference();
                oLatitude = Double.parseDouble(placeInfos.get(idx).oLat);
                oLongitude = Double.parseDouble(placeInfos.get(idx).oLng);
                MainActivity.inflateAddress();
                isPlaceNull = false;
                selectActivity.finish();
            });
        }
    }

    @NonNull
    @Override
    public PlaceHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.place_item, viewGroup, false);
        return new PlaceHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceHolder viewHolder, int position) {

        int textColor = 0xFF000000;
        int icon = getIconRaw(placeInfos.get(position).oIcon);
        if (icon == -1) {
            textColor = 0xFFFF0000;
            String s = placeInfos.get(position).oIcon;
            utils.log("icon error "+s,"https://maps.gstatic.com/mapfiles/place_api/icons/v1/png_71/"+s+"-71.png");
            Toast.makeText(mContext,"UnKnown Icon ["+s+"]",Toast.LENGTH_LONG).show();
            icon = iconRaws[0];
            placeInfos.get(position).setoName(placeInfos.get(position).oName+" "+s);
        }
        viewHolder.tvName.setText(placeInfos.get(position).oName);
        viewHolder.tvName.setTextColor(textColor);
        viewHolder.tvAddress.setText(placeInfos.get(position).oAddress);
        viewHolder.tvAddress.setTextColor(textColor);
        viewHolder.ivIcon.setImageResource(icon);
    }

    private int getIconRaw(String s) {
        for (int i = 0; i < iconNames.length; i++) {
            if (s.equals(iconNames[i]))
                return iconRaws[i];
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return (placeInfos == null) ? 0 : placeInfos.size();
    }

}
