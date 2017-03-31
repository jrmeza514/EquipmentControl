package com.jrmeza.equipmentcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.Set;
import java.util.UUID;

import static com.jrmeza.equipmentcontrol.MainActivity.MAC_ADDRESS;
import static com.jrmeza.equipmentcontrol.MainActivity.SCANNER_NAME;


/**
 * Created by juanmeza on 3/15/17.
 */
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
                String message = new String(buffer, 0 , numBytes);


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

