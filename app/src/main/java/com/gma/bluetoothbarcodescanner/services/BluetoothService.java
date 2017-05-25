package com.gma.bluetoothbarcodescanner.services;

import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.gma.bluetoothbarcodescanner.activities.LaunchingActivity;
import com.gma.bluetoothbarcodescanner.interfaces.HandlerBluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;


public class BluetoothService{
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private Context mContext;
    private BluetoothDevice mDevice;
    private HandlerBluetooth mHandlerBluetooth;
    private Handler mHandler;

    private final BluetoothAdapter mBluetoothAdapter;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private boolean mManualDisconnect = false;

    public static final int BLUETOOTH_CONNECTION = 1;
    public static final int BLUETOOTH_CONNECTION_LOST = 2;
    public static final int BLUETOOTH_CONNECTION_FAILED = 3;
    public static final int BLUETOOTH_RECEIVE_CODE = 4;

    public BluetoothService(Context context) {
        mContext = context;
        mHandlerBluetooth = (LaunchingActivity) context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch( msg.what ) {
                    case BLUETOOTH_CONNECTION:
                        connected();
                        break;
                    case BLUETOOTH_CONNECTION_LOST:
                        connectionLost();
                        break;
                    case BLUETOOTH_CONNECTION_FAILED:
                        connectionFailed();
                        break;
                    case BLUETOOTH_RECEIVE_CODE:
                        receiveCode((String) msg.obj);
                        break;
                }
            }
        };
    }

    public synchronized Set<BluetoothDevice> getBondedScanners(){
        return mBluetoothAdapter.getBondedDevices();
    }

    public void connected() {
        mHandlerBluetooth.deviceConnectedEvent(true, mDevice.getName());
        Toast.makeText(mContext, "Bluetooth port connection established with " + mDevice.getName(), Toast.LENGTH_LONG).show();
    }

    public void connectionFailed() {
        mHandlerBluetooth.deviceConnectedEvent(false, mDevice.getName());
        Toast.makeText(mContext, "Bluetooth connection failed...", Toast.LENGTH_LONG).show();
    }

    public void connectionLost() {
        mHandlerBluetooth.deviceConnectedEvent(false, mDevice.getName());
        Toast.makeText(mContext, "Bluetooth connection lost...", Toast.LENGTH_LONG).show();
    }

    public void receiveCode(String data) {
        Toast.makeText(mContext, "Scanned: " + data, Toast.LENGTH_SHORT).show();
        mHandlerBluetooth.handleBarcode(data);
    }

    public synchronized void connect(BluetoothDevice device) {
        if(device.getUuids() == null){
            Toast.makeText(mContext, "This is not an SPP device...", Toast.LENGTH_LONG).show();
        } else {
            ParcelUuid uuid = device.getUuids()[0];
            if (uuid != null && uuid.getUuid().equals(MY_UUID)) {
                mDevice = device;
            } else {
                Toast.makeText(mContext, "This device is not a proper SPP scanner...", Toast.LENGTH_LONG).show();
            }
            if (mDevice != null) {
                mManualDisconnect = false;
                mConnectThread = new ConnectThread(mDevice);
                mConnectThread.start();
            }
        }
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    public synchronized void disconnect(boolean manual) {
        if (manual) mManualDisconnect = true;
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    public synchronized Boolean isConnectedAndRunning(){
        if (mConnectedThread == null) {
            return false;
        }
        return true;
    }

    //Connect to a UUID with a socket
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                mHandler.sendMessage(mHandler.obtainMessage(BLUETOOTH_CONNECTION_FAILED));
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    e.printStackTrace();
                }
                mHandler.sendMessage(mHandler.obtainMessage(BLUETOOTH_CONNECTION_FAILED));
                disconnect(false);
            }
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Start listening to the socket
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                mHandler.sendMessage(mHandler.obtainMessage(BLUETOOTH_CONNECTION_FAILED));
                disconnect(false);
                e.printStackTrace();
            }
            mmInStream = tmpIn;
        }

        public void run() {
            final int BUFFER_SIZE = 1024;
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes;

            synchronized (BluetoothService.this) {
                if(isConnectedAndRunning()) {
                    mHandler.sendMessage(mHandler.obtainMessage(BLUETOOTH_CONNECTION));
                }
            }

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String data = new String(buffer, 0, bytes);
                    mHandler.sendMessage(mHandler.obtainMessage(BLUETOOTH_RECEIVE_CODE, data));
                } catch (IOException e) {
                    if (!mManualDisconnect) {
                        mHandler.sendMessage(mHandler.obtainMessage(BLUETOOTH_CONNECTION_LOST));
                    }
                    disconnect(false);
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }



}
