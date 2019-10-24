package com.osam2019.DreamCar.EyesON;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.annotation.KeepName;
import com.google.firebase.perf.metrics.AddTrace;
import com.osam2019.DreamCar.EyesON.google.CameraSource;
import com.osam2019.DreamCar.EyesON.google.CameraSourcePreview;
import com.osam2019.DreamCar.EyesON.google.GraphicOverlay;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.osam2019.DreamCar.EyesON.BluetoothChooserActivity.EXTRA_DEVICE_ADDRESS;
import static com.osam2019.DreamCar.EyesON.BluetoothChooserActivity.REQUEST_ENABLE_BT;
import static com.osam2019.DreamCar.EyesON.FaceContourDetectorProcessor.LeftEyeOpenProbability;
import static com.osam2019.DreamCar.EyesON.FaceContourDetectorProcessor.RightEyeOpenProbability;
import static com.osam2019.DreamCar.EyesON.FaceContourDetectorProcessor.SmileProbability;
import static com.osam2019.DreamCar.EyesON.FaceContourGraphic.drowsinessTime;


@KeepName
public final class CameraPreviewActivity extends AppCompatActivity
        implements OnRequestPermissionsResultCallback,
        OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener {
    private static final String FACE_DETECTION = "Face Detection";
    private static final String FACE_CONTOUR = "Face Contour";
    private static final String TAG = "LivePreviewActivity";
    private static final int PERMISSION_REQUESTS = 1;

    private CameraSource cameraSource;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private String selectedModel = FACE_DETECTION;
    String address = null;

    private ProgressDialog progressDialog;
    BluetoothAdapter myBluetoothAdapter = null;
    static BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static byte[] readBuffer;
    public static int readBufferPosition;
    public static Thread workerThread = null;
    public static InputStream inputStream = null;
    private int newConnectionFlag = 0;

    public CameraPreviewActivity() {
        cameraSource = null;
    }

    @Override
    @AddTrace(name = "onCreateTrace")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_live_preview);
        Intent newIntent = getIntent();
        address = newIntent.getStringExtra(EXTRA_DEVICE_ADDRESS);

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!myBluetoothAdapter.isEnabled()) {
            Intent enableIntentBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntentBluetooth, REQUEST_ENABLE_BT);
        }


        preview = findViewById(R.id.firePreview);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.fireFaceOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        } else {
            getRuntimePermissions();
        }
        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
    }

    public boolean onCreateOptionsMenu(android.view.Menu menu){
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        Toast toast = Toast.makeText(getApplicationContext(),"", Toast.LENGTH_LONG);
        preview.stop();
        if(!address.equals("00:00:00:00:00:00")) workerThread.interrupt();
        switch(item.getItemId())
        {
            case R.id.CameraCheck:
                Log.d(TAG, "Set facing");
                if (cameraSource != null) {
                    if (cameraSource.getCameraFacing() == CameraSource.CAMERA_FACING_BACK) {
                        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
                    } else {
                        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
                    }
                }
                preview.stop();
                startCameraSource();
                break;
            case R.id.Contour:
                int status = FaceContourDetectorProcessor.getStatus();
                if(status == 0) {
                    createCameraSource(FACE_CONTOUR);
                    startCameraSource();
                    toast.setText("윤곽선 표시 설정");
                }
                else{
                    createCameraSource(FACE_DETECTION);
                    startCameraSource();
                    toast.setText("윤곽선 표시 해제");
                }
                break;
        }
        if(!address.equals("00:00:00:00:00:00")) receiveData();
        toast.show();

        return super.onOptionsItemSelected(item);
    }

    private void sendData(String data) {
        Log.d("bluetoothData", "send : "+data);
        if (btSocket != null && workerThread!=null && workerThread.isAlive()) {
            try {
                btSocket.getOutputStream().write(data.getBytes());
            } catch (IOException e) {
                Log.d(TAG, "Toast Error");
            }
        }
    }
    @SuppressLint("DefaultLocale")
    public void receiveData() {
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(() -> {
            StringBuilder str = new StringBuilder();
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    int byteAvailable = inputStream.available();
                    if(byteAvailable >= 2) {
                        byte[] bytes = new byte[byteAvailable];
                        inputStream.read(bytes);
                        str.append(new String(bytes, StandardCharsets.US_ASCII));
                        Log.d("bluetoothData", "received : " + str);
                        while(str.length() >= 2){
                            String subString = str.substring(0, 2);
                            if(subString.equals("!~")) {
                                sendData("@" + String.format("%.2f", LeftEyeOpenProbability).replace(".", "") + String.format("%.2f", RightEyeOpenProbability).replace(".", "") + "~");
                            }
                            else if (subString.equals("^~")) {
                                if (LeftEyeOpenProbability == -2 || RightEyeOpenProbability == -2)
                                    sendData("#~");
                                else
                                    sendData("%" + (SmileProbability >= 0.7 ? "2" : drowsinessTime > 600 ? "1" : "0") + "~");
                            }
                            str = new StringBuilder(str.length() > 2 ? str.substring(2) : "");
                            Log.d("bluetoothData", "now : " + str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        workerThread.start();
    }


    @Override
    public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

        selectedModel = parent.getItemAtPosition(pos).toString();
        Log.d(TAG, "Selected model: " + selectedModel);
        preview.stop();
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
            startCameraSource();
        } else {
            getRuntimePermissions();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Set facing");
        if (cameraSource != null) {
            if (!isChecked) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            } else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }
        }
        preview.stop();
        startCameraSource();
    }

    private void createCameraSource(String model) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        try {
            switch(model) {
                case FACE_CONTOUR:
                    Log.i(TAG, "Using Face Contour Detector Processor");
                    cameraSource.setMachineLearningFrameProcessor(new FaceContourDetectorProcessor("Contour"));
                    break;

                case FACE_DETECTION:
                    Log.i(TAG, "Using Face Detector Processor");
                    cameraSource.setMachineLearningFrameProcessor(new FaceContourDetectorProcessor("Detect"));
                    break;

            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + model, e);
            Toast.makeText(
                    getApplicationContext(),
                    "Can not create image processor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        newConnectionFlag++;
        if (address != null) {
            //call the class to connect to bluetooth
            if (newConnectionFlag == 1) {
                new ConnectBT().execute();
            }
        }
        startCameraSource();
    }


    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(workerThread!=null && workerThread.isAlive()) workerThread.interrupt();
        preview.stop();
        try {
            if(inputStream!=null)
                inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (cameraSource != null) {
            cameraSource.release();
        }
        Disconnect();
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

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
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

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
    @SuppressLint("StaticFieldLeak")
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean connectSuccess = true;

        @Override
        protected void onPreExecute() {

            //show a progress dialog
            progressDialog = ProgressDialog.show(CameraPreviewActivity.this,
                    "Connecting...", "Please wait!!!");
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                if(address.equals("00:00:00:00:00:00"))
                    return null;
                if (btSocket == null || !isBtConnected) {
                    myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice bluetoothDevice = myBluetoothAdapter.getRemoteDevice(address);
                    btSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                    inputStream = btSocket.getInputStream();
                    receiveData();
                }

            } catch (IOException e) {
                connectSuccess = false;
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.e(TAG, connectSuccess + "");
            if (!connectSuccess) {
                makeToast("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                isBtConnected = true;
                if(!address.equals("00:00:00:00:00:00")) makeToast("Connected");
                LeftEyeOpenProbability = -1;
                RightEyeOpenProbability = -1;
                sendData("$~");
            }
            progressDialog.dismiss();
        }
    }
    private void makeToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {
                makeToast("Error");
            }
        }
    }
}