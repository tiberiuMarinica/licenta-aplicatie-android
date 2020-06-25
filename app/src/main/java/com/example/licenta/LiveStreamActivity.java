package com.example.licenta;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ToggleButton;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class LiveStreamActivity extends AppCompatActivity {

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private ConnectionFactory factory = new ConnectionFactory();
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_stream);
        setupConnectionFactory();

        addClickListenerOnQualityRadioGroup();

        receiveStreamFromSocketAndSendToScreen();

        ToggleButton toggleDoorLockButton = (ToggleButton) findViewById(R.id.doorLockToggle);
        toggleDoorLockButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // The toggle is enabled
                sendCommand("LOCK_DOOR");
            } else {
                // The toggle is disabled
                sendCommand("UNLOCK_DOOR");
            }
        });

        ToggleButton toggleAlarmButton = (ToggleButton) findViewById(R.id.alarmToggle);
        toggleAlarmButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // The toggle is enabled
                sendCommand("START_ALARM");
            } else {
                // The toggle is disabled
                sendCommand("STOP_ALARM");
            }
        });

    }

    private void addClickListenerOnQualityRadioGroup(){
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.qualityRadioGroup);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                RadioButton clickedRadioButton = (RadioButton) findViewById(checkedId);
                Log.i("test", String.valueOf(clickedRadioButton.getText()));

                String param = "";
                switch (String.valueOf(clickedRadioButton.getText())){
                    case "Mica":
                        param = "low";
                        break;
                    case "Medie":
                        param = "medium";
                        break;
                    case "Mare":
                        param = "high";
                        break;
                    default:
                        param = "medium";
                }

                sendCommand("CHANGE_LIVE_STREAM_QUALITY", param);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if(socket != null){
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        executorService.shutdownNow();
    }

    private void sendCommand(String command) {
        sendCommand(command, "");
    }

    private void sendCommand(String command, String parameter){
        JSONObject json = new JSONObject();
        try {
            json.put("name", command);
            json.put("parameter", parameter);
            executorService.submit(() -> {

                try {
                    Connection connection = factory.newConnection();
                    Channel channel = connection.createChannel();
                    channel.basicPublish("comenzi", "comenzi", null, json.toString().getBytes("UTF-8"));
                    channel.close();
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void receiveStreamFromSocketAndSendToScreen() {
        executorService.submit(() -> {

            socket = null;
            try {
                // establish the connection with server port 5056
                socket = new Socket("192.168.100.10", 8001);

                // obtaining input and out streams
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                // the following loop performs the exchange of
                // information between client and client handler
                while (true) {
                    // printing date or time as requested by client
                    try {
                        String receivedEncodedImage = dis.readUTF();
                        sendImageToScreen(receivedEncodedImage);
                    }catch(EOFException eofException){
                        Log.e("Error", eofException.getMessage(), eofException);
                    }


                }


            } catch (Exception e1) {
                Log.e("MESAJ", "Exception: " + e1.toString(), e1);
                e1.printStackTrace();
            }

        });
    }

    private void sendImageToScreen(String encodedImage){
        if(encodedImage.equals("")){
            return;
        }
        ImageView img = (ImageView) findViewById(R.id.mainImage);
        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        Drawable d = new BitmapDrawable(getResources(), decodedByte);
        LiveStreamActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                img.setImageDrawable(d);
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