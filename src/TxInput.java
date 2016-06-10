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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class TxInput {

    private String txid;
    private int vout;
    private byte[] script;
    private int sequence;

    public TxInput(String txid, int vout, byte[] script, int sequence)
    {
        this.txid = txid;
        this.vout = vout;
        this.script = script;
        this.sequence = sequence;
    }

    public String getTxid()
    {
        return txid;
    }

    public int getVout()
    {
        return vout;
    }

    public static byte[] serialize(TxInput in, boolean unsigned)
    {
        byte[] txid = Utils.byteSwap(Utils.hexStringToByteArray(in.txid));
        byte[] vout = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(in.vout).array();
        byte[] script = null;
        if(unsigned) {
            script = new byte[1];
            script[0] = (byte) 0x00;
        }
        else
        {
            byte[] varInt = Utils.toVarInt(in.script.length);
            script = new byte[in.script.length + varInt.length];
            System.arraycopy(varInt, 0, script, 0, varInt.length);
            System.arraycopy(in.script, 0, script, varInt.length, in.script.length);
        }
        byte[] sequence = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(in.sequence).array();
        byte[] finalInput = new byte[txid.length + vout.length + script.length + sequence.length];
        System.arraycopy(txid, 0, finalInput, 0, txid.length);
        System.arraycopy(vout, 0, finalInput, txid.length, vout.length);
        System.arraycopy(script, 0, finalInput, txid.length + vout.length, script.length);
        System.arraycopy(sequence, 0, finalInput, txid.length + vout.length + script.length, sequence.length);
        return finalInput;
    }
}
