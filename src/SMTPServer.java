/**
 * Created by maxfranke on 04.05.17.
 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.StreamHandler;


public class SMTPServer {
    /** The port on which the Server runs**/
    private int port;

    /** The ServerSocket **/
    private ServerSocket socket;

    private int curId;
    /** The Channel that will handle the connections**/
    ServerSocketChannel serverChannel = null;

    /** The Selector we need to monitor **/
    Selector selector = null;

    /** The Path for the Directory **/
    Path path = null;

    private static Charset messageCharset = null;
    private static CharsetDecoder decoder = null;

    private static byte [] clientName = null;
    private static byte [] messageChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', ' '};

    StreamHandler txtOut;

    /** Controller for server Loop**/
    static boolean running=false;

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

            /** Create new ServerState and attach it to key**/
            SMTPServerState state = new SMTPServerState(curId);
            this.curId++;
            socketChannel.keyFor(this.selector).attach(state);
        } catch (ClosedChannelException e) {

            /**Channel is closed or blocked**/
            e.printStackTrace();
        } catch (IOException e) {

            /**Channels connection couldn't be accepted**/
            e.printStackTrace();
        }
    }

    /** Reads available message from channel and returns it as a String **/
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
        //String[] splited = message.split("\\r?\\n.\\r?\\n");

        System.out.println("read: "+message);

        return message;
    }
    /** Sends "message" in ASCII to Client **/
    private void sendMessage(SelectionKey key, String message) throws IOException {

        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buf = ByteBuffer.allocate(message.length());
        buf.clear();
        buf.put(message.getBytes(messageCharset));
        buf.flip();
        socketChannel.write(buf);
    }

    /** Returns the Operation from a String **/
    private String getOP(String payload) throws IOException{
        String[] splited = payload.split("\\s+");
        return splited[0];
    }

    /** Returns the Data from a String **/
    private String getSender(String payload) throws IOException{
        String[] splited = payload.split("\\:");
        return splited[1].substring(1);
    }

    //** Handles received Message and evaluates the new server state **/
    private void handleMSG(String payload, SelectionKey key, SMTPServerState state) throws  IOException{
        String OP = this.getOP(payload);

        //System.out.println(OP);
        switch (OP){
            case "HELO":
                state.setState(SMTPServerState.HELORECEIVED);
                break;
            case "MAIL":
                state.setFrom(this.getSender(payload));
                state.setState(SMTPServerState.MLFRMRECEIVED);
                break;
            case "RCPT":
                state.setTo(this.getSender(payload));
                state.setState(SMTPServerState.RCPTTORECEIVED);
                break;
            case "HELP":
                state.setState(SMTPServerState.HELPRECEIVED);
                break;
            case "DATA":
                state.setState(SMTPServerState.DATARECEIVED);
                break;
            case "QUIT":
                state.setState(SMTPServerState.QUITRECEIVED);
                break;
            default:
                state.setMessage(payload);
                createReceiverFolder(state.getTo());
                createReceiverFile(state.getFrom(),state.getTo(), payload);
                state.setState(SMTPServerState.MSGRECEIVED);
                break;
        }
        state.switchSent();
    }

    //** Determines the server response based on server state **/
    private void handleReply(SelectionKey key, SMTPServerState state) throws IOException{

        switch (state.getState()){
            case SMTPServerState.CONNECTED:
                this.sendMessage(key, "220 127.0.0.1\r\n");
                System.out.println("220 sent");
                break;
            case SMTPServerState.HELORECEIVED:
                this.sendMessage(key, "250 Privyet Tovarish\r\n");
                System.out.println("250 Privyet Tovarish sent");
                break;
            case SMTPServerState.MLFRMRECEIVED:
                this.sendMessage(key, "250 OK\r\n");
                System.out.println("250 OK\r\n");
                break;
            case SMTPServerState.RCPTTORECEIVED:
                this.sendMessage(key, "250 OK\r\n");
                System.out.println("250 OK\r\n");
                break;
            case SMTPServerState.DATARECEIVED:
                this.sendMessage(key, "354 start mail\r\n");
                System.out.println("354 start mail input sent");
                break;
            case SMTPServerState.HELPRECEIVED:
                this.sendMessage(key, "214 HELP\r\n");
                System.out.println("214 HELP sent");
                break;
            case SMTPServerState.QUITRECEIVED:
                this.sendMessage(key, "221 Closing Channel\r\n");
                key.channel().close();
                System.out.println("221 Closing Channel");
                break;
            case SMTPServerState.MSGRECEIVED:
                this.sendMessage(key, "250 OK\r\n");
                System.out.println("250 OK\r\n");
                break;
        }

        state.switchSent();
    }



    //** Creates receiver folder structure **/
    private void createReceiverFolder(ArrayList<String> arrayList){
        if(arrayList==null || arrayList.isEmpty())return;

        for(String s:arrayList){
            //get receiver name
            //String[] sender=s.split("@");
            //s=sender[0];
            System.out.println("Created new path: " + s);
            //add receiver to Path
            this.path = Paths.get("./Emails"+"/"+s);

            try {
                Files.createDirectory(path);
            } catch(FileAlreadyExistsException e){
                // the directory already exists.
                System.out.println("Emails exists");
            } catch (IOException e) {
                //something else went wrong
                e.printStackTrace();
            }}
    }

    //** Creates receiver files **/
    private void createReceiverFile(String from, ArrayList<String> to, String message)
    {
        Random rand= new Random();
        if(from==null || to==null || from.isEmpty() || to.isEmpty())
            return;

        for(String s:to)
        {
            //get receiver name
            //String[] sender=s.split("@");
            //s=sender[0];
            //System.out.println("s"+s);

            //add receiver to Path
            this.path = Paths.get("./Emails"+"/"+s+"/"+from+"_"+rand.nextInt(10000)+".txt");

            try {
                // Create the empty file with default permissions, etc.
                java.nio.file.Files.write(this.path,message.getBytes("utf-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (FileAlreadyExistsException x) {
                System.err.format("file named %s" +
                        " already exists%n", path);
            } catch (IOException x) {
                // Some other sort of failure, such as permissions.
                System.err.format("createFile error: %s%n", x);
            }
        }
    }


    /** Starts (and runs) the Server **/
    private void start() throws IOException{
        while(running) {

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

                SMTPServerState state = (SMTPServerState) key.attachment();

                if (!key.isValid()){
                    System.out.println("Invalid Key!");
                }

                if (key.isAcceptable()){
                    this.accept(key);
                }else if (key.isReadable() && state.hasSent()){
                    String payload = this.read(key);
                    this.handleMSG(payload, key, state);
                    //System.out.println("payload: "+payload);
                    /** TODO: Analyse payload and determine what state to put the Server in next **/
                }else if (key.isConnectable()){
                    System.out.println("Key can be connected");
                    /** TODO: Is the key ever connectable? **/

                }else if (key.isWritable() && !state.hasSent()){
                    this.handleReply(key, state);
                    /** TODO: Add handling for all other possible States **/
                }else if (key.isWritable() && state.hasSent()){
                    //Idle waiting

                }else if (!key.isWritable() && state.hasSent()){
                    //State never hit in tests

                }else if (!key.isWritable() && !state.hasSent()){
                    //State never hit in tests
                }else if (!key.isConnectable()){
                    System.out.println("Key cannot be connected");
                    /** TODO: Is the key ever connectable? **/
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
            /** Initiate ServerChannel failed**/
            e.printStackTrace();
            System.exit(1);
        }

        /** Initialize Path and create "Emails" directory if it doesn't already exist**/
        this.path = Paths.get("./Emails");
        try {
            Files.createDirectory(path);
        } catch(FileAlreadyExistsException e){
            // the directory already exists.
            System.out.println("Emails exists");
        } catch (IOException e) {
            //something else went wrong
            e.printStackTrace();
        }

        /** Init message Id**/
        this.curId = 0;
    }

    public static void main(String [] args) {

        /** Exit if Port isn't specified **/
        if (args.length != 1) {
            System.out.println("Please specify the port");
            System.exit(1);
        }
        /** Get the Port by parsing the first argument**/
        int port = Integer.parseInt(args[0]);
        SMTPServer server = new SMTPServer(port);

        try{
            /** Initializes the Server **/
            server.init();

            /** allow the Server to loop **/
            running=true;

            /** Starts the Server **/
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

