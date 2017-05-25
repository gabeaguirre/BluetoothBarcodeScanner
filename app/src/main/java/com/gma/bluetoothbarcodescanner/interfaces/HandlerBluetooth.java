package com.gma.bluetoothbarcodescanner.interfaces;

public interface HandlerBluetooth {

    void handleBarcode(String barcode);

    void deviceConnectedEvent(boolean connected, String name);
}
