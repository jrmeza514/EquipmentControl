package com.jrmeza.equipmentcontrol;

import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private final int REQUEST_ENABLE_BT = 1;
    public static final String SCANNER_UUID_1 = "00001101-0000-1000-8000-00805F9B34FB";
    public static final String SCANNER_UUID_2 = "00000000-0000-1000-8000-00805F9B34FB";
    public static final String SCANNER_NAME = "CT1016926047";
    public static final String MAC_ADDRESS = "00:06:72:49:06:61";
    /* DB Constants */
    public static final String TARGET_EQUIPMENT_CONTROL = "targetequipmentcontrol";
    public static final String EQUIPMENT_DB = "Equipment";
    public static final String RECORDS_DB = "Records";



    private TextView barcodeLabel;
    private TextView statusLabel;
    private Button checkOutBtn;
    private Button checkInBtn;
    private ConnectThread connectThread;

    private HashMap<String, String>  EquipmentIdMap = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference targetEquipmentControl = database.getReference();
        DatabaseReference equipmentRef = targetEquipmentControl.child(EQUIPMENT_DB);
        DatabaseReference recordsRef = database.getReference(RECORDS_DB);
        Log.d("JRM", equipmentRef.getParent().getKey());
        barcodeLabel = (TextView) findViewById(R.id.barcodeLabel);
        statusLabel = (TextView) findViewById(R.id.equipmentStatusLabel);
        checkOutBtn = (Button) findViewById(R.id.check_out_btn);
        checkInBtn = (Button) findViewById(R.id.check_in_btn);

        /* Check for bluetooth Support */
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
        }

        /* Enable Bluetooth */
        if (!mBluetoothAdapter.isEnabled()){
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }

        connectThread = new ConnectThread(this);
        DatabaseReference PDA = equipmentRef.child("PDA-2605-001");
        DatabaseReference Status = PDA.child("status");
        Log.d("JRM", Status.toString());
        Status.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int value = dataSnapshot.getValue(Integer.class);
                Log.d("JRM", "Value is: " + value);
                switch (value){
                    case 0:
                        statusLabel.setText("Available");
                        break;
                    case 1:
                        statusLabel.setText("Unavailable");
                        break;
                    case 2:
                        statusLabel.setText("Out for Repair");
                        break;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("JRM", "Failed to read value.", databaseError.toException());
            }
        });
    }


    public void onDestroy() {
        super.onDestroy();
    }

    public void onStart() {
        super.onStart();
        connectThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        connectThread.cancel();
    }

    public void handlerScan(final Context context, final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String equipmentName = EquipmentIdMap.get(message);
                if (equipmentName == null){
                    barcodeLabel.setText(message + "!");
                }else{
                    barcodeLabel.setText("Equipment ID: " + equipmentName);
                    statusLabel.setText("Status: Available");
                    checkOutBtn.setEnabled(true);
                }

            }
        });
    }


    public class ConnectThread extends Thread {
        private BluetoothSocket mBluetoothSocket;
        private BluetoothDevice mBluetoothDevice;
        private BluetoothAdapter mBluetoothAdapter;
        private Context mContext;

        public ConnectThread(Context context){
            mContext = context;
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        @Override
        public void run() {
            super.run();


            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice bluetoothDevice : pairedDevices){
                if (bluetoothDevice.getName().equals(SCANNER_NAME) || bluetoothDevice.getAddress().equals(MAC_ADDRESS)){
                    mBluetoothDevice = bluetoothDevice;
                    break;
                }
            }


            // Make sure we have a device
            if (mBluetoothDevice == null){
                Toast.makeText(mContext, "No Such Device", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create BT Socket
            BluetoothSocket tmp = null;
            try{
                tmp = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(MainActivity.SCANNER_UUID_1));
            }
            catch (IOException ioexception){
                Log.e("Error", "Socket's create() method failed", ioexception);
            }
            catch (Exception e ){
                e.printStackTrace();
            }

            mBluetoothSocket = tmp;
            //
            //Disable Discovery
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mBluetoothSocket.connect();
                InputStream stream = mBluetoothSocket.getInputStream();
                Log.d("JRM", "Connected to: " + mBluetoothDevice.getName());

                int numBytes = 0;
                byte[] buffer = new byte[1024];

                while (true){
                    numBytes = stream.read(buffer);
                    String message = new String(buffer, 0 , numBytes - 1  );
                    for (char character : message.toCharArray()){
                        Log.d("JRM", Character.getNumericValue(character) + "!");
                    }
                    handlerScan(mContext, message);
                    Log.d("JRM", "Message Received: " + message + "!");
                }

            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.

                Log.e("Error", "Could Not Connect", connectException);
                try {
                    mBluetoothSocket.close();
                } catch (IOException closeException) {
                    Log.e("Error", "Could not close the client socket", closeException);
                }
                return;
            }


        }

        public void cancel() {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                Log.e("Error", "Could not close the client socket", e);
            }
        }
    }
}

