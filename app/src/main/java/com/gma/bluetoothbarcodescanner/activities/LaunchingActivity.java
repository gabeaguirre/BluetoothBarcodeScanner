package com.gma.bluetoothbarcodescanner.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.gma.bluetoothbarcodescanner.R;
import com.gma.bluetoothbarcodescanner.services.BluetoothService;

import java.util.Set;

public class LaunchingActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    Set<BluetoothDevice> mDevicesSet;
    BluetoothDevice[] mDevicesArray;
    ArrayAdapter<String> mDeviceAdapter;
    ListView mListView;
    BluetoothService mBluetoothService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launching);

        mDeviceAdapter = new ArrayAdapter<String>(this, R.layout.listview);
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setAdapter(mDeviceAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mBluetoothService.connect(mDevicesArray[i]);
            }
        });

        startBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                if (mBluetoothService == null) {
                    mBluetoothService = new BluetoothService(this);
                    mDevicesSet = mBluetoothService.getBondedScanners();
                    mDevicesArray = mDevicesSet.toArray(new BluetoothDevice[mDevicesSet.size()]);
                    if (mDevicesSet != null) {
                        for (BluetoothDevice device : mDevicesSet) {
                            mDeviceAdapter.add(device.getName());
                        }
                        mDeviceAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    public void startBluetooth(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "bluetooth is not available", Toast.LENGTH_LONG).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mBluetoothService == null) {
                mBluetoothService = new BluetoothService(this);
                mDevicesSet = mBluetoothService.getBondedScanners();
                mDevicesArray = mDevicesSet.toArray(new BluetoothDevice[mDevicesSet.size()]);
                if(mDevicesSet != null){
                    for (BluetoothDevice device: mDevicesSet) {
                        mDeviceAdapter.add(device.getName());
                    }
                    mDeviceAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
