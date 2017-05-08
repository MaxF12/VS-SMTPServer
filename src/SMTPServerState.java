/**
 * Created by maxfranke on 07.05.17.
 */

import java.nio.ByteBuffer;

public class SMTPServerState {

    public final static int CONNECTED = 0;
    public final static int HELORECEIVED = 1;
    /** TODO: Add the remaining states **/

    private int state;
    private int previousState;
    private ByteBuffer buffer;
    private byte [] from;
    private byte [] to;
    private byte [] message;

    public void SMTPClientState() {
        this.state = CONNECTED;
        this.buffer = ByteBuffer.allocate(8192);
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public ByteBuffer getByteBuffer() {
        return this.buffer;
    }

    public byte[] getFrom() {
        return this.from;
    }

    public void setFrom(byte[] from){
        this.from = from;
    }
    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public int getPreviousState() {
        return previousState;
    }

    public void setPreviousState(int previousState) {
        this.previousState = previousState;
    }
}
