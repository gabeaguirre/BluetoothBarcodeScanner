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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
    private ArrayList<Device> mDeviceArray;
    private ArrayList<String> mScanHistoryArray;
    private DeviceAdapter mDeviceAdapter;
    private ArrayAdapter<String> mScanHistoryAdapter;
    private ListView mListView;
    private ListView mScanHistoryListView;
    private BluetoothService mBluetoothService;

    private TextView mTextView;
    private TextView mScanHistoryTv;
    private TextView mConnectedToTv;
    private TextView mConnectedDeviceTv;
    private Button mDisconnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launching);

        mDeviceArray = new ArrayList<>();
        mDeviceAdapter = new DeviceAdapter(this, mDeviceArray);
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setAdapter(mDeviceAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mBluetoothService.connect(mDeviceAdapter.getItem(i).device);
            }
        });

        mScanHistoryArray = new ArrayList<>();
        mScanHistoryAdapter = new ArrayAdapter<>(this, R.layout.scanhistory, mScanHistoryArray);
        mScanHistoryListView = (ListView) findViewById(R.id.scanHistoryListView);
        mScanHistoryListView.setAdapter(mScanHistoryAdapter);
        mScanHistoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                displayBarcodeDialog(mScanHistoryAdapter.getItem(i).toString());
            }
        });

        mTextView = (TextView) findViewById(R.id.textView);
        mScanHistoryTv = (TextView) findViewById(R.id.scanHistoryTv);
        mConnectedToTv = (TextView) findViewById(R.id.connectedToTv);
        mConnectedDeviceTv = (TextView) findViewById(R.id.connectedDeviceTv);
        mDisconnectButton = (Button) findViewById(R.id.disconnectButton);
        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnectScanner();
            }
        });
        startBluetooth();
    }

    @Override
    protected void onResume() {
        System.out.println("RESUME IS RUNNING");
        initBluetooth();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.refreshOption) {
            initBluetooth();
            return true;
        }
        if(id == R.id.clearHistoryOption) {
            clearScanHistory();
        }
        return super.onOptionsItemSelected(item);
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
    public void handleBarcode(String barcode) {
        mScanHistoryArray.add(0, barcode);
        mScanHistoryAdapter.notifyDataSetChanged();
    }

    public void displayBarcodeDialog(final String barcode) {
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

    private void clearScanHistory() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Clear Scan History?")
                .setMessage("Are you sure you want to clear your Scan History?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mScanHistoryArray.clear();
                        mScanHistoryAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void disconnectScanner(){
        mBluetoothService.disconnect(true);
        mConnectedToTv.setVisibility(View.GONE);
        mConnectedDeviceTv.setVisibility(View.GONE);
        mDisconnectButton.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
        mTextView.setText("Paired Devices (select one to connect)");
        mScanHistoryTv.setVisibility(View.GONE);
        mScanHistoryListView.setVisibility(View.GONE);
    }

    @Override
    public void deviceConnectedEvent(boolean connected, String name) {
        if(connected) {
            mTextView.setText("Disconnect to pair to another device!");
            mConnectedToTv.setVisibility(View.VISIBLE);
            mConnectedDeviceTv.setVisibility(View.VISIBLE);
            mConnectedDeviceTv.setText(name);
            mDisconnectButton.setText("Disconnect from " + name);
            mDisconnectButton.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            mScanHistoryTv.setVisibility(View.VISIBLE);
            mScanHistoryListView.setVisibility(View.VISIBLE);
        } else {
            disconnectScanner();
        }
    }
}
