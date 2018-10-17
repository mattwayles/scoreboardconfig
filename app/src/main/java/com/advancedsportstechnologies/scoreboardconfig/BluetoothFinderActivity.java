package com.advancedsportstechnologies.scoreboardconfig;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * App activity responsible for all Bluetooth activities. This activity scans for bluetooth devices,
 * connects to selected devices on background threads, handles unexpected loss of connection, and writes
 * data to the server via Bluetooth socket.
 */
public class BluetoothFinderActivity extends AppCompatActivity {

    /**
     * Register all necessary page elements
     */
    private Button scanButton;
    private TextView statusView;
    private ListView deviceView;
    private ArrayList<String> deviceDisplayList;
    private ArrayList<BluetoothDevice> deviceList;
    private ArrayAdapter<String> adapter;
    private ConnectThread connectThread;
    private BluetoothAdapter bluetoothAdapter;

    static ConnectedThread connectedThread;

    /**
     * This UUID is used to communicate with the scoreboard
     */
    private static final UUID MY_UUID = UUID.fromString("04c6093b-0000-1000-8000-00805f9b34fb");

    /**
     * When this page is opened, immediately being scanning for devices
     * @param savedInstanceState    The saved state of the previous application run
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_finder);

        //Get handles to all Views
        initializeViews();

        //Setup the Intent Filter
        initializeIntentFilter();

        //Start auto discovery
        statusView.setText(R.string.scanning);
        bluetoothAdapter.startDiscovery();
    }

    /**
     * On RESCAN button check, clear the list and begin scanning again
     * @param view  A handle to the origin button
     */
    public void rescan(View view) {
        deviceDisplayList.clear();
        adapter.notifyDataSetChanged();

        scanButton.setVisibility(View.INVISIBLE);
        statusView.setText(R.string.scanning);

        bluetoothAdapter.startDiscovery();
    }

    /**
     * Resolve each element object to their corresponding Views
     */
    private void initializeViews() {
        deviceDisplayList = new ArrayList<>();
        deviceList = new ArrayList<>();
        scanButton = findViewById(R.id.scanButton);
        statusView = findViewById(R.id.statusView);
        deviceView = findViewById(R.id.deviceView);
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceDisplayList) {

            //Modify the ListView item text color
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(Color.parseColor("#575757"));
                return view;
            }
        };

        //Set adapters
        deviceView.setAdapter(adapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        setDeviceClickListener();
    }

    /**
     * TWhen a Bluetooth device is clicked on the ListView, establish a connection
     */
    private void setDeviceClickListener() {
        deviceView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = deviceList.get(position);
                connectThread = new ConnectThread(device);
                connectThread.start();
            }
        });
    }

    /**
     * Create an Intent Filter to register and log messages from the bluetootth adapter
     */
    private void initializeIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        //Register a new Broadcast Receiver
        registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * Create a broadcast received to properly handle messages from the BluetoothAdapter
     */
    private final BroadcastReceiver broadcastReceiver =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("Action", action);

            assert action != null;
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    //When discovery has finished, enabled the rescan button and change the TextView
                    scanButton.setVisibility(View.VISIBLE);
                    scanButton.setEnabled(true);
                    if (deviceDisplayList.size() == 0) {
                        statusView.setText(R.string.no_devices_found);
                    } else {
                        statusView.setText(R.string.select_device);
                    }

                    break;
                case BluetoothDevice.ACTION_FOUND:
                    //When a device has been found, add to the ListView
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String name = device.getName();
                    String address = device.getAddress();
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    String deviceStr = name != null ? name + " (" + address + ") - Strength: " + String.valueOf(Math.abs(rssi)) + "%"
                            : address + "- Strength: " + String.valueOf(Math.abs(rssi)) + "%";

                    deviceDisplayList.add(deviceStr);
                    deviceList.add(device);
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     */
    public synchronized void connected(BluetoothSocket socket) {
        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Intent intent = new Intent(getApplicationContext(), ConfigurationActivity.class);
        startActivity(intent);
    }

    /**
     * Write an int buffer to the server to buffer the subsequent message
     * @param data  The length of the following message
     */
    public static void writeToServer(int data) {
        connectedThread.write(data);
    }

    /**
     * Write data to the server
     * @param data  The data to be written to the server
     */
    public static void writeToServer(String data) {
        byte[] dataBytes = data.getBytes();
        connectedThread.write(dataBytes);
    }

    /**
     * Close the bluetooth socket
     */
    public static void closeSocket() {
        connectedThread.cancel();
    }


    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        BluetoothFinderActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                statusView.setText(R.string.select_device);
                Toast.makeText(BluetoothFinderActivity.this, "Device connection was lost", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), BluetoothFinderActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        /**
         * Create a new ConnectThread and attempt to establish a connection to a bluetooth device
         * @param device    The device to establish a connection with
         */
        ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e("Failure", "create() failed", e);
            }
            mmSocket = tmp;
        }

        /**
         * Starts the worker thread, makes the actual connection to the bluetooth device
         */
        public void run() {
            Log.i("BEGIN", "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e("Failure", "Connection Failed");
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e("FAIL", "unable to close() socket during connection failure", e2);
                }
            }

            // Reset the ConnectThread because we're done
            connectThread = null;

            // Start the connected thread
            connected(mmSocket);
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("FAIL", "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        /**
         * Create a ConnectedThread and open the data socket
         * @param socket    The socket information
         */
        ConnectedThread(BluetoothSocket socket) {
            Log.d("CREATE", "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("FAIL", "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        /**
         * Run the worker thread and open communication between server/client
         */
        public void run() {
            Log.i("BEGIN", "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    final int bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    BluetoothFinderActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            write(bytes);
                        }
                    });
                } catch (IOException e) {
                    Log.e("DISCONNECTED", "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e("WRITE FAIL", "Exception during write", e);
            }
        }

        /**
         * Write integers to the connected OtuStream
         * @param out   The integer to write
         */
        void write(int out) {
            try {
                mmOutStream.write(out);
            } catch (IOException e) {
                Log.e("WRITE FAIL", "Exception during write", e);
            }
        }

        /**
         * Close the server/client connection socket
         */
        void cancel() {
            try {
                mmOutStream.write(-1);
                mmSocket.close();
            } catch (IOException e) {
                Log.e("FAIL", "close() of connect socket failed", e);
            }
        }
    }
}
