/**
 * Created by maxfranke on 04.05.17.
 */

import com.sun.security.ntlm.Server;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;
import java.util.Set;


public class SMTPServer {
    /** The port on which the Server runs**/
    private int port;

    /** The ServerSocket **/
    private ServerSocket socket;

    /** The Channel that will handle the connections**/
    ServerSocketChannel serverChannel = null;

    /** The Selector we need to monitor **/
    Selector selector = null;

    private static Charset messageCharset = null;
    private static CharsetDecoder decoder = null;

    private static byte [] clientName = null;
    private static byte [] messageChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', ' '};

    public SMTPServer(int port) {
        this.port = port;
    }

    /** Accepts a Connection, registers new channel to monitor **/
    private void accept(SelectionKey key) throws IOException{
        System.out.println("Key can be accepted");

        /** get Channel from key **/
        ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();

        /** Try to accept the connection **/
        try {
            /** Create new Channel for connection **/
            SocketChannel socketChannel = keyChannel.accept();

            /** Print remote Address **/
            System.out.println("Connected to " + socketChannel.getRemoteAddress());

            /** Set new channel to non Blocking **/
            socketChannel.configureBlocking(false);

            /** Register new Channel with Selector **/
            socketChannel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            /** Create new ServerState **/
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Reads availavle message from channel and returns it as a String **/
    private String read(SelectionKey key) throws IOException {

        SocketChannel socketChannel = (SocketChannel) key.channel();

        ByteBuffer buf = ByteBuffer.allocate(8192);

        int len = 0;

        /** reads the Channel data into buf **/
        try{
            len = socketChannel.read(buf);
        }catch (IOException e) {
            e.printStackTrace();
        }

        buf.flip();

        /** Create Byte Array **/
        byte[] bytes = new byte[buf.remaining()];

        /** Load Bytes from buf into bytes **/
        buf.get(bytes);

        /** Turn bytes into String using messageCharset format **/
        String message = new String(bytes, messageCharset);

        System.out.print(message);

        return message;
    }
    private void sendMessage(SelectionKey key, String message) throws IOException {

        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocate(message.length());
        buf.clear();

        buf.put(message.getBytes(messageCharset));

        buf.flip();

        socketChannel.write(buf);
    }
    /** Sends "220" to the Client **/
    private void send220(SelectionKey key) throws IOException{

        this.sendMessage(key, "220 127.0.0.1\r\n");

    }
    /** Starts (and runs) the Server **/
    private void start() throws IOException{
        while(true) {

            /** Create Channel and check for new Connections **/
            try {
                if (this.selector.select() == 0)
                    continue;
            } catch (IOException e) {
                e.printStackTrace();
            }
            Set<SelectionKey> selectionKeys = this.selector.selectedKeys();
            Iterator<SelectionKey> iter = selectionKeys.iterator();
            while(iter.hasNext()){
                //System.out.println("Key available");
                SelectionKey key = (SelectionKey) iter.next();
                iter.remove();

                if (!key.isValid()){
                    System.out.println("Invalid Key!");
                }

                if (key.isAcceptable()){
                    this.accept(key);
                }else if (key.isReadable()){
                    String payload;
                    if (key.attachment() != null){
                        payload = this.read(key);
                    }
                    /** TODO: Analyse payload and determine what state to put the Server in next **/
                }else if (key.isConnectable()){
                    System.out.println("Key can be connected");
                    /** TODO: Is the key ever connectable? **/
                }else if (key.isWritable()){
                    if (key.attachment() == null){
                        SMTPServerState state = new SMTPServerState();
                        key.attach(state);
                    }
                    //System.out.println("Key can be written");
                    SMTPServerState state = (SMTPServerState) key.attachment();
                    if (state.getState() == SMTPServerState.CONNECTED){
                        this.send220(key);
                        System.out.println("220 sent");
                        state.setState(SMTPServerState.HELORECEIVED);
                    }
                    /** TODO: Add handling for all other possible States **/
                }
            }
        }
    }

    /** Initialises the Server **/
    private void init() throws IOException {
        /** Create charset **/
        try {
            messageCharset = Charset.forName("US-ASCII");
        } catch(UnsupportedCharsetException uce) {
            System.err.println("Cannot create charset for this application. Exiting...");
            System.exit(1);
        }
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
            serverChannel.socket().bind(new InetSocketAddress(this.port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String [] args) {

        /** Exit if Port isn't specified **/
        if (args.length != 1) {
            System.out.println("Please specify the port");
            System.exit(1);
        }
        /** Get the Port**/
        int port = Integer.parseInt(args[0]);
        SMTPServer server = new SMTPServer(port);

        try{
            /** Initialises the Server **/
            server.init();

            /** Starts the Server **/
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

