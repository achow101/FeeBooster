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

import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class TxOutput {

    private long value;
    private byte[] script;

    public TxOutput(long value, byte[] script)
    {
        this.value = value;
        this.script = script;
    }

    public long getValue()
    {
        return value;
    }

    public byte[] getScript()
    {
        return script;
    }

    public String getAddress()
    {
        // P2PKH Address
        if(script[0] == 0x76)
        {
            byte[] hash160 = Arrays.copyOfRange(script, 3, 23);
            return Utils.base58Encode((byte)0x00, hash160);
        }
        // P2SH Address
        else if(Utils.getUnsignedByte(script[0]) == 0xa9)
        {
            byte[] hash160 = Arrays.copyOfRange(script, 2, 22);
            return Utils.base58Encode((byte)0x05, hash160);
        }
        // P2PK Address Uncompressed
        else if(Utils.getUnsignedByte(script[0]) == 0x41
                && Utils.getUnsignedByte(script[1]) == 0x04
                && Utils.getUnsignedByte(script[script.length - 1]) == 0xac)
        {
            byte[] pk = Arrays.copyOfRange(script, 1, 66);
            SHA256Digest sha256 = new SHA256Digest();
            sha256.update(pk, 0, pk.length);
            byte[] sha256out = new byte[32];
            sha256.doFinal(sha256out, 0);
            RIPEMD160Digest ripemd160 = new RIPEMD160Digest();
            ripemd160.update(sha256out, 0, sha256out.length);
            byte[] ripemd160out = new byte[20];
            ripemd160.doFinal(ripemd160out, 0);
            return Utils.base58Encode((byte)0x00, ripemd160out);
        }
        // P2PK Address Compressed
        else if((Utils.getUnsignedByte(script[1]) == 0x03
                || Utils.getUnsignedByte(script[1]) == 0x02)
                && Utils.getUnsignedByte(script[0]) == 21
                && Utils.getUnsignedByte(script[script.length - 1]) == 0xac)
        {
            byte[] pk = Arrays.copyOfRange(script, 1, 34);
            SHA256Digest sha256 = new SHA256Digest();
            sha256.update(pk, 0, pk.length);
            byte[] sha256out = new byte[32];
            sha256.doFinal(sha256out, 0);
            RIPEMD160Digest ripemd160 = new RIPEMD160Digest();
            ripemd160.update(sha256out, 0, sha256out.length);
            byte[] ripemd160out = new byte[20];
            ripemd160.doFinal(ripemd160out, 0);
            return Utils.base58Encode((byte)0x00, ripemd160out);
        }
        // Nonstandard
        else
        {
            return "Non Standard Output";
        }
    }

    public void decreaseValueBy(long decrease)
    {
        value -= decrease;
    }

    public static byte[] serialize(TxOutput out)
    {
        byte[] valueBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(out.value).array();
        byte[] scriptVarInt = Utils.toVarInt(out.getScript().length);
        byte[] finalOutput = new byte[8 + scriptVarInt.length + out.getScript().length];
        System.arraycopy(valueBytes, 0, finalOutput, 0, valueBytes.length);
        System.arraycopy(scriptVarInt, 0, finalOutput, valueBytes.length, scriptVarInt.length);
        System.arraycopy(out.getScript(), 0, finalOutput, valueBytes.length + scriptVarInt.length, out.getScript().length);
        return finalOutput;
    }
}
