package com.gma.bluetoothbarcodescanner.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gma.bluetoothbarcodescanner.R;
import com.gma.bluetoothbarcodescanner.models.Device;

import java.util.ArrayList;

public class DeviceAdapter extends ArrayAdapter<Device> {
    private ArrayList<Device> mDevices;
    public DeviceAdapter(Context context, ArrayList<Device> devices) {
        super(context, 0, devices);
        mDevices = devices;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Device device = getItem(position);

        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview, parent, false);
        }

        TextView deviceName = (TextView) convertView.findViewById(R.id.deviceNameTv);

        deviceName.setText(device.device.getName());
        return convertView;
    }
}
