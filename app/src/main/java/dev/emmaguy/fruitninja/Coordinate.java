package dev.emmaguy.fruitninja;

import java.nio.ByteBuffer;

public class Coordinate {

    public static final long NO_TIME = -1;
    public static final int NUM_BYTES = 33;
    // 25: 8 for Long, 3 * 8 for double and 1 for boolean


    private static int maxDim = 3;
    public double[] mCoor = new double[maxDim];

    public long timestamp;
    public boolean reset;
    public boolean pressLeftMouse;
    public boolean pressRightMouse;


    public Coordinate(double[] coor){
        for (int i = 0; i < coor.length; i++) {
            this.mCoor[i] = coor[i];
        }
        this.timestamp = NO_TIME;
        this.reset = false;
        this.pressLeftMouse = false;
        this.pressRightMouse = false;
    }

    public Coordinate(double[] coor, long timestamp){
        for (int i = 0; i < coor.length; i++) {
            this.mCoor[i] = coor[i];
        }
        this.timestamp = timestamp;
        this.reset = false;
        this.pressLeftMouse = false;
        this.pressRightMouse = false;
    }

    public Coordinate(double[] coor, long timestamp, boolean reset){
        for (int i = 0; i < coor.length; i++) {
            this.mCoor[i] = coor[i];
        }
        this.timestamp = timestamp;
        this.reset = reset;
        this.pressLeftMouse = false;
        this.pressRightMouse = false;
    }

    @Override
    public String toString(){
        return "x = " + mCoor[0] + ", y = " + mCoor[1] + ", z = " + mCoor[2] +
                ", timestamp = " + timestamp + ", reset = " + reset +
                ", pressLeftMouse = " + pressLeftMouse + ", pressRightMouse = " + pressRightMouse;
    }

    public byte[] toByte() {
        byte[] output = new byte[NUM_BYTES];
        ByteBuffer buf = ByteBuffer.wrap(output);
        buf.putLong(timestamp);
        for (int i = 0; i < maxDim; i++) {
            buf.putDouble(mCoor[i]);
        }
        byte tmp = (byte) (reset? 1: 0);
        tmp |= (byte) (pressRightMouse ? 1 << 1 : 0);
        tmp |= (byte) (pressLeftMouse ? 1 << 2 : 0);
        buf.put(tmp);
        return output;
    }

    public void toBytes(byte[] buffer, int start) {
        ByteBuffer buf = ByteBuffer.wrap(buffer, start, NUM_BYTES);
        buf.putLong(timestamp);
        for (int i = 0; i < maxDim; i++) {
            buf.putDouble(mCoor[i]);
        }
        byte tmp = (byte) (reset? 1: 0);
        tmp |= (byte) (pressRightMouse ? (1 << 1) : 0);
        tmp |= (byte) (pressLeftMouse ? (1 << 2) : 0);
        buf.put(tmp);
    }

    static public Coordinate fromByte(byte b[]) {
        ByteBuffer buf = ByteBuffer.wrap(b);
        long timestamp = buf.getLong();
        double[] xyz = new double[maxDim];
        for (int i = 0; i < maxDim; i++) {
            xyz[i] = buf.getDouble();
        }
        byte tmp = buf.get();
        boolean reset = (tmp & 1) != 0;
        Coordinate ret = new Coordinate(xyz, timestamp, reset);
        ret.pressRightMouse = (tmp & (1 << 1)) != 0;
        ret.pressLeftMouse = (tmp & (1 << 2)) != 0;
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if((o == null) || (this.getClass() != o.getClass())){
            return false;
        }
        Coordinate toCmp = (Coordinate) o;

        boolean ret = true;
        for (int i = 0; i < maxDim; i++) {
            ret = ret && toCmp.mCoor[i] == this.mCoor[i];
        }
        return ret && toCmp.timestamp == this.timestamp && toCmp.reset == this.reset
                && toCmp.pressLeftMouse == this.pressLeftMouse && toCmp.pressRightMouse == this.pressRightMouse;
    }
}
