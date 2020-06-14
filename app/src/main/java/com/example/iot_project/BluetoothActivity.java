package com.example.iot_project;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import com.example.iot_project.MyBluetoothService.ConnectedThread;

public class BluetoothActivity extends AppCompatActivity {
    Button goBackButton;
    Button sendButton;
    Button scanButton;
    TextView receivedMessageText;
    EditText sendMessageText;

    BluetoothAdapter bluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    private final static int REQUEST_ENABLE_BT = 1;
    UUID my_uuid = UUID.randomUUID();
    MyBluetoothService myBluetoothService;
    BluetoothDevice chosenDevice = null;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("-D-", "BroadcastReceiver: received  deviceName = " + deviceName
                        + " deviceHardwareAddress = " + deviceHardwareAddress);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w("-D-", "Creating the BluetoothActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        InitUI();
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver:
        unregisterReceiver(receiver);
    }

    private void InitUI() {
        Log.w("-D-", "BluetoothActivity.InitUI(): initializing UI");
        goBackButton = (Button) findViewById(R.id.go_back_button);
        sendButton = (Button) findViewById(R.id.send_button);
        scanButton = (Button) findViewById(R.id.scan_button);
        receivedMessageText = findViewById(R.id.received_msg_text);
        sendMessageText = findViewById(R.id.send_msg_text);

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothScanAndChoose();
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

    }

    public void bluetoothScanAndChoose(){
        checkBluetoothSupport();
        checkBluetoothEnabled();
        checkPairedDevices();
        chooseDevice();
        initializeConnection();
    }

    public void initializeConnection(){
        if (chosenDevice == null){
            Log.w("-D-", "BluetoothActivity.sendMessage(): must choose a target device by pressing 'scan' ");
            Toast.makeText(getApplicationContext(), "must choose a target device by pressing 'scan'", Toast.LENGTH_LONG).show();
            return;
        }
        ConnectedThread = new ConnectedThread();
    }

    public void sendMessage(){
        if (bluetoothAdapter == null){
            Log.w("-D-", "BluetoothActivity.sendMessage(): must enable bluetooth by pressing 'scan' ");
            Toast.makeText(getApplicationContext(), "must enable bluetooth by pressing 'scan'", Toast.LENGTH_LONG).show();
            return;
        }
        if (chosenDevice == null){
            Log.w("-D-", "BluetoothActivity.sendMessage(): must choose a target device by pressing 'scan' ");
            Toast.makeText(getApplicationContext(), "must choose a target device by pressing 'scan'", Toast.LENGTH_LONG).show();
            return;
        }

    }

    public void checkBluetoothSupport(){
        Log.w("-D-", "BluetoothActivity.checkBluetoothSupport(): checking that Bluetooth is supported");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth!", Toast.LENGTH_LONG).show();
        }
    }

    public void checkBluetoothEnabled(){
        Log.w("-D-", "BluetoothActivity.checkBluetoothEnabled(): checking that Bluetooth is enabled");
        if (!bluetoothAdapter.isEnabled()) {
            Log.w("-D-", "BluetoothActivity.checkBluetoothEnabled(): Bluetooth is not enabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        Log.w("-D-", "BluetoothActivity.checkBluetoothEnabled(): Bluetooth is enabled");
        Toast.makeText(getApplicationContext(), "Your Bluetooth is enabled. Yay!", Toast.LENGTH_LONG).show();
    }

    public void chooseDevice(){
        ArrayList<String> deviceInfoList = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            deviceInfoList.add(device.getAddress());
        }
        deviceInfoList.add("None");

        final String[] options = (String[]) deviceInfoList.toArray();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a bluetooth device");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.w("-D-", "BluetoothActivity.chooseDevice(): user clicked option " + which);
                // the user clicked on options[which]
                if (options[which].equals("None")) {
                    Log.w("-D-", "BluetoothActivity.chooseDevice(): No device was chosen");
                    Toast.makeText(getApplicationContext(), "No device was chosen", Toast.LENGTH_LONG).show();
                }
                else {
                    for (BluetoothDevice device : pairedDevices) {
                        if (options[which].equals(device.getAddress())){
                            chosenDevice = device;
                            Log.w("-D-", "BluetoothActivity.chooseDevice(): chosen " + device.toString());
                            return;
                        }
                    }
                }
            }
        });
        builder.show();
    }

    public void checkPairedDevices(){
        pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            Log.w("-D-", "BluetoothActivity.checkPairedDevices(): found paired device - "
                    + deviceName + "  " + deviceHardwareAddress);
        }
    }

    public void makeDiscoverable(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1800);
        startActivity(discoverableIntent);
    }

    private void manageMyConnectedSocket(BluetoothSocket socket) {

    }

    /*
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("AutonomicIOTproject", my_uuid);
            } catch (IOException e) {
                Log.e("-D-", "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run(){
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e("-D-", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e){
                        Log.e("-D-", "AcceptThread.run(): IOException occurred", e);
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("-D-", "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(my_uuid);
            } catch (IOException e) {
                Log.e("-D-", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("-D-", "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("-D-", "Could not close the client socket", e);
            }
        }
    }

     */

}
