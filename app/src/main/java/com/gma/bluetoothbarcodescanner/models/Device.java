package com.gma.bluetoothbarcodescanner.models;

import android.bluetooth.BluetoothDevice;

public class Device {
    public BluetoothDevice device;
    public int index;

    public Device(BluetoothDevice device, int index) {
        this.device = device;
        this.index = index;
    }
}
