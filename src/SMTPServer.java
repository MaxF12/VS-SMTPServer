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

    /**  public SMTPServer(int port) throws IOException {
        socket = new ServerSocket(port);
    }
    public void startServer() {
        while (true){
            try{
                System.out.println("Waiting for Client connection on Port " + socket.getLocalPort());
                Socket server = socket.accept();

                System.out.println("Established connection to " + server.getRemoteSocketAddress());
            }catch(IOException e) {
                e.printStackTrace();
                break;
            }
        }

    } **/

    public static void main(String [] args) {


        ServerSocketChannel serverChannel = null;
        InetSocketAddress remoteAddress = null;
        Selector selector = null;

        try {
            messageCharset = Charset.forName("US-ASCII");
        } catch(UnsupportedCharsetException uce) {
            System.err.println("Cannot create charset for this application. Exiting...");
            System.exit(1);
        }

        if (args.length != 1) {
            System.out.println("Please specify the port");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        /**try {
            SMTPServer s = new SMTPServer(port);
            s.startServer();
        }catch (IOException e) {
            e.printStackTrace();
        }**/
        try {
            selector = Selector.open();
        } catch (IOException e1) {
            e1.printStackTrace();
            System.exit(1);
        }
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(6332));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);


        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

