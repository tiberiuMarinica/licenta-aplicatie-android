package com.example.proiectdomotica;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DetectedFacesFeedActivity extends AppCompatActivity {

    private Thread subscribeThread;
    private ConnectionFactory factory = new ConnectionFactory();
    private static final String QUEUE_NAME = "poze";
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detected_faces);
        Log.i("Mesag","sdfaasfdasdfasdf");
        setupConnectionFactory();

        DeliverCallback deliverCallback = new DeliverCallback() {
            @Override
            public void handle(String consumerTag, Delivery delivery) throws IOException {
                String message = new String(delivery.getBody(), "UTF-8");
                Log.i("MESAJ"," [x] Received '" + message + "'");

                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(500);
                }

                changeImage(message);
            }
        };

        CancelCallback cancelCallback = new CancelCallback() {
            @Override
            public void handle(String consumerTag) throws IOException {
                Log.i("MESAJ"," [x] Received '" + consumerTag + "'");
            }
        };

        try {
            subscribe(deliverCallback, cancelCallback);
        }catch(Exception e){
            Log.e("MESAJ", e.toString());
            e.printStackTrace();
        }

    }

    private void changeImage(String encodedImage){

        ImageView img = (ImageView) findViewById(R.id.imageView);
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        Drawable d = new BitmapDrawable(getResources(), decodedByte);
        this.runOnUiThread(new Runnable() {
            public void run() {
                img.setImageDrawable(d);
            }
        });

    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        subscribeThread.interrupt();
    }

    void subscribe(DeliverCallback deliverCallback, CancelCallback cancelCallback) {
        if(subscribeThread != null){
            Log.e("MESAJ", "AVEM DEJA SUBSCRIBE THREAD< NU MAI FACEM ALTUL");
            return;
        }
        subscribeThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    //Connection connection = factory.newConnection();
                    Connection connection = null;

                    //Channel channel = connection.createChannel();
                    Channel channel = null;

                    String queueName = "";

                    while(true){

                        if((connection == null || !connection.isOpen()) || (channel == null || !channel.isOpen())){
                            Log.i("MESAJ", "CONEXIUNEA E INCHISA!, refac conexiunea!");
                            connection = factory.newConnection();
                            channel = connection.createChannel();
                            AMQP.Queue.DeclareOk dc = channel.queueDeclare("", true, false, true, null);
                            queueName = dc.getQueue();

                            channel.queueBind(queueName, "poze", "");
                        }


                        channel.basicConsume(queueName, true, deliverCallback, cancelCallback);
                        Log.i("MESAJ","Astept 1 secunda...");
                        Thread.sleep(1000);
                    }

                } catch (Exception e1) {
                    Log.d("MESAJ", "Exception: " + e1.toString());
                    e1.printStackTrace();
                }
            }
        });

        subscribeThread.start();
    }

    private void setupConnectionFactory() {
        try {
            System.out.println("here");
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUsername("dkpszkss");
            factory.setPassword("jyZnGWIwdRy-H44cF8EqBX94g3MzJHmJ");
            factory.setVirtualHost("dkpszkss");
            factory.setHost("stingray.rmq.cloudamqp.com");
            factory.setPort(5672);
            factory.setConnectionTimeout(5000);

        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }
}