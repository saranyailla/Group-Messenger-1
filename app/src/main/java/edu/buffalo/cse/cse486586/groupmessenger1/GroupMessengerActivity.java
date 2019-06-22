package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Objects;

public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    //Hardcoding the values of avd port numbers
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    //Sequence number of messages when storing them as (key,value) pair.
    static int SEQ_NUM=0;
    static String ACK = "sent";
    static String[] AVD_PORTS={REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
    static final int SERVER_PORT = 10000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        final EditText et = (EditText) findViewById(R.id.editText1);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final  String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        OnPTestClickListener optc= new OnPTestClickListener(tv, getContentResolver());
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket,optc);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */


        findViewById(R.id.button1).setOnClickListener(
                optc);

         /*
          * The onClick method of OnClickListener interface is overridden to get the message from the
          * input box (EditText) and send it to other AVDs upon clicking the send button.
          */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg=et.getText().toString();
                et.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
 }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;


    }



    private class ServerTask extends AsyncTask<Object, String, Void> {
        @Override
        protected Void doInBackground(Object... sockets) {
            ServerSocket serverSocket =(ServerSocket)sockets[0];
            OnPTestClickListener optc=(OnPTestClickListener)sockets[1];
            Socket client = null;
            try {
                while (true) {
                    String msg;

                    /* Establishes socket connection with the client */
                    client = serverSocket.accept();

                    /* Reads client message from the socket */
                    BufferedReader message = new BufferedReader(new InputStreamReader(client.getInputStream()));

                    /* To write acknowledgement message on the socket */
                    PrintWriter ACK_SERVER = new PrintWriter(client.getOutputStream(), true);

                    if (( msg = message.readLine()) != null) {

                        /* Stores the sequence number and message as (key,value) */
                        ContentValues keyValueToInsert = new ContentValues();
                        keyValueToInsert.put("key", SEQ_NUM++);
                        keyValueToInsert.put("value", msg);
                        getContentResolver().insert(optc.mUri, keyValueToInsert);

                        /* Updates the UI */
                        publishProgress(msg);

                        /* Server writes the acknowledgement message on the socket */
                        ACK_SERVER.println(ACK);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            /* Closing the socket */
            finally {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }


        /* Updates the view of each avd by appending the message to its textView */
        protected void onProgressUpdate(String...strings) {
            String receivedMessage = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(receivedMessage+"\n");
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String msgToSend = msgs[0];
                /* Creates 5 different sockets on the client side wit their respective port number*/
                for(int i=0;i<AVD_PORTS.length;i++){
                    Socket socket = new Socket(InetAddress.getByAddress
                            (new byte[]{10, 0, 2, 2}),Integer.parseInt(AVD_PORTS[i]));

                    /* Writes the message from the client on the socket*/
                    PrintWriter CLIENT_MSG = new PrintWriter(socket.getOutputStream(), true);
                    CLIENT_MSG.println(msgToSend);



                    /*Once the client receives the acknowledgement from the server,the socket will be closed*/
                    if(!socket.isClosed()) {

                        /*Reads for the acknowledgement message from the server*/
                        BufferedReader ACK_CLIENT = new BufferedReader
                                                    (new InputStreamReader(socket.getInputStream()));
                        String ack_server_msg = ACK_CLIENT.readLine();
                        if (ack_server_msg.equals(ACK)) {
                            socket.close();
                        }
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }
}
