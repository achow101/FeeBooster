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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class Utils {

    public static long parseVarInt(byte[] bytes, int offset)
    {
        byte[] varint;
        if(bytes[offset] == 0xfd)
        {
            varint = Arrays.copyOfRange(bytes, offset + 1, offset + 3);
            ByteBuffer varintwrap = ByteBuffer.wrap(varint).order(ByteOrder.LITTLE_ENDIAN);
            return (long) getUnsignedShortBuf(varintwrap);
        }
        else if(bytes[offset] == 0xfe)
        {
            varint = Arrays.copyOfRange(bytes, offset + 1, offset + 5);
            ByteBuffer varintwrap = ByteBuffer.wrap(varint).order(ByteOrder.LITTLE_ENDIAN);
            return (long) getUnsignedIntBuf(varintwrap);
        }
        else if(bytes[offset] == 0xff)
        {
            varint = Arrays.copyOfRange(bytes, offset + 1, offset + 9);
            ByteBuffer varintwrap = ByteBuffer.wrap(varint).order(ByteOrder.LITTLE_ENDIAN);
            return varintwrap.getLong();
        }
        else
        {
            varint = new byte[]{bytes[offset]};
            ByteBuffer varintwrap = ByteBuffer.wrap(varint).order(ByteOrder.LITTLE_ENDIAN);
            return (long) getUnsignedByteBuf(varintwrap);
        }
    }

    public static byte[] toVarInt(long varint)
    {
        if(varint <= 252)
        {
            return ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN).put((byte)varint).array();
        }
        else if(varint > 252 && varint <= 0xffff)
        {
            byte[] out = new byte[3];
            out[0] = (byte)0xfd;
            System.arraycopy(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short)varint).array(), 0,
                    out, 1, 2);
        }
        else if(varint > 0xffff && varint <= 0xffffffff)
        {
            byte[] out = new byte[5];
            out[0] = (byte)0xfe;
            System.arraycopy(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)varint).array(), 0,
                    out, 1, 4);
        }
        else if(varint > 0xffffffff && varint <= Long.parseLong("ffffffffffffffff", 16))
        {
            byte[] out = new byte[9];
            out[0] = (byte)0xff;
            System.arraycopy(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(varint).array(), 0,
                    out, 1, 4);
        }
        return null;
    }

    public static int varIntLen(byte[] bytes, int offset)
    {
        if(bytes[offset] == 0xfd)
            return 3;
        else if(bytes[offset] == 0xfe)
            return 5;
        else if(bytes[offset] == 0xff)
            return 9;
        else
            return 1;
    }

    public static byte[] hexStringToByteArray(String s) {
        if(!s.matches("[0-9A-Fa-f]+"))
            throw new IllegalArgumentException();
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static byte[] byteSwap(byte[] bytes)
    {
        for(int i = 0; i < bytes.length / 2; i++)
        {
            byte temp = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = temp;
        }
        return bytes;
    }

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    final private static char[] base58Array = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    public static String base58Encode(byte version, byte[] payload) {

        // Create bytes that will be converted
        byte[] concat = new byte[payload.length + 1];
        concat[0] = version;
        System.arraycopy(payload, 0, concat, 1, payload.length);
        byte[] checksumHash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            checksumHash = digest.digest(digest.digest(concat));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] bytes = new byte[concat.length + 4];
        System.arraycopy(concat, 0, bytes, 0, concat.length);
        System.arraycopy(checksumHash, 0, bytes, concat.length, 4);

        // Do the conversion
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                sb.append(1);
            } else {
                break;
            }
        }

        byte[] temp = new byte[bytes.length + 1];

        for (int i = 1; i < temp.length; i++) {
            temp[i] = bytes[i - 1];
        }

        BigInteger n = new BigInteger(temp);

        BigInteger d = BigInteger.valueOf(58);

        BigInteger[] result;

        StringBuilder sb2 = new StringBuilder();

        while (n.compareTo(BigInteger.ZERO) != 0) {

            result = n.divideAndRemainder(d);
            BigInteger div = result[0];
            BigInteger rem = result[1];
            n = div;
            sb2.append(base58Array[rem.intValue()]);
        }

        return sb.toString() + sb2.reverse().toString();
    }

    public static byte[] byteArrToPrimitive(Object[] bytes)
    {
        byte[] outBytes = new byte[bytes.length];
        for(int i = 0; i < bytes.length; i++)
        {
            outBytes[i] = (byte)bytes[i];
        }
        return outBytes;
    }

    public static short getUnsignedByteBuf(ByteBuffer bb)
    {
        return ((short)(bb.get() & 0xff));
    }

    public static int getUnsignedShortBuf(ByteBuffer bb)
    {
        return (bb.getShort() & 0xffff);
    }

    public static long getUnsignedIntBuf(ByteBuffer bb)
    {
        return ((long)bb.getInt() & 0xffffffffL);
    }

    public static short getUnsignedByte(byte b)
    {
        return ((short)(b & 0xff));
    }

    public static JSONObject getFromAnAPI(String url, String method)
    {
        try {
            URL apiUrl = new URL(url);
            HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
            con.setRequestMethod(method);
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36");
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Set JSON result
                JSONObject result = new JSONObject(response.toString());

                return result;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new JSONObject("{\"Error\":\"Failed\"}");
    }
}
