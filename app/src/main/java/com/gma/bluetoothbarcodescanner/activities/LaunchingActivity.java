package com.gma.bluetoothbarcodescanner.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gma.bluetoothbarcodescanner.R;
import com.gma.bluetoothbarcodescanner.adapters.DeviceAdapter;
import com.gma.bluetoothbarcodescanner.interfaces.HandlerBluetooth;
import com.gma.bluetoothbarcodescanner.models.Device;
import com.gma.bluetoothbarcodescanner.services.BluetoothService;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Set;

public class LaunchingActivity extends AppCompatActivity implements HandlerBluetooth{

    private static final int REQUEST_ENABLE_BT = 1;
    private Set<BluetoothDevice> mDevicesSet;
    private BluetoothDevice[] mDevicesArray;
    private ArrayList<Device> mDeviceStatusArray;
    private DeviceAdapter mDeviceAdapter;
    private ListView mListView;
    private BluetoothService mBluetoothService;

    private TextView mConnectedToTv;
    private TextView mConnectedDeviceTv;
    private Button mDisconnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launching);

        mDeviceStatusArray = new ArrayList<>();
        mDeviceAdapter = new DeviceAdapter(this, mDeviceStatusArray);
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setAdapter(mDeviceAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mBluetoothService.connect(mDeviceAdapter.getItem(i).device);
            }
        });
        mConnectedToTv = (TextView) findViewById(R.id.connectedToTv);
        mConnectedDeviceTv = (TextView) findViewById(R.id.connectedDeviceTv);
        mDisconnectButton = (Button) findViewById(R.id.disconnectButton);
        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothService.disconnect(true);
                mConnectedToTv.setVisibility(View.GONE);
                mConnectedDeviceTv.setVisibility(View.GONE);
                mDisconnectButton.setVisibility(View.GONE);
            }
        });
        startBluetooth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                initBluetooth();
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
            initBluetooth();
        }
    }

    public void initBluetooth(){
        if (mBluetoothService == null) {
            mBluetoothService = new BluetoothService(this);
            mDevicesSet = mBluetoothService.getBondedScanners();
            mDevicesArray = mDevicesSet.toArray(new BluetoothDevice[mDevicesSet.size()]);
            if(mDevicesSet != null){
                for (BluetoothDevice device: mDevicesSet) {
                    Device theDevice = new Device(device);
                    mDeviceAdapter.add(theDevice);
                }
                mDeviceAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void handleBarcode(final String barcode) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Barcode read: " + barcode)
                .setMessage("Would you like to do a search on this barcode?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/#q=" + barcode)));
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void deviceConnectedEvent(String name) {
        mConnectedToTv.setVisibility(View.VISIBLE);
        mConnectedDeviceTv.setVisibility(View.VISIBLE);
        mConnectedDeviceTv.setText(name);
        mDisconnectButton.setText("Disconnect from " + name);
        mDisconnectButton.setVisibility(View.VISIBLE);
    }
}
