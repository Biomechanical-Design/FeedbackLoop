package com.uoguelph.feedbackloop;

import androidx.annotation.MainThread;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.ContentValues.TAG;


class quickChart {
    static private AppCompatActivity activity;

    private LineChart chart;
    private List<Entry> entry = new ArrayList<Entry>();
    private LineDataSet dataSet;
    private LineData lineData;
    private double increment;
    private Thread thread;
    private int color;

    public quickChart( LineChart chart, int c ){
        // TODO: remove increment later...
        increment = 0.05 + Math.random()/20;
        this.chart = chart;
        this.color = c;
    }

    public static void SetBaseActivity( AppCompatActivity activity ) {
        quickChart.activity = activity;
    }

    void Start() {
        // chart options
        chart.getDescription().setEnabled( false );
        chart.setTouchEnabled( true );
        chart.setDragEnabled( true );
        chart.setScaleEnabled( false );
        chart.setDrawGridBackground( false );

        // x-axis options
        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(false);


        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setEnabled( false );
        leftAxis.setDrawGridLines( false );
        leftAxis.setDrawLabels( false );
        leftAxis.setAxisMaximum( 1.1f );
        leftAxis.setAxisMinimum( -1.1f );

        chart.getAxisRight().setEnabled( false );
        chart.getLegend().setEnabled( false );

        double initialX = Math.random() * Math.PI * 2;
        entry.add( new Entry( 0f, 0f ));

        dataSet = new LineDataSet( entry, "Values");
        dataSet.setColor(color);
        dataSet.setDrawCircles( false );
        dataSet.setAxisDependency( YAxis.AxisDependency.LEFT );
        dataSet.setDrawValues( false );

        lineData = new LineData( dataSet );
        chart.setData(lineData);

        feed();
    }

    private void AddEntry() {
        int numEntries = entry.size();
        // float xValue = entry.get( entry.size() - 1 ).getX();
        float xValue = (float)( numEntries * increment );

        // TODO: CHANGE NEXT LINE
        //int CONNECTING_STATUS = 1;
        //int MESSAGE_READ = 2;
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case 1:
                        break;
                    case 2:
                        Float arduinoMsg = (Float) msg.obj; // Read message from Arduino
                        lineData.addEntry( new Entry( (float)numEntries, arduinoMsg), 0 );
                        lineData.notifyDataChanged();
                        chart.notifyDataSetChanged();
                        // 500 +-= PI * 2 * 4 * 10
                        chart.setVisibleXRange( 500f, 500f );
                        chart.moveViewToX(numEntries);
                        break;
                }
            }
        };


    }

    // warning this function is dangerous
    private void feed () {
        if( thread != null ) thread.interrupt();
        final Runnable runnable = () -> AddEntry();
        thread = new Thread(() -> {
            while(true) {
                activity.runOnUiThread( runnable );

                try {
                    Thread.sleep(25);
                } catch ( InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}


public class MainActivity extends AppCompatActivity {

    private quickChart pressureChart;
    private quickChart volumeChart;
    private quickChart flowChart;

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update



    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        quickChart.SetBaseActivity( this );
        pressureChart = new quickChart( findViewById( R.id.chartPressure), Color.BLUE );
        flowChart = new quickChart( findViewById( R.id.chartFlow), Color.RED );
        volumeChart = new quickChart( findViewById( R.id.chartVolume), Color.GREEN );

        pressureChart.Start();
        flowChart.Start();
        volumeChart.Start();

        registerControlChangers(
                findViewById( R.id.textIP ),
                findViewById( R.id.buttonUp_fip ),
                findViewById( R.id.buttonDn_fip ) );

        registerControlChangers(
                findViewById( R.id.textRate ),
                findViewById( R.id.buttonUp_frate ),
                findViewById( R.id.buttonDn_frate ) );

        registerControlChangers(
                findViewById( R.id.textIE ),
                findViewById( R.id.buttonUp_fie ),
                findViewById( R.id.buttonDn_fie ) );

        registerControlChangers(
                findViewById( R.id.textPPT ),
                findViewById( R.id.buttonUp_fppt ),
                findViewById( R.id.buttonDn_fppt ) );
        registerControlChangers(
                findViewById( R.id.textPEEP ),
                findViewById( R.id.buttonUp_fpeep ),
                findViewById( R.id.buttonDn_fpeep ) );

        registerControlChangers(
                findViewById( R.id.textFO ),
                findViewById( R.id.buttonUp_ffo ),
                findViewById( R.id.buttonDn_ffo ) );



        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        //final Toolbar toolbar = findV\
        // 'iewById(R.id.toolbar);
        //final ProgressBar progressBar = findViewById(R.id.progressBar);
        //progressBar.setVisibility(View.GONE);
        //final TextView textViewInfo = findViewById(R.id.textViewInfo);
        //final Button buttonToggle = findViewById(R.id.buttonToggle);
        //buttonToggle.setEnabled(false);
        //final ImageView imageView = findViewById(R.id.imageView);
        //imageView.setBackgroundColor(getResources().getColor(R.color.colorOff));

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progree and connection status
            //toolbar.setSubtitle("Connecting to " + deviceName + "...");
            //progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }


        /*Second most important piece of Code. GUI Handler
         */


        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Move to adapter list
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        // Button to ON/OFF LED on Arduino Board
        /*buttonToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cmdText = null;
                String btnState = buttonToggle.getText().toString().toLowerCase();
                switch (btnState){
                    case "turn on":
                        buttonToggle.setText("Turn Off");
                        // Command to turn on LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn on>";
                        break;
                    case "turn off":
                        buttonToggle.setText("Turn On");
                        // Command to turn off LED on Arduino. Must match with the command in Arduino code
                        cmdText = "<turn off>";
                        break;
                }
                // Send command to Arduino board
                connectedThread.write(cmdText);
            }
        });*/
    }

    void registerControlChangers(EditText valueField, ImageButton upButton, ImageButton downButton ) {
        upButton.setOnClickListener(
                v -> {
                    int val = Integer.parseInt(String.valueOf(valueField.getText()));
                    valueField.setText( Integer.toString( val + 1 ));
                }
        );
        downButton.setOnClickListener(
                v -> {
                    int val = Integer.parseInt(String.valueOf(valueField.getText()));
                    valueField.setText( Integer.toString( val - 1 ));
                }
        );

    }


    /* ============================ Thread to Create Bluetooth Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

}