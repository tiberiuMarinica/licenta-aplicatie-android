package com.example.proiectdomotica;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class LiveStreamActivity extends AppCompatActivity {

    public static final String EXCHANGE_COMENZI = "comenzi";
    private Thread liveStreamReceiveThread;
    private Thread readFromBufferThread;
    private ConnectionFactory factory = new ConnectionFactory();
    private BlockingQueue<String> buffer = new LinkedBlockingDeque<>();
    private Boolean startedReadingFromBuffer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_stream);

        subscribe();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        liveStreamReceiveThread.interrupt();

    }

    void subscribe() {
        if(liveStreamReceiveThread != null){
            Log.e("MESAJ", "AVEM DEJA SUBSCRIBE THREAD< NU MAI FACEM ALTUL");
            return;
        }
        liveStreamReceiveThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    // establish the connection with server port 5056
                    Socket s = new Socket("192.168.100.10", 8001);

                    // obtaining input and out streams
                    DataInputStream dis = new DataInputStream(s.getInputStream());

                    // the following loop performs the exchange of
                    // information between client and client handler
                    while (true)
                    {
                        // printing date or time as requested by client
                        String receivedEncodedImage = dis.readUTF();
                        sendImageToScreen(receivedEncodedImage);
                    }

                } catch (Exception e1) {
                    Log.d("MESAJ", "Exception: " + e1.toString());
                    e1.printStackTrace();
                }
            }
        });

        liveStreamReceiveThread.start();
    }

    private void sendImageToScreen(String encodedImage){

        ImageView img = (ImageView) findViewById(R.id.imageView);
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        Drawable d = new BitmapDrawable(getResources(), decodedByte);
        LiveStreamActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                img.setImageDrawable(d);
            }
        });


    }


}