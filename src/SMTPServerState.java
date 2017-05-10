/**
 * Created by maxfranke on 07.05.17.
 */

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SMTPServerState {

    public final static int CONNECTED = 0;
    public final static int HELORECEIVED = 1;
    public final static int MLFRMRECEIVED = 2;
    public final static int RCPTTORECEIVED = 3;
    public final static int DATARECEIVED = 4;
    public final static int MSGRECEIVED = 5;
    public final static int QUITRECEIVED = 6;
    public final static int HELPRECEIVED = 7;

    /** TODO: Add the remaining states **/

    private int state;
    private int previousState;
    private String  from;
    private ArrayList<String> to;
    private String message;
    private boolean sent;
    private int id;

    public SMTPServerState(int id) {
        this.state = CONNECTED;
        this.sent = false;
        this.to  = new ArrayList<String>();
        this.id = id;
    }

    public int getId() {return this.id;}

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getFrom() {
        return this.from;
    }

    public void setFrom(String from){this.from = from;}

    public ArrayList<String> getTo() {
        return to;
    }

    public void setTo(String to) {this.to.add(to);}

    public void switchSent() {if (this.sent) this.sent = false; else this.sent = true;}

    public boolean hasSent() {return this.sent;}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getPreviousState() {
        return previousState;
    }

    public void setPreviousState(int previousState) {
        this.previousState = previousState;
    }
}
