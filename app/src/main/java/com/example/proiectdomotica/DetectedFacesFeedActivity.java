package com.example.proiectdomotica;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetectedFacesFeedActivity extends AppCompatActivity implements RecyclerViewAdapter.ItemClickListener {

    private ConnectionFactory factory = new ConnectionFactory();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Channel subscribeChannel;
    private String detectedFacesDynamicQueueName;

    private RecyclerViewAdapter recyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detected_faces);
        setupConnectionFactory();

        DeliverCallback deliverCallback = new DeliverCallback() {
            @Override
            public void handle(String consumerTag, Delivery delivery) throws IOException {
                String message = new String(delivery.getBody(), "UTF-8");

                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    //deprecated in API 26
                    v.vibrate(500);
                }

                changeMainImage(message);

                saveImageToStorage(message);

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

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.imageList);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), LinearLayoutManager.HORIZONTAL);

        recyclerView.addItemDecoration(dividerItemDecoration);

        SnapHelper helper = new LinearSnapHelper();
        helper.attachToRecyclerView(recyclerView);

        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(DetectedFacesFeedActivity.this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(horizontalLayoutManager);
        recyclerViewAdapter = new RecyclerViewAdapter(this);
        recyclerViewAdapter.setClickListener(this);
        recyclerView.setAdapter(recyclerViewAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP | ItemTouchHelper.DOWN) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
                recyclerViewAdapter.removeImage(viewHolder.getAdapterPosition());
                Toast.makeText(getApplicationContext(), "on Swipe", Toast.LENGTH_SHORT).show();
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onItemClick(View view, int position) {
        //Toast.makeText(this, "You clicked " + recyclerViewAdapter.getItem(position) + " on item position " + position, Toast.LENGTH_SHORT).show();
        ImageView img = (ImageView) findViewById(R.id.mainImage);
        Picasso.with(getApplicationContext()).load(recyclerViewAdapter.getItem(position)).into(img);
    }

    private void changeMainImage(String encodedImage){

        ImageView img = (ImageView) findViewById(R.id.mainImage);
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        Drawable d = new BitmapDrawable(getResources(), decodedByte);
        this.runOnUiThread(new Runnable() {
            public void run() {
                img.setImageDrawable(d);
            }
        });
    }

    private void saveImageToStorage(String encodedImage){
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_APPEND);

        File mypath = new File(directory, new Date().toString());

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            decodedByte.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.runOnUiThread(() -> recyclerViewAdapter.addImage(mypath));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        executorService.submit(() -> {
            if(subscribeChannel != null){
                try {
                    subscribeChannel.queueDelete(detectedFacesDynamicQueueName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        executorService.shutdown();
    }

    private void subscribe(DeliverCallback deliverCallback, CancelCallback cancelCallback) {

        executorService.submit(() -> {

                try {

                    Connection subscribeConnection = null;

                    subscribeChannel = null;

                    detectedFacesDynamicQueueName = "";

                    while(true){

                        if((subscribeConnection == null || !subscribeConnection.isOpen()) || (subscribeChannel == null || !subscribeChannel.isOpen())){
                            Log.i("MESAJ", "CONEXIUNEA E INCHISA!, refac conexiunea!");
                            subscribeConnection = factory.newConnection();
                            subscribeChannel = subscribeConnection.createChannel();
                            AMQP.Queue.DeclareOk dc = subscribeChannel.queueDeclare("", true, false, true, null);
                            detectedFacesDynamicQueueName = dc.getQueue();

                            subscribeChannel.queueBind(detectedFacesDynamicQueueName, "poze", "");
                        }


                        subscribeChannel.basicConsume(detectedFacesDynamicQueueName, true, deliverCallback, cancelCallback);
                        Log.i("MESAJ","Astept 1 secunda...");
                        Thread.sleep(1000);
                    }

                } catch (Exception e1) {
                    Log.d("MESAJ", "Exception: " + e1.toString());
                    e1.printStackTrace();
                }
        });
    }

    private void setupConnectionFactory() {
        try {

            factory.setAutomaticRecoveryEnabled(false);
            factory.setUsername("admin");
            factory.setPassword("admin");
            factory.setVirtualHost("/");
            factory.setHost("192.168.100.29");
            factory.setPort(5672);
            factory.setConnectionTimeout(5000);

        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }



}