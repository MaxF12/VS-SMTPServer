/**
 * Created by maxfranke on 04.05.17.
 */

import com.sun.security.ntlm.Server;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.Set;


public class SMTPServer {

    private ServerSocket socket;

    private static Charset messageCharset = null;
    private static CharsetDecoder decoder = null;

    private static byte [] clientName = null;
    private static byte [] messageChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', ' '};


    public static void main(String [] args) {


        ServerSocketChannel serverChannel = null;
        InetSocketAddress remoteAddress = null;
        Selector selector = null;
        /** Create charset **/
        try {
            messageCharset = Charset.forName("US-ASCII");
        } catch(UnsupportedCharsetException uce) {
            System.err.println("Cannot create charset for this application. Exiting...");
            System.exit(1);
        }
        /** Exit if Port isn't specified **/
        if (args.length != 1) {
            System.out.println("Please specify the port");
            System.exit(1);
        }
        /** Get the Port**/
        int port = Integer.parseInt(args[0]);
        /** Initiate the Selector **/
        try {
            selector = Selector.open();
        } catch (IOException e1) {
            e1.printStackTrace();
            System.exit(1);
        }
        /** Initiate ServerChannel **/
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        /** Main loop **/
        while(true) {
            /** Check if selector is valid **/
            try {
                if(selector.select() == 0)
                    continue;
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            /** While there are keys left **/
            while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    try {

                        if (key.isReadable()){

                        }
                    }
            }
        }
    }
}

