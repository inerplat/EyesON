package com.osam2019.DreamCar.EyesON;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;


public class BluetoothDevicesAdapter extends ArrayAdapter<String> {


    public BluetoothDevicesAdapter(@NonNull Context context, ArrayList<String> devices) {
        super(context, 0, devices);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {



        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_device, parent, false);
        }

        ViewHolder holder = new ViewHolder();
        holder.deviceName = (TextView) convertView.findViewById(R.id.device_name_textView);
        convertView.setTag(holder);


        String deviceName = getItem(position);
        if (deviceName != null) {
            holder.deviceName.setText(deviceName);
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView deviceName;
    }
}
