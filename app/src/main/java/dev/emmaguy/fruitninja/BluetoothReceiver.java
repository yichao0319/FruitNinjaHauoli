package dev.emmaguy.fruitninja;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import dev.emmaguy.fruitninja.ui.GameSurfaceView;

public class BluetoothReceiver {


    private String TAG = "BluetoothReceiver";

    // Objects
    private BluetoothAdapter mBluetoothAdapter;
    private AcceptThread socketThread;
    private GameSurfaceView mGameSurfaceView;
    // private Context context;

    // Constants
    private static final int MAX_BYTE_LEN = 4 * Coordinate.NUM_BYTES;
    private static final long IGNORE_COUNT = 0;
    private static boolean sendBack = false;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final boolean DEBUG = false;

    // Setup
    private static int maxDim = 3;
    public final double[][] RANGES = {{0.05, 0.17},{-0.1, -0.25},{0, 0.3}};
    public static float[] METER_TO_PIXEL_SCALE = new float[maxDim];

    // Variables
    private boolean startingState;
    long count;
    byte[] buffer = new byte[MAX_BYTE_LEN];
    byte[] coorBuffer = new byte[Coordinate.NUM_BYTES];


    public BluetoothReceiver(GameSurfaceView aGameSurfaceView, float screen_height, float screen_width) {
        // this.context      = context;
        this.mGameSurfaceView    = aGameSurfaceView;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        count = 0;
        startingState = false;

        // If this device cannot support bluetooth, we cannot work at all.
        // The safest way is to close this app.
        if (mBluetoothAdapter == null) {

        }

        METER_TO_PIXEL_SCALE[0] = (float)(screen_width  / (RANGES[0][1]-RANGES[0][0]));
        METER_TO_PIXEL_SCALE[1] = (float)(screen_height / (RANGES[1][1]-RANGES[1][0]));
        METER_TO_PIXEL_SCALE[2] = (float)(255 / (RANGES[2][1]-RANGES[2][0]));
    }

    public boolean isStarting() {
        return startingState;
    }

    public synchronized void start() {
        if(socketThread == null) {
            socketThread = new AcceptThread(mGameSurfaceView);
            socketThread.start();
            startingState = true;
        }
    }

    public synchronized void stop() {
        if(socketThread != null) {
            socketThread.cancel();
            socketThread.interrupt();
            socketThread = null;
            startingState = false;
        }
    }

    /**
     * AcceptThread waits and receives information (coordinates) sent by bluetooth sender
     * Then, it ignore several initial coordinates and pass those coordinates to further use.
     */
    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;
        private boolean waitingAndReceiving;
        GameSurfaceView mGameSurfaceView;

        public AcceptThread(GameSurfaceView aGameSurfaceView) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            mmServerSocket = null;
            waitingAndReceiving = true;
            this.mGameSurfaceView = aGameSurfaceView;
        }

        public void run() {
            // Waiting for connection
            BluetoothServerSocket tmp = null;
            while(mmServerSocket == null && waitingAndReceiving) {
                try {
                    // MY_UUID is the app's UUID string, also used by the client code.
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("AcceptThread", MY_UUID);
                } catch (Exception e) {
                    //Nothing because waiting for connection
                }
                mmServerSocket = tmp;
            }
            BluetoothSocket socket = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            // Keep listening until exception occurs or a socket is returned.
            while (waitingAndReceiving) {
                try {
                    Log.d("CheckStatus", "BT wait for accept()");
                    socket = mmServerSocket.accept();
                    Log.d("CheckStatus", "BT accept new connection");

                    inputStream = socket.getInputStream();
                    outputStream = sendBack ? socket.getOutputStream(): null;
                    long sumWaitTime = 0;
                    long sumAddCoorTime = 0;
                    long sumWholeTime = 0;
                    int waitCount = 0, coorCount = 0, wholeCount = 0;
                    long waitStartTime = -1;
                    while(waitingAndReceiving) {
                        if(waitStartTime == -1) {
                            waitStartTime = System.currentTimeMillis();
                        }

                        if (inputStream.available() <= 0) {
                            continue;
                        }

                        int inputByteNum = inputStream.read(buffer);
                        if(inputByteNum == -1) {
                            break;
                        }
                        long waitEndTime = System.currentTimeMillis();
                        long receiveTime = waitEndTime - waitStartTime;
                        sumWaitTime += receiveTime;
                        ++waitCount;

                        if(DEBUG) {
                            Log.d(TAG, "Wait Input at " + waitStartTime + ", Read Data at " + waitEndTime);
                            Log.d(TAG, "Receive Data takes time " + receiveTime + ", Average Reading Takes " + (float)sumWaitTime / waitCount);
                        }

                        if (sendBack && inputByteNum > 0) {
                            Coordinate coordinate = Coordinate.fromByte(buffer);
                            outputStream.write(coordinate.toByte(), 0, Coordinate.NUM_BYTES);
                        }

                        int numCoordinate = inputByteNum / Coordinate.NUM_BYTES;
                        //System.out.println("numCoordinates = " + numCoordinate);
                        // We ignore several initial estimations.
                        if (count < IGNORE_COUNT) {
                            count += numCoordinate;
                        } else {
                            long coorStartTime = System.currentTimeMillis();
                            for (int i = 0; i < numCoordinate; ++i) {
                                System.arraycopy(buffer, i * Coordinate.NUM_BYTES, coorBuffer, 0, Coordinate.NUM_BYTES);
                                Coordinate coordinate = Coordinate.fromByte(coorBuffer);

                                // eventSimulator.addCoordinate(coordinate);
                                coordinate = convertMetersToScreenPixel(coordinate);
                                mGameSurfaceView.RemoteTouchEvent(coordinate);
                            }
                            long coorEndTime = System.currentTimeMillis();
                            long addCoorTime = coorEndTime - coorStartTime;
                            sumAddCoorTime += addCoorTime;
                            ++coorCount;
                            if(DEBUG) {
                                Log.d(TAG, "Wait Coord at " + coorStartTime + ", Added Coordinate at " + coorEndTime);
                                Log.d(TAG, "Adding Coordinate takes time " + addCoorTime + ", Average Adding Takes " + (float)sumAddCoorTime / coorCount);
                            }
                        }
                        if(DEBUG) {
                            long wholeTime = System.currentTimeMillis() - waitStartTime;
                            sumWholeTime += wholeTime;
                            ++wholeCount;
                            Log.d(TAG, "Whole Time = " + wholeTime + ", Average Whole Time = " + (float)sumWholeTime / wholeCount);
                        }
                        waitStartTime = -1;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }
            }

            try {
                if(inputStream != null) {
                    inputStream.close();
                }
                if(outputStream != null) {
                    outputStream.close();
                }
                if(mmServerSocket != null) {
                    mmServerSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            waitingAndReceiving = false;
        }


        private Coordinate convertMetersToScreenPixel(Coordinate coor) {
            for (int di = 0; di < maxDim; di++) {
                coor.mCoor[di] = (coor.mCoor[di]-RANGES[di][0]) * METER_TO_PIXEL_SCALE[di];
            }

            return coor;
        }
    }
}
