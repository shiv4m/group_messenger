package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

import static java.lang.String.valueOf;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    private static String myPort;
    private static HashMap<String, String> porttoProc = new HashMap<String, String>();
    String[] AVD_PORTS = {"11108", "11112", "11116", "11120", "11124"};
    String avd_shut = "null";
    int counter=0;

    // thread for pink-ack runs forever in background untill an avd is shut down - https://developer.android.com/guide/components/processes-and-threads
    private void startThread(){
        new Thread(){
            @Override
            public void run(){
                try {
                    Thread.sleep(5000);
                    while (true) {
                        try {
                            Thread.sleep(1700);
                            for (int m = 0; m < AVD_PORTS.length; m++) {
                                try {
                                    Socket s = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[m]));
                                    s.setSoTimeout(2500);
                                    PrintWriter p = new PrintWriter(new BufferedWriter(new OutputStreamWriter((s.getOutputStream()))));
                                    BufferedReader b = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                    p.println("PING");
                                    p.flush();
                                    String readAck = b.readLine();
                                    if (readAck.equals("ACK"))
                                        s.close();
                                } catch (NullPointerException ne) {
                                    ne.printStackTrace();
                                    avd_shut = AVD_PORTS[m];
                                    Log.e("Null exception", "onThread");
                                    for (int z = 0; z < AVD_PORTS.length; z++) {
                                        try {
                                            if (AVD_PORTS[z].equals(avd_shut))
                                                continue;
                                            Socket s1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[m]));
                                            s1.setSoTimeout(2500);
                                            PrintWriter p = new PrintWriter(new BufferedWriter(new OutputStreamWriter((s1.getOutputStream()))));
                                            BufferedReader b = new BufferedReader(new InputStreamReader(s1.getInputStream()));
                                            p.println("shut-" + avd_shut);
                                            p.flush();
                                        } catch (IOException ee) {
                                            Log.e("IO exception", "sending failure via thread");
                                        }
                                    }
                                    break;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.e("IO exception", "onThread");
                                    avd_shut = AVD_PORTS[m];
                                    for (int z = 0; z < AVD_PORTS.length; z++) {
                                        try {
                                            if (AVD_PORTS[z].equals(avd_shut))
                                                continue;
                                            Socket s1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[m]));
                                            s1.setSoTimeout(2500);
                                            PrintWriter p = new PrintWriter(new BufferedWriter(new OutputStreamWriter((s1.getOutputStream()))));
                                            BufferedReader b = new BufferedReader(new InputStreamReader(s1.getInputStream()));
                                            p.println("shut-" + avd_shut);
                                            p.flush();
                                        } catch (IOException ee) {
                                            Log.e("IO exception", "sending failure via thread");
                                        }
                                    }
                                    break;
                                }
                            }
                            if (avd_shut != null)
                                break;
                        } catch (InterruptedException iE) {
                            iE.printStackTrace();
                        }
                    }
                }catch (InterruptedException il) {
                    il.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        //Most of the structure is from PA2A
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = valueOf((Integer.parseInt(portStr) * 2));
        Log.e("My Port ", myPort);

        porttoProc.put("11108", "1");
        porttoProc.put("11112", "2");
        porttoProc.put("11116", "3");
        porttoProc.put("11120", "4");
        porttoProc.put("11124", "5");

        //Most of the structure of code taken from PA2A
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());


        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        try{
            ServerSocket serverSocket = new ServerSocket(10000);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }catch (IOException e){
            e.printStackTrace();
            Log.e("message from socket", "can't create server");
        }

        Button button = (Button) findViewById(R.id.button4);
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg);
                editText.setText("");
            }
        });

        startThread();
    }

    //building a custom comparator for the priorityqueue to sort the sequence is ascending order - https://stackoverflow.com/questions/46891478/java-priorityqueue-with-custom-comparator/46893117
    class compareSequence implements Comparator<Message>{
        public int compare(Message m1, Message m2) {
            if(m1.sequence > m2.sequence)
                return 1;
            else  if(m1.sequence < m2.sequence)
                return  -1;
            return 0;
        }
    }
    class Message{
        public String messageTxt;
        public double sequence;
        public boolean isAgreed;
        public String originator;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void>{
        int keySeq = 0;
        @Override
        protected Void doInBackground(ServerSocket... sockets){
            String localProc = porttoProc.get(myPort);
            ServerSocket serverSocket = sockets[0];
            PriorityQueue<Message> holdBack= new PriorityQueue<Message>(50, new compareSequence());
            while(true){
                try{
                    counter += 1;
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout(2500);
                    BufferedReader bR = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter pf = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                    String message = bR.readLine();
                    if(message != null && message.startsWith("first")){
                        String[] mSplit = message.split("-");
                        String genSeq = String.valueOf(counter)+"."+localProc;
                        Message msg = new Message();
                        msg.isAgreed = false;
                        msg.messageTxt = mSplit[1];
                        msg.sequence = Double.parseDouble(genSeq);
                        msg.originator = mSplit[2];
                        holdBack.add(msg);
                        pf.println("proposal-" + genSeq + "-" + myPort);
                        pf.flush();
                    }
                    else if (message != null && message.startsWith("final")){
                        String[] finalSeq = message.split("-");
                        //sequence should be greater than all the previous sequences and agreed sequences
                        counter = Math.max(counter, (int)Double.parseDouble(String.valueOf(finalSeq[1]))) + 2;
                        Log.e("final sequence recieved", finalSeq[1] + " for proposal " + finalSeq[2]);
                        pf.println("ACK");
                        pf.flush();
                        //Iterate through the holdback to get object and it's sequence - https://stackoverflow.com/questions/8600724/how-to-iterate-through-list-of-object-arrays
                        Iterator iterator = holdBack.iterator();
                        Message nn = null;
                        while (iterator.hasNext()){
                            nn = (Message) iterator.next();
                            if(nn.sequence == Double.parseDouble(finalSeq[2])){
                                break;
                            }
                        }
                        if(nn != null){
                            holdBack.remove(nn);
                            nn.sequence = Double.parseDouble(finalSeq[1]);
                            nn.isAgreed = true;
                            holdBack.add(nn);
                        }
                        else{
                            Log.e("holdback", "No object found in holdback");
                        }

                        while(!holdBack.isEmpty()){
                            if(holdBack.peek() != null && holdBack.peek().isAgreed) {
                                Log.e("holdback", String.valueOf(holdBack.peek().sequence));
                                publishProgress(holdBack.peek().messageTxt);
                                holdBack.poll();
                            }
                            else if(holdBack.peek() != null && holdBack.peek().originator.equals(avd_shut)){
                                holdBack.poll();
                            }
                            else
                                break;

                        }
                    }else if(message != null && message.startsWith("shut")) {
                        socket.close();
                        String[] shutt = message.split("-");
                        avd_shut = shutt[1];
                        Log.e("avd shut down",avd_shut);
                    }else  if(message != null & message.startsWith("PING")){
                        pf.println("ACK");
                        pf.flush();
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        protected void onProgressUpdate(String... strings){
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append("\n"+ strings[0] + "\t\n");
            Log.e(String.valueOf(keySeq), strings[0]);
            ContentResolver mContentResolver;
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
            uriBuilder.scheme("content");
            ContentValues[] cv = new ContentValues[2];
            for (int i = 0; i < 1; i++) {
                cv[i] = new ContentValues();
                cv[i].put("key", keySeq);
                cv[i].put("value", strings[0]);
            }
            keySeq++;
            try {
                mContentResolver = getContentResolver();
                mContentResolver.insert(uriBuilder.build(), cv[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void>{
        String read;
        ArrayList<Double> collectProposals = new ArrayList<Double>();
        HashMap<String, String> portToProposed = new HashMap<String, String>();
        @Override
        protected Void doInBackground(String... msgs){
            for(int i = 0; i < AVD_PORTS.length; i++){
                try{
                    //if an avd is down skip the loop
                    if (AVD_PORTS[i].equals(avd_shut))
                        continue;

                    String msgToSend = msgs[0];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[i]));
                    socket.setSoTimeout(2500);
                    PrintWriter pF = new PrintWriter(new BufferedWriter(new OutputStreamWriter((socket.getOutputStream()))));
                    BufferedReader bR = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    pF.println("first-"+msgToSend+"-"+myPort);
                    pF.flush();
                    read = bR.readLine();
                    if (read != null && read.startsWith("proposal")){
                        socket.close();
                        String[] proposalMsg = read.split("-");
                        Log.e("Sequence recieved", read + " from avd "+ proposalMsg[2]);
                        collectProposals.add(Double.parseDouble(proposalMsg[1]));
                        portToProposed.put(proposalMsg[2], proposalMsg[1]);
                    }


                }catch (IOException e){
                    e.printStackTrace();
                    Log.e("Client Socket", "IO Exception at client");
                    avd_shut = AVD_PORTS[i];
                    Log.e("AVD shut down", AVD_PORTS[i] + " was shut down");
                    for(int k=0; k<AVD_PORTS.length; k++){
                        try {
                            if (AVD_PORTS[k].equals(avd_shut))
                                continue;
                            Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[k]));
                            PrintWriter pp = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket2.getOutputStream())));
                            pp.println("shut-" + avd_shut);
                            pp.flush();
                        }catch (IOException io){
                            io.printStackTrace();
                            Log.e("Error", "error sending failed node");
                        }
                    }
                }
            }

            //calculating maximum value and sending to all avds
            Object obj = Collections.max(collectProposals);
            for(int j=0; j<AVD_PORTS.length; j++){
                try{
                    if(AVD_PORTS[j].equals(avd_shut))
                        continue;
                    Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[j]));
                    socket1.setSoTimeout(2500);
                    PrintWriter pW = new PrintWriter(new BufferedWriter(new OutputStreamWriter((socket1.getOutputStream()))));
                    BufferedReader bb = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
                    pW.println("final-" + String.valueOf(obj) + "-" + portToProposed.get(AVD_PORTS[j]));
                    pW.flush();
                    String ack = bb.readLine();
                    if(ack.equals("ACK")){
                        socket1.close();
                    }
                }catch (NullPointerException ne){
                    ne.printStackTrace();
                    Log.e("Exception", "AVD " + AVD_PORTS[j] + " was shut down while sending final sequence");
                    avd_shut = AVD_PORTS[j];
                    for(int l=0; l<AVD_PORTS.length; l++){
                        try{
                            if(AVD_PORTS[l].equals(avd_shut))
                                continue;
                            Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[l]));
                            PrintWriter p3 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket3.getOutputStream())));
                            p3.println("shut-"+avd_shut);
                            p3.flush();
                        }catch (IOException ioe){
                            ioe.printStackTrace();
                            Log.e("Exception", "error sending failed node at sending final sequence");
                        }
                    }
                }
                catch (IOException e){
                    e.printStackTrace();
                    Log.e("Exception", "AVD " + AVD_PORTS[j] + " was shut down while sending final sequence");
                    avd_shut = AVD_PORTS[j];
                    for(int l=0; l<AVD_PORTS.length; l++){
                        try{
                            if(AVD_PORTS[l].equals(avd_shut))
                                continue;
                            Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(AVD_PORTS[l]));
                            PrintWriter p3 = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket3.getOutputStream())));
                            p3.println("shut-"+avd_shut);
                            p3.flush();
                        }catch (IOException ioe){
                            ioe.printStackTrace();
                            Log.e("Exception", "error sending failed node at sending final sequence");
                        }
                    }
                }
            }
            portToProposed.clear();
            collectProposals.clear();
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
