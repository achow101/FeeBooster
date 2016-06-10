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

import org.bitcoinj.core.*;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;

import java.io.*;
import java.net.InetSocketAddress;

public class Broadcaster {

    public static void broadcastTransaction(byte[] txBytes)
    {
        NetworkParameters params = new MainNetParams();
        Transaction tx = new Transaction(params, txBytes);
        PeerGroup peerGroup = new PeerGroup(params);
        peerGroup.setUserAgent("FeeBooster", "1.0");
        peerGroup.start();
        try {
            BufferedReader br = new BufferedReader(new FileReader("nodes.txt"));
            String line;
            while((line  = br.readLine()) != null)
            {
                int colonIndex = line.indexOf(":");
                String ip = line.substring(0, colonIndex);
                int port = Integer.parseInt(line.substring(colonIndex + 1));
                peerGroup.connectTo(new InetSocketAddress(ip, port));
                peerGroup.broadcastTransaction(tx);
                line = null;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
