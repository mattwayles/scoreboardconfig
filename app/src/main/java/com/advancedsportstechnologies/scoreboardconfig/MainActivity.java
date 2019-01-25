package com.advancedsportstechnologies.scoreboardconfig;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 *  Application Welcome screen presenting the corporation logo, Scoreboard Configuration title,
 *  and a button allowing the user to scan for Bluetooth devices at their convenience
 */
public class MainActivity extends AppCompatActivity {
    private TextView statusView;
    private Button button;
    private boolean connecting = false;
    private boolean connected = false;
    private ProgressBar loading;
    private ConnectThread connectThread;
    private static ConnectedThread connectedThread;
    private BluetoothAdapter bluetoothAdapter;

    /**
     * This UUID is used to communicate with the scoreboard
     */
    private static final UUID SCOREBOARD_UUID = UUID.fromString("04c6093b-0000-1000-8000-00805f9b34fb");

    private final String SCOREBOARD_HOSTNAME = "scoreboard"; 
        private final String DEBUG_HOSTNAME = "TAPSC3X9FN32";
    private final String DEBUG_HOSTNAME2 = "TAPSCCXTG7H2";

    /**
     * Register the button when the app is opened
     * @param savedInstanceState    The saved state of the previous application run
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get components from activity
        button = findViewById(R.id.button);
        loading = findViewById(R.id.loading);
        loading.setVisibility(View.INVISIBLE);
        statusView = findViewById(R.id.statusView);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Check for Bluetooth availability
        checkBluetooth(bluetoothAdapter);

        //Create handlers and actions for Bluetooth events
        initializeIntentFilter();

        Intent intent = getIntent();
        boolean connectionLost = intent.getBooleanExtra("connectionLost", false);

        //Begin bluetooth discovery
        if (!connectionLost) {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
                bluetoothAdapter.startDiscovery();
            statusView.setText(getString(R.string.cant_start_bt_disc));
            statusView.setTextColor(getResources().getColor(R.color.redFailure));
        } else {
            statusView.setText(R.string.disconnected);
            statusView.setTextColor(getResources().getColor(R.color.redFailure));
            loading.setVisibility(View.INVISIBLE);
            button.setText(R.string.connect_to_scoreboard);
            button.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Check if Bluetooth is enabled on client device
     */
    private void checkBluetooth(BluetoothAdapter adapter) {
        if (adapter == null || !adapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not Available", Toast.LENGTH_LONG).show();
            finish();
        }
    }
    /**
     * Create an Intent Filter to register and log messages from the bluetooth adapter
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
     * Create a broadcast receiver to properly handle messages from the BluetoothAdapter
     */
    private final BroadcastReceiver broadcastReceiver =  new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    statusView.setText(R.string.scanning);
                    statusView.setTextColor(getResources().getColor(R.color.greenSuccess));
                    loading.setVisibility(View.VISIBLE);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    //When a device has been found, add to the ListView
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    //Check if found device is a scoreboard
                    String name = device.getName();
                    Log.i("DEVICE FOUND: ", name != null ? name : "null");
                    if (!connecting && name != null && (name.equals(SCOREBOARD_HOSTNAME) || name.equals(DEBUG_HOSTNAME) || name.equals(DEBUG_HOSTNAME2))) {
                        connecting = true;
                        connectThread = new ConnectThread(device);
                        connectThread.start();
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    if (connecting) {
                        statusView.setText(R.string.connecting);
                        statusView.setTextColor(getResources().getColor(R.color.greenSuccess));
                        connecting = false;
                    }
                    else {
                        statusView.setText(getString(R.string.no_scoreboard_found));
                        statusView.setTextColor(getResources().getColor(R.color.redFailure));
                        loading.setVisibility(View.INVISIBLE);
                        button.setText(getString(R.string.try_again));
                        button.setVisibility(View.VISIBLE);
                    }
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
        connected = true;
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), ConfigurationActivity.class);
                startActivity(intent);
            }
        });
        try {
            Thread.sleep(1000);
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusView.setText(R.string.connected);
                    statusView.setTextColor(getResources().getColor(R.color.greenSuccess));
                    button.setText(getString(R.string.go_to_config));
                    loading.setVisibility(View.INVISIBLE);
                    button.setVisibility(View.VISIBLE);
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Device connection was lost", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("connectionLost", true);
                        startActivity(intent);
                        finish();
                    }
            });
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
     * On button press, begin scanning for Bluetooth devices
     * @param view  A handle to the origin button
     */
    public void startScan(View view) {
        if (connected) {
            Intent intent = new Intent(getApplicationContext(), ConfigurationActivity.class);
            startActivity(intent);
        } else {
            button.setVisibility(View.INVISIBLE);
            statusView.setTextColor(getResources().getColor(R.color.greenSuccess));
            statusView.setText(R.string.scanning);
            loading.setVisibility(View.VISIBLE);
            bluetoothAdapter.startDiscovery();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////// CONNECT THREAD ///////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


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
                tmp = device.createRfcommSocketToServiceRecord(SCOREBOARD_UUID);
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
                Log.e("Failure", "Connection Failed", e);
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


    ////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////// CONNECTED THREAD ///////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

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
                    MainActivity.this.runOnUiThread(new Runnable() {
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
