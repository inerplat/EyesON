package com.osam2019.DreamCar.EyesON;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothChooserActivity extends AppCompatActivity {
    private static final String TAG = "DeviceActivity";
    private FloatingActionButton searchForNewDevices;
    private ListView DevicesList;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_ENABLE_FINE_LOCATION = 1256;
    private static final int PERMISSION_REQUESTS = 1;
    private BluetoothDevicesAdapter DeviceAdapter;
    private ArrayList<String> DeviceActivityNamesList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices_chooser);
        initializeScreen();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        DeviceAdapter = new BluetoothDevicesAdapter(this, DeviceActivityNamesList);

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.does_not_have_bluetooth, Toast.LENGTH_LONG).show();
            finish();
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntentBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntentBluetooth, REQUEST_ENABLE_BT);
        } else if (mBluetoothAdapter.isEnabled()) {
            PairedDevicesList();
        }

        setBroadCastReceiver();

        searchForNewDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchForNewDevices.setEnabled(false);
                DeviceAdapter.clear();
                PairedDevicesList();
                NewDevicesList();
            }
        });
        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
    }
    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }
    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }
    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ENABLE_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Access Location must be allowed for bluetooth Search", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                PairedDevicesList();
            } else {
                finish();
            }
        }
    }

    private void initializeScreen() {
        searchForNewDevices = (FloatingActionButton) findViewById(R.id.search_fab_button);
        DevicesList = (ListView) findViewById(R.id.devices_list_listView);
    }

    private void setBroadCastReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private void NewDevicesList() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

                    DeviceAdapter.add(device.getName() + "\n" + device.getAddress());
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                searchForNewDevices.setEnabled(true);
            }
        }
    };

    private void PairedDevicesList() {
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        DeviceAdapter.add("Continue without pairing\n"+"00:00:00:00:00:00");
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                DeviceAdapter.add(bt.getName() + "\n" + bt.getAddress());
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.no_paired_devices,
                    Toast.LENGTH_LONG).show();
        }

        DevicesList.setAdapter(DeviceAdapter);
        DevicesList.setOnItemClickListener(bluetoothListClickListener);
    }

    private AdapterView.OnItemClickListener bluetoothListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String info = (String) parent.getItemAtPosition(position);
            String MACAddress = info.substring(info.length() - 17);
            Log.d("CLICK", info+" | "+MACAddress);
            Intent intent = new Intent(BluetoothChooserActivity.this, CameraPreviewActivity.class);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, MACAddress);
            startActivity(intent);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        unregisterReceiver(mReceiver);
    }
}
