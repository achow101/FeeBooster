/*
 * Copyright (C) 2016 Andrew Chow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Transaction {

    private int version = 0;
    private long numInputs = 0;
    private long numOutputs = 0;
    private List<TxInput> inputs = new ArrayList<TxInput>();
    private List<TxOutput> outputs = new ArrayList<TxOutput>();
    private int locktime = 0;
    private String hash;
    private long size;
    private long fee = 0;
    private long outAmt = 0;
    private long totalAmtPre = 0;

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion()
    {
        return version;
    }

    public void setNumInputs(long numInputs) {
        this.numInputs = numInputs;
    }

    public long getNumInputs()
    {
        return numInputs;
    }

    public void setNumOutputs(long numOutputs) {
        this.numOutputs = numOutputs;
    }

    public long getNumOutputs()
    {
        return numOutputs;
    }

    public void addInput(TxInput in) {
        inputs.add(in);
    }

    public void addOutput(TxOutput out) {
        outputs.add(out);
    }

    public void setLocktime(int locktime) {
        this.locktime = locktime;
    }

    public int getLocktime()
    {
        return locktime;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public long getFee() {
        return fee;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize()
    {
        return size;
    }

    public long getTotalAmt()
    {
        return outAmt + fee;
    }

    public void setOutAmt(long outAmt)
    {
        this.outAmt = outAmt;
    }

    public long getOutAmt()
    {
        return outAmt;
    }

    public void setTotalAmtPre(long totalAmtPre)
    {
        this.totalAmtPre = totalAmtPre;
    }

    public List<TxInput> getInputs()
    {
        return inputs;
    }

    public List<TxOutput> getOutputs()
    {
        return outputs;
    }

    public static boolean deserializeStr(String txHex, Transaction tx)
    {
        try
        {
            int cursor = 0;
            byte[] txBytes = Utils.hexStringToByteArray(txHex);

            // Set the size
            tx.setSize(txBytes.length);

            // Get the version
            byte[] versionBytes = Arrays.copyOfRange(txBytes, 0, 4);
            ByteBuffer versionWrap = ByteBuffer.wrap(versionBytes).order(ByteOrder.LITTLE_ENDIAN);
            tx.setVersion(versionWrap.getInt());
            cursor = 4;

            // Get the number of inputs
            long numIns = Utils.parseVarInt(txBytes, cursor);
            tx.setNumInputs(numIns);
            cursor += Utils.varIntLen(txBytes, cursor);

            // Get inputs
            for (int in = 0; in < numIns; in++) {
                // Get outpoint
                byte[] txidBytes = Arrays.copyOfRange(txBytes, cursor, cursor + 32);
                cursor += 32;
                byte[] voutBytes = Arrays.copyOfRange(txBytes, cursor, cursor + 4);
                cursor += 4;
                ByteBuffer voutWrap = ByteBuffer.wrap(voutBytes).order(ByteOrder.LITTLE_ENDIAN);
                int vout = voutWrap.getInt();

                // Get the script
                int scriptlen = (int) Utils.parseVarInt(txBytes, cursor);
                cursor += Utils.varIntLen(txBytes, cursor);
                byte[] script = Arrays.copyOfRange(txBytes, cursor, cursor + scriptlen);
                cursor += scriptlen;

                // Get the sequence
                byte[] sequenceBytes = Arrays.copyOfRange(txBytes, cursor, cursor + 4);
                ByteBuffer sequenceWrap = ByteBuffer.wrap(sequenceBytes).order(ByteOrder.LITTLE_ENDIAN);
                int sequence = sequenceWrap.getInt();
                cursor += 4;

                // Create the input and add to list
                TxInput input = new TxInput(Utils.bytesToHex(Utils.byteSwap(txidBytes)), vout, script, sequence);
                tx.addInput(input);
            }

            // Get the number of outputs
            long numOuts = Utils.parseVarInt(txBytes, cursor);
            tx.setNumOutputs(numOuts);
            cursor += Utils.varIntLen(txBytes, cursor);

            // Get Outputs
            for (int out = 0; out < numOuts; out++) {
                // Get value
                byte[] valueBytes = Arrays.copyOfRange(txBytes, cursor, cursor + 8);
                ByteBuffer valueWrap = ByteBuffer.wrap(valueBytes).order(ByteOrder.LITTLE_ENDIAN);
                long value = valueWrap.getLong();
                cursor += 8;

                // Get script
                int scriptlen = (int) Utils.parseVarInt(txBytes, cursor);
                cursor += Utils.varIntLen(txBytes, cursor);
                byte[] script = Arrays.copyOfRange(txBytes, cursor, cursor + scriptlen);
                cursor += scriptlen;

                // Create output and add to list
                TxOutput output = new TxOutput(value, script);
                tx.addOutput(output);

                // Add amount
                tx.setOutAmt(tx.getOutAmt() + value);
            }

            // Get the locktime
            byte[] locktimeBytes = Arrays.copyOfRange(txBytes, cursor, cursor + 4);
            ByteBuffer locktimeWrap = ByteBuffer.wrap(locktimeBytes).order(ByteOrder.LITTLE_ENDIAN);
            int locktime = locktimeWrap.getInt();
            tx.setLocktime(locktime);

            // Get the hash
            SHA256Digest sha256 = new SHA256Digest();
            sha256.update(txBytes, 0, txBytes.length);
            byte[] hash1 = new byte[32];
            sha256.doFinal(hash1, 0);
            SHA256Digest sha256_2 = new SHA256Digest();
            sha256_2.update(hash1, 0, hash1.length);
            byte[] hash2 = new byte[32];
            sha256_2.doFinal(hash2, 0);
            tx.setHash(Utils.bytesToHex(Utils.byteSwap(hash2)));
        }
        catch(Exception e)
        {
            return false;
        }

        return true;
    }

    public static byte[] serialize(Transaction tx, boolean unsigned)
    {
        byte[] version = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(tx.getVersion()).array();
        byte[] inVarInt = Utils.toVarInt(tx.getNumInputs());
        List<Byte> inBytes = new ArrayList<Byte>();
        for(TxInput input : tx.getInputs())
        {
            byte[] inputBytes = TxInput.serialize(input, unsigned);
            for(byte b : inputBytes)
            {
                inBytes.add(b);
            }
        }
        byte[] outVarInt = Utils.toVarInt(tx.getNumOutputs());
        List<Byte> outBytes = new ArrayList<Byte>();
        for(TxOutput output : tx.getOutputs())
        {
            byte[] outputBytes = TxOutput.serialize(output);
            for(byte b : outputBytes)
            {
                outBytes.add(b);
            }
        }
        byte[] locktime = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(tx.getLocktime()).array();
        byte[] unsignedTx = new byte[4 + inVarInt.length + inBytes.size() + outVarInt.length + outBytes.size() + 4];
        System.arraycopy(version, 0, unsignedTx, 0, version.length);
        System.arraycopy(inVarInt, 0, unsignedTx, version.length, inVarInt.length);
        System.arraycopy(Utils.byteArrToPrimitive(inBytes.toArray()), 0, unsignedTx, version.length + inVarInt.length, inBytes.size());
        System.arraycopy(outVarInt, 0, unsignedTx, version.length + inVarInt.length + inBytes.size(), outVarInt.length);
        System.arraycopy(Utils.byteArrToPrimitive(outBytes.toArray()), 0, unsignedTx, version.length + inVarInt.length + inBytes.size() + outVarInt.length, outBytes.size());
        System.arraycopy(locktime, 0, unsignedTx, version.length + inVarInt.length + inBytes.size() + outVarInt.length + outBytes.size(), locktime.length);
        return unsignedTx;
    }
}
