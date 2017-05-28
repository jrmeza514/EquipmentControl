package com.jrmeza.equipmentcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    public static final String SCANNER_UUID_1 = "00001101-0000-1000-8000-00805F9B34FB";
    public static final String SCANNER_NAME = "CT1016926047";
    public static final String MAC_ADDRESS = "00:06:72:49:06:61";
    /* DB Constants */
    public static final String EQUIPMENT_DB = "Equipment";
    public static final String STORE_ID = "T-2605";
    /*Constants*/
    private final int REQUEST_ENABLE_BT = 1;
    /* UI Variable Declarations */
    private TextView barcodeLabel;
    private TextView statusLabel;


    private TextView activeTMLabel;
    private TextView dateTimeLabel;
    private TextView scannerStatusLabel;
    private Button checkOutBtn;
    private Button checkInBtn;
    private Button clearBtn;
    private Toolbar mToolbar;

    /* Logic Variables */
    private String referenceCode;
    /* Click Listener for the Clear Button*/
    public View.OnClickListener clearClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            clearAll();
        }
    };
    private ScannerState scannerState;

    /* Utitlity Variables */
    private Timer mTimer;
    private ConnectThread connectThread;
    private FirebaseDatabase database;
    /* Click Listener for the Checkout Button */
    public View.OnClickListener checkoutClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            checkout();
        }
    };
    /* Click Listener for the CheckIn Button */
    public View.OnClickListener checkInClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            checkIn();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bar_master_view:
                if (connectThread != null) {
                    connectThread.cancel();
                }
                scannerState = ScannerState.DISCONNECTED;
                connectThread = null;
                mTimer.cancel();

                Intent intent = new Intent(this, MasterControl.class);
                startActivity(intent);
                finish();

                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.app_toolbar);
        setSupportActionBar(mToolbar);

        /* Initialize UI Variables */
        barcodeLabel = (TextView) findViewById(R.id.barcodeLabel);
        statusLabel = (TextView) findViewById(R.id.equipmentStatusLabel);
        dateTimeLabel = (TextView) findViewById(R.id.dateTimeLabel);
        scannerStatusLabel = (TextView) findViewById(R.id.scannerStatusLabel);
        checkOutBtn = (Button) findViewById(R.id.check_out_btn);
        checkInBtn = (Button) findViewById(R.id.check_in_btn);
        clearBtn = (Button) findViewById(R.id.clear_btn);
        activeTMLabel = (TextView) findViewById(R.id.activeTeamMemberLabel);



        /*Set Click Listeners*/
        checkInBtn.setOnClickListener(checkInClickListener);
        checkOutBtn.setOnClickListener(checkoutClickListener);
        clearBtn.setOnClickListener(clearClickListener);

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

        database = FirebaseDatabase.getInstance();
        scannerState = ScannerState.DISCONNECTED;
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onStart() {
        super.onStart();
        /* Start the connection Thread for the BluetoothScanner */
        connectThread = new ConnectThread(this);
        mTimer = new Timer();
        /*Check the connection every 2.5s */
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                switch (scannerState) {
                    case CONNECTED:
                        Log.d("ASS", "Scanner Already Connected");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                scannerStatusLabel.setText("Connected");
                            }
                        });
                        break;
                    case DISCONNECTED:
                        scannerState = ScannerState.CONNECTING;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                scannerStatusLabel.setText("Searching");
                            }
                        });
                        if (connectThread != null && connectThread.mBluetoothSocket != null && connectThread.mBluetoothSocket.isConnected()) {
                            scannerState = ScannerState.CONNECTED;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    scannerStatusLabel.setText("Connected");
                                }
                            });
                        } else {
                            if (connectThread != null && connectThread.mBluetoothSocket != null && connectThread.mBluetoothSocket.isConnected()) {
                                try {
                                    connectThread.mBluetoothSocket.close();

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            connectThread = new ConnectThread(getApplicationContext());
                            connectThread.start();
                            Log.d("ASS", "Attempting to Connect");
                        }
                        break;
                    case CONNECTING:

                        break;
                }
            }
        }, 0, 2500);
        connectThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (connectThread != null) {
            connectThread.cancel();
        }
        scannerState = ScannerState.DISCONNECTED;
        connectThread = null;
        mTimer.cancel();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connectThread != null) {
            connectThread.cancel();
        }
        scannerState = ScannerState.DISCONNECTED;
        connectThread = null;
        mTimer.cancel();
    }

    /* Handle the Data Received from the Scanner */
    public void handlerScan(final Context context, final String equipmentId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /* Clear the UI */
                clearAll();
                /* Set Reference Code */
                referenceCode = equipmentId;

                /* Obtain the Database Reference */
                DatabaseReference dbref = database.getReference(EQUIPMENT_DB).child(STORE_ID);
                /* Fetch the Data */
                dbref.child(equipmentId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        /* Obtain Equipment Object */
                        Equipment equipment = dataSnapshot.getValue(Equipment.class);

                        /*  Return the function Call if the Data is Invalid */
                        if (equipment == null) return;

                        /*  Set equipmentID Label */
                        barcodeLabel.setText(equipment.alias);

                        /*  Change Status-Dependent UI */
                        switch (equipment.status) {
                            /* The Equipment Is Available */
                            case 0:
                                statusLabel.setText("Available");
                                activeTMLabel.setText("");
                                checkOutBtn.setEnabled(true);
                                checkInBtn.setEnabled(false);
                                break;

                            /* The Equipment is Unavailable*/
                            case 1:
                                statusLabel.setText("Unavailable");
                                SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a MM/dd/yyyy");
                                dateTimeLabel.setText(dateFormat.format(equipment.lastTransaction.timestamp));
                                activeTMLabel.setText(equipment.activeTM);
                                checkOutBtn.setEnabled(false);
                                checkInBtn.setEnabled(true);
                                break;

                            /* The Equipment is Out For Repair */
                            case 2:
                                statusLabel.setText("Out for Repair");
                                activeTMLabel.setText("");
                                checkOutBtn.setEnabled(false);
                                checkInBtn.setEnabled(false);
                                break;
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }
        });
    }

    /* Check Out the Last Scanned Item */
    public void checkout() {

        /*
            Create a Dialog to prompt for the user's name
         */
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setMaxLines(1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Check Out Equipment")
                .setMessage("Please Enter Your Name:")
                .setView(input);

        /* Set Positive Action Listener */
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /* Obtain Database References*/
                final DatabaseReference equipmentRef = database.getReference(EQUIPMENT_DB).child(STORE_ID);
                final DatabaseReference ref = equipmentRef.child(referenceCode);

                /* Read and Modify Data */
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        /* Get Equipment Object */
                        final Equipment equipment = dataSnapshot.getValue(Equipment.class);

                        /* Set the Active TM and Transaction Time*/
                        equipmentRef.child(referenceCode).setValue(new Equipment(1, input.getText().toString(), equipment.alias, new Equipment.LastTransaction(
                                new Date().getTime(),
                                input.getText().toString()
                        )));

                        /* Acknowledge the Transaction with a Toast */
                        Toast.makeText(getApplicationContext(), "Checked Out " + referenceCode, Toast.LENGTH_LONG).show();
                        /* Clear the UI */
                        clearAll();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }
        });

        builder.show();
    }

    /* CheckIn the Last Scanned Item */
    public void checkIn() {
        /* Obtain the database reference for the equipment*/
        final DatabaseReference equipmentRef = database.getReference(EQUIPMENT_DB).child(STORE_ID);
        final DatabaseReference ref = equipmentRef.child(referenceCode);

        /* Read and Modify the Data */
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                /* Get the Equipment Object */
                Equipment equipment = dataSnapshot.getValue(Equipment.class);
                /* Get the Active TM */
                String name = equipment.activeTM.toString();

                /* Remove Active TM */
                equipmentRef.child(referenceCode).setValue(new Equipment(0, "none", equipment.alias, new Equipment.LastTransaction(
                        new Date().getTime(),
                        name
                )));
                /* Make a toast to Acknowledge the Transaction  */
                Toast.makeText(getApplicationContext(), "Checked In " + referenceCode, Toast.LENGTH_LONG).show();
                /* Clear the UI */
                clearAll();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Unable to Complete Transaction", Toast.LENGTH_LONG).show();
            }
        });
    }

    /* Reset Every UI Element To Clear Any Data */
    public void clearAll() {
        referenceCode = null;
        barcodeLabel.setText("");
        statusLabel.setText("");
        activeTMLabel.setText("");
        dateTimeLabel.setText("");
        checkInBtn.setEnabled(false);
        checkOutBtn.setEnabled(false);
        dateTimeLabel.setText("");
    }

    private enum ScannerState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "No Such Device", Toast.LENGTH_SHORT).show();
                        Toast.makeText(mContext, "No Such Device", Toast.LENGTH_SHORT).show();
                        scannerStatusLabel.setText("Disconnected");
                    }
                });
                scannerState = ScannerState.DISCONNECTED;
                return;
            }

            // Create BT Socket
            BluetoothSocket tmp = null;
            try{
                tmp = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(MainActivity.SCANNER_UUID_1));
            }
            catch (Exception e ){
                scannerState = ScannerState.DISCONNECTED;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scannerStatusLabel.setText("Disconnected");
                    }
                });
                e.printStackTrace();
                return;
            }


            mBluetoothSocket = tmp;
            //Disable Discovery
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mBluetoothSocket.connect();
                scannerState = ScannerState.CONNECTED;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scannerStatusLabel.setText("Connected");
                    }
                });
                InputStream stream = mBluetoothSocket.getInputStream();
                Log.d("JRM", "Connected to: " + mBluetoothDevice.getName());
                int numBytes = 0;
                byte[] buffer = new byte[1024];

                while (true){
                    scannerState = ScannerState.CONNECTED;
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
                scannerState = ScannerState.DISCONNECTED;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scannerStatusLabel.setText("Disconnected");
                    }
                });
                return;
            }


        }

        public void cancel() {
            try {
                mBluetoothSocket.close();
                scannerState = ScannerState.DISCONNECTED;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scannerStatusLabel.setText("Disconnected");
                    }
                });

            } catch (Exception e) {
                Log.e("Error", "Could not close the client socket", e);
            }
        }
    }


}

