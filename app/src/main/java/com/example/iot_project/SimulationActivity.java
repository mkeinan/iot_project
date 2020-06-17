package com.example.iot_project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class SimulationActivity extends AppCompatActivity implements Handler.Callback {

    Button scanButton;

    BluetoothAdapter bluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    private final static int REQUEST_ENABLE_BT = 1;
    //    UUID my_uuid = UUID.randomUUID();
    UUID my_uuid;
    //    MyBluetoothService myBluetoothService;
    BluetoothDevice chosenDevice = null;

    private static final String TAG = "MY_APP_DEBUG_TAG";
    private Handler handler = new Handler(this); // handler that gets info from Bluetooth service

    SimulationActivity.ConnectedThread connectedThread;
    SimulationActivity.ConnectThread connectThread;

    Integer receivedMessagesCount = 0;
    StringBuilder incomingMessage = new StringBuilder();

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

    Button runButton;
    Button backToMainButton;
    Button toggleDebugInfoButton;
    Button answerMyselfButton;
    TextView mapText;
    TextView debugInfoText;

//    Graph graph;
    Robot myRobot;
    Wrapper myWrapper;

    String lastSentMessage = "";
    boolean runningOffline = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation);
        InitUI();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        myRobot = new Robot();
        myWrapper = new Wrapper(myRobot);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver:
        unregisterReceiver(receiver);
        terminateSocketConnection();
    }

    private void InitUI() {
        Log.w("-D-", "CreateMapActivity.InitUI(): initializing UI. TID = " + Thread.currentThread().getId());
        runButton = (Button) findViewById(R.id.button_run_simulation);
        scanButton = (Button) findViewById(R.id.simulation_scan_button);
        backToMainButton = (Button) findViewById(R.id.button_go_back_to_main);
        toggleDebugInfoButton = (Button) findViewById(R.id.simulation_debug_info_button);
        answerMyselfButton = (Button) findViewById(R.id.simulation_answer_myself_button);
        mapText = (TextView) findViewById(R.id.text_simulation_map);
        debugInfoText = (TextView) findViewById(R.id.text_debug_info);

        backToMainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        });

        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectedThread == null){
                    AlertDialog.Builder builder = new AlertDialog.Builder(SimulationActivity.this);
                    builder.setTitle("No Bluetooth Connection...");
                    builder.setMessage("Do you want to run the simulation offline? \nYou will have to answer by yourself if you do...");
                    builder.setPositiveButton("Yes, run simulation offline", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User clicked OK button
                            runningOffline = true;
                            runSimulation();
                        }
                    });
                    builder.setNegativeButton("No, cancel simulation", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                            runningOffline = false;
                        }
                    });
                    builder.show();
                } else {
                    runSimulation();
                }
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothScanAndChoose();
            }
        });

        toggleDebugInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (debugInfoText.getVisibility() == View.VISIBLE)
                    debugInfoText.setVisibility(View.INVISIBLE);
                else
                    debugInfoText.setVisibility(View.VISIBLE);
            }
        });

        answerMyselfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] options = {Responses.SUCCESS, Responses.OBSTACLE_DETECTED};
                AlertDialog.Builder builder = new AlertDialog.Builder(SimulationActivity.this);
                builder.setTitle("send to yourself:");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the user clicked on options[which]
                        if (options[which].equals(Responses.SUCCESS)) {
                            Message readMsg = handler.obtainMessage(
                                    SimulationActivity.MessageConstants.MESSAGE_READ,
                                    Responses.SUCCESS.getBytes().length,
                                    -1,
                                    Responses.SUCCESS.getBytes().clone());  // send a clone to not interfere with the original buffer
                            readMsg.sendToTarget();
                        }
                        if (options[which].equals(Responses.OBSTACLE_DETECTED)) {
                            Message readMsg = handler.obtainMessage(
                                    SimulationActivity.MessageConstants.MESSAGE_READ,
                                    Responses.OBSTACLE_DETECTED.getBytes().length,
                                    -1,
                                    Responses.OBSTACLE_DETECTED.getBytes().clone());  // send a clone to not interfere with the original buffer
                            readMsg.sendToTarget();
                        }
                    }
                });
                builder.show();
            }
        });

        updateMap();
    }

    public void updateMap(){
        Log.w("-D-", "SimulationActivity.updateMap(): updating map's text");
        mapText.setText(StaticVars.grid);
    }

    public void updateDebugInfo(){
        Log.w("-D-", "SimulationActivity.updateDebugInfo(): TID = " + Thread.currentThread().getId());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("out: ");
                sb.append(lastSentMessage);
                sb.append(" in: ");
                if (incomingMessage.length() > 0) {
                    sb.append(incomingMessage.charAt(0));
                } else {
                    sb.append("");
                }
                sb.append("\n");
                sb.append(" curRow: ");
                sb.append(StaticVars.curRow);
                sb.append(" curCol: ");
                sb.append(StaticVars.curCol);
                sb.append(" direction: ");
                sb.append(StaticVars.direction);
                debugInfoText.setText(sb.toString());
            }
        });
    }

    public void runSimulation(){
        Log.w("-D-", "SimulationActivity.runSimulation(): starting");
        if (StaticVars.hasReachTarget){
            Toast.makeText(getApplicationContext(), "runSimulation(): a simulation has already ended, please reset...", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(getApplicationContext(), "runSimulation(): starting simulation", Toast.LENGTH_LONG).show();
        myWrapper = new Wrapper(myRobot);  // to allow multiple clicks on "run" (should we?)
        myWrapper.start();
        Log.w("-D-", "SimulationActivity.runSimulation(): Wrapper launched");
    }

    public void checkSimulationStatus(){
        if (StaticVars.hasReachTarget){
            Toast.makeText(getApplicationContext(), "checkSimulationStatus(): Simulation Finished!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg){
        if (msg.what == SimulationActivity.MessageConstants.MESSAGE_READ){
            receivedMessagesCount++;
            Log.w("-D-", "SimulationActivity.handleMessage(): received chunk number " + receivedMessagesCount);
            int chunkSize = msg.arg1;
            Log.w("-D-", "SimulationActivity.handleMessage(): reading chunk of size " + chunkSize);
            byte[] buff = (byte[]) msg.obj;
            String receivedMsg;
            try {
                receivedMsg = new String(buff, 0, chunkSize, "ISO-8859-1");
            } catch (UnsupportedEncodingException e){
                Log.e("-E-", "SimulationActivity.handleMessage(): ERROR: ", e);
                return false;
            }
            Log.w("-D-", "SimulationActivity.handleMessage(): " + receivedMsg);
            incomingMessage.append(receivedMsg);  // since the message is received in chunks, we append..
            myRobot.shouldBusyWait = false;  // horrible interference with Robot's internal field... telling him that response has arrived
            Thread.yield();  // very, very heuristic usage... to allow StaticVars to be updated
            updateDebugInfo();
            updateMap();
            checkSimulationStatus();
            return true;
        }
        if (msg.what == SimulationActivity.MessageConstants.MESSAGE_WRITE){
            Log.w("-D-", "SimulationActivity.handleMessage(): writing");
            return true;
        }
        if (msg.what == SimulationActivity.MessageConstants.MESSAGE_TOAST){
            Log.w("-D-", "SimulationActivity.handleMessage(): toasting");
            Bundle bundle = msg.getData();
            Toast.makeText(getApplicationContext(), "handleMessage(): " + bundle.getString("toast"), Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    public void bluetoothScanAndChoose(){
        checkBluetoothSupport();
        checkBluetoothEnabled();
        checkPairedDevices();
        chooseDevice();
        Log.w("-D-", "SimulationActivity.bluetoothScanAndChoose(): done ");
    }

    public void initializeConnection(){
        Log.w("-D-", "SimulationActivity.initializeConnection(): starting ");
        if (chosenDevice == null){
            Log.w("-D-", "SimulationActivity.initializeConnection(): must choose a target device by pressing 'scan' ");
            Toast.makeText(getApplicationContext(), "must choose a target device by pressing 'scan'", Toast.LENGTH_LONG).show();
            return;
        }
        connectThread = new SimulationActivity.ConnectThread(chosenDevice);
        connectThread.start();
        Log.w("-D-", "SimulationActivity.initializeConnection(): spawned connectThread ");
    }

    public void sendMessage(String msg){
        Log.d("-D-", "ConnectThread: sendMessage() - going to try and send a message");
        incomingMessage = new StringBuilder();  // "zero-out" the incoming message to be ready for the feedback
        if (!runningOffline) {
            // not running offline, must verify that bluetooth connection is established
            if (bluetoothAdapter == null) {
                Log.w("-D-", "SimulationActivity.sendMessage(): must enable bluetooth by pressing 'scan' ");
                Toast.makeText(getApplicationContext(), "must enable bluetooth by pressing 'scan'", Toast.LENGTH_LONG).show();
                return;
            }
            if (chosenDevice == null) {
                Log.w("-D-", "SimulationActivity.sendMessage(): must choose a target device by pressing 'scan' ");
                Toast.makeText(getApplicationContext(), "must choose a target device by pressing 'scan'", Toast.LENGTH_LONG).show();
                return;
            }
            if (connectedThread == null) {
                Log.w("-E-", "SimulationActivity.sendMessage(): connectedThread is null");
                Toast.makeText(getApplicationContext(), "connectedThread is null", Toast.LENGTH_LONG).show();
                return;
            }
            connectedThread.write(msg.getBytes());  // actually send the message via bluetooth
        } else {
            Log.d("-D-", "ConnectThread: sendMessage() - running offline... message not really sent");
        }
        lastSentMessage = msg;
        updateDebugInfo();
        Log.w("-D-", "SimulationActivity.sendMessage(): sending message: " + msg);
    }

    public void checkBluetoothSupport(){
        Log.w("-D-", "SimulationActivity.checkBluetoothSupport(): checking that Bluetooth is supported");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth!", Toast.LENGTH_LONG).show();
        }
    }

    public void checkBluetoothEnabled(){
        Log.w("-D-", "SimulationActivity.checkBluetoothEnabled(): checking that Bluetooth is enabled");
        if (!bluetoothAdapter.isEnabled()) {
            Log.w("-D-", "SimulationActivity.checkBluetoothEnabled(): Bluetooth is not enabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        Log.w("-D-", "SimulationActivity.checkBluetoothEnabled(): Bluetooth is enabled");
        Toast.makeText(getApplicationContext(), "Your Bluetooth is enabled. Yay!", Toast.LENGTH_LONG).show();
    }

    public void chooseDevice(){
        ArrayList<String> deviceInfoList = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            deviceInfoList.add(device.getAddress());
        }
        deviceInfoList.add("None");

        final String[] options = deviceInfoList.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a bluetooth device");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.w("-D-", "SimulationActivity.chooseDevice(): user clicked option " + which);
                // the user clicked on options[which]
                if (options[which].equals("None")) {
                    Log.w("-D-", "SimulationActivity.chooseDevice(): No device was chosen");
                    Toast.makeText(getApplicationContext(), "No device was chosen", Toast.LENGTH_LONG).show();
                }
                else {
                    for (BluetoothDevice device : pairedDevices) {
                        if (options[which].equals(device.getAddress())){
                            chosenDevice = device;
                            Log.w("-D-", "SimulationActivity.chooseDevice(): chosen " + device.toString());
                            initializeConnection();  // now it's possible after device was chosen
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
            Log.w("-D-", "SimulationActivity.checkPairedDevices(): found paired device - "
                    + deviceName + "  " + deviceHardwareAddress);
        }
    }

    public void makeDiscoverable(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1800);
        startActivity(discoverableIntent);
    }

    private void manageMyConnectedSocket(BluetoothSocket socket) {
        Log.e("-D-", "SimulationActivity.manageMyConnectedSocket(): beginning...");
        connectedThread = new SimulationActivity.ConnectedThread(socket);
        connectedThread.start();
        Log.e("-D-", "SimulationActivity.manageMyConnectedSocket(): spawned connectedThread");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Bluetooth connection established", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void terminateSocketConnection(){
        Toast.makeText(getApplicationContext(), "terminating Bluetooth connection", Toast.LENGTH_SHORT).show();
        Log.w("-D-", "SimulationActivity.terminateSocketConnection(): starting");
        if (connectThread != null) {
            Log.w("-D-", "SimulationActivity.manageMyConnectedSocket(): terminating connectThread");
            connectThread.cancel();
        }
        if (connectedThread != null){
            Log.w("-D-", "SimulationActivity.manageMyConnectedSocket(): terminating connectedThread");
            connectedThread.cancel();
        }
    }

    public interface Commands {
        public static final String TURN_90_RIGHT = "e";
        public static final String TURN_90_LEFT = "q";
        public static final String MOVE_TO_BLACK_LINE = "g";
    }

    public interface Responses {
        public static final String SUCCESS = "y";
        public static final String OBSTACLE_DETECTED = "n";
    }

    // =======================================================================================
    // [MK]
    // Worst code ever..?
    // we have Robot class literally so that two objects that one contains the other could
    // communicate with each other's internal fields, while keeping them "apparently" separated
    // and encapsulated...
    // =======================================================================================
    public class Robot {
        boolean shouldBusyWait;

        Robot(){
            shouldBusyWait = false;
        }

        private void sendCommand(String command){
            Log.e("-D-", "Robot.sendCommand(): " + command);
            sendMessage(command);
        }

        private boolean readFeedback(){
            Log.w("-D-", "Robot.readFeedback(): ");
            String feedback = incomingMessage.toString().substring(0, 1);  // only consider the first character!
            if (feedback.equals(Responses.SUCCESS)){
                return true;
            } else if (feedback.equals(Responses.OBSTACLE_DETECTED)){
                return false;
            } else {
                Log.e("-E-", "Robot.readFeedback(): invalid feedback from robot!: " + feedback);
                return false;
            }
        }

        // this should be called by the wrapper
        //     true = success
        //     false = obstacle
        public boolean doCommand(String command){
            Log.w("-D-", "Robot.doCommand(): " + command);
            Log.w("-D-", "Robot.doCommand(): TID = " + Thread.currentThread().getId());
            if (!(command.equals(Commands.TURN_90_RIGHT) ||
                    command.equals(Commands.TURN_90_LEFT) ||
                    command.equals(Commands.MOVE_TO_BLACK_LINE))) {
                Log.e("-D-", "Robot.doCommand(): invalid command!");
                return false;
            }
            shouldBusyWait = true;
            sendCommand(command);
            Log.w("-D-", "Robot.doCommand(): going to busy-wait");
            while (shouldBusyWait){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e){
                    Log.e("-D-", "Robot.doCommand(): interrupted during sleep!");
                }
            }
            Log.w("-D-", "Robot.doCommand(): finished busy-wait");
            return readFeedback();
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            Log.w(TAG, "constructor of ConnectThread");
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                my_uuid = mmDevice.getUuids()[0].getUuid();
                tmp = device.createRfcommSocketToServiceRecord(my_uuid);
            } catch (IOException e) {
                Log.e("-D-", "ConnectThread(): Socket's create() method failed");
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.e("-D-", "ConnectThread: run() - starting...");

            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            Log.e("-D-", "ConnectThread: canceled discovery");
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("-D-", "ConnectThread: mmSocket.connect() seem to succeed..");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    Log.e("-E-", "ConnectThread: Unable to connect; close the socket and return. " + connectException.getMessage());
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("-E-", "ConnectThread: Could not close the client socket");
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



    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            Log.w(TAG, "constructor of ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.e("-D-", "ConnectedThread(): run() - starting..");
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // zero the buffer before trying to read into it again!
                    Arrays.fill(mmBuffer, (byte)0);

                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);  // this read command is BLOCKING the thread until there's something in the buffer!
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = handler.obtainMessage(
                            SimulationActivity.MessageConstants.MESSAGE_READ, numBytes, -1,
                            mmBuffer.clone());  // send a clone to not interfere with the original buffer
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            Log.w(TAG, "ConnectedThread: write() - starting..");
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = handler.obtainMessage(
                        SimulationActivity.MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        handler.obtainMessage(SimulationActivity.MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}
