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

public class BluetoothFinderActivity extends AppCompatActivity {

    private Button scanButton;
    private TextView statusView;
    private ListView deviceView;
    private ArrayList<String> deviceDisplayList;
    private ArrayList<BluetoothDevice> deviceList;
    private ArrayAdapter<String> adapter;
    private IntentFilter intentFilter;
    private ConnectThread connectThread;
    private BluetoothAdapter bluetoothAdapter;

    static ConnectedThread connectedThread;

    private static final UUID MY_UUID = UUID.fromString("04c6093b-0000-1000-8000-00805f9b34fb");


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

    public void scan(View view) {
        deviceDisplayList.clear();
        adapter.notifyDataSetChanged();
        scanButton.setVisibility(View.INVISIBLE);
        statusView.setText(R.string.scanning);
        bluetoothAdapter.startDiscovery();
    }


    private void initializeViews() {
        deviceDisplayList = new ArrayList<>();
        deviceList = new ArrayList<>();
        scanButton = findViewById(R.id.scanButton);
        statusView = findViewById(R.id.statusView);
        deviceView = findViewById(R.id.deviceView);
        adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceDisplayList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                return view;
            }
        };

        deviceView.setAdapter(adapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        setDeviceClickListener();
    }

    private void initializeIntentFilter() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        //Register a new Broadcast Receiver
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void setDeviceClickListener() {
        deviceView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //bluetoothAdapter.cancelDiscovery();
                BluetoothDevice device = deviceList.get(position);
                connectThread = new ConnectThread(device);
                connectThread.start();
            }
        });
    }

    private final BroadcastReceiver broadcastReceiver =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("Action", action);

            assert action != null;
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    scanButton.setVisibility(View.VISIBLE);
                    scanButton.setEnabled(true);
                    statusView.setText(R.string.select_device);
                    break;
                case BluetoothDevice.ACTION_FOUND:
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

    public static void writeToServer(int data) {
        connectedThread.write(data);
    }

    public static void writeToServer(String data) {
        byte[] dataBytes = data.getBytes();
        connectedThread.write(dataBytes);
    }

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
                if (ConfigurationActivity.connectionView != null) {
                    ConfigurationActivity.connectionView.setText(R.string.disconnected);
                }
                if (ConfigurationActivity.startMatchButton != null) {
                    ConfigurationActivity.startMatchButton.setEnabled(false);
                    ConfigurationActivity.startMatchButton.setBackgroundColor(Color.GRAY);
                }
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

        void write(int out) {
            try {
                mmOutStream.write(out);
            } catch (IOException e) {
                Log.e("WRITE FAIL", "Exception during write", e);
            }
        }

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
