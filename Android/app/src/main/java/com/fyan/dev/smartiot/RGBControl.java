package com.fyan.dev.smartiot;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.flag.BubbleFlag;
import com.skydoves.colorpickerview.flag.FlagMode;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.List;
import java.util.Map;

public class RGBControl extends AppCompatActivity {

    Button btn_on, btn_off;
    ImageView img_color;
    TextView status, koneksi;
    Animation fadeIn;
    int red, green, blue;
    WebSocket ws;
    String Server = "wss://codelab-api.herokuapp.com";
    final int timeout = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rgb_control);

        status = findViewById(R.id.status);
        koneksi = findViewById(R.id.koneksi);
        btn_on = findViewById(R.id.btn_on);
        btn_off = findViewById(R.id.btn_off);
        img_color = findViewById(R.id.img_color);
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        red = 255;
        green = 0;
        blue = 0;

        btn_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("ON");
            }
        });

        btn_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("OFF");
            }
        });

        img_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorPickerDialog.Builder builder = new ColorPickerDialog.Builder(
                        RGBControl.this, R.style.MyDialogTheme)
                        .setTitle("Select Color")
                        .setPreferenceName("color")
                        .setPositiveButton("SELECT",
                                new ColorEnvelopeListener() {
                                    @Override
                                    public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                        int[] color = envelope.getArgb(); //[1,255,0,0]
                                        red = color[1];
                                        green = color[2];
                                        blue = color[3];
                                        status.setShadowLayer(
                                                30,2,2, Color.rgb(red, green, blue));
                                        status.setTextColor(Color.parseColor("#ffffff"));
                                        String message = "Color:" + red + "," + green + "," + blue;
                                        sendMessage(message);

                                    }
                                })
                        .setNegativeButton("CANCEL",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                        .attachAlphaSlideBar(false)
                        .attachBrightnessSlideBar(true)
                        .setBottomSpace(12);
                ColorPickerView colorPickerView = builder.getColorPickerView();
                BubbleFlag bubbleFlag = new BubbleFlag(RGBControl.this);
                bubbleFlag.setFlagMode(FlagMode.ALWAYS);
                colorPickerView.setFlagView(bubbleFlag);
                builder.show();

            }
        });

        try {
            ws = connect();
        } catch (Exception e) {
            Log.e("TAG", "onCreate: " + e.toString());
            setKoneksi("ERROR: " + e.getMessage());
        }
    }

    private  WebSocket connect() throws Exception
    {
        return new WebSocketFactory()
                .setConnectionTimeout(timeout)
                .createSocket(Server)
                .addListener(new WebSocketAdapter(){
                    @Override
                    public void onConnected(WebSocket websocket, Map<String, List<String>> headers)
                            throws Exception {
                        super.onConnected(websocket, headers);
                        Log.d("WebSocket", "Connected to web socket");
                        setKoneksi("CONNECTED");
                        ws.sendText("STATUS");

                    }

                    @Override
                    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
                                               WebSocketFrame clientCloseFrame,
                                               boolean closedByServer) throws Exception {
                        super.onDisconnected(
                                websocket, serverCloseFrame, clientCloseFrame, closedByServer);
                        setKoneksi("DISCONNECTED");
                    }

                    @Override
                    public void onTextMessage(WebSocket websocket, String text) throws Exception {
                        super.onTextMessage(websocket, text);
                        if (text.equalsIgnoreCase("ON")) {
                            setStatusOn();
                        }
                        if(text.equalsIgnoreCase("OFF")){
                            setStatusOff();
                        }

                    }

                    @Override
                    public void onError(WebSocket websocket, WebSocketException cause)
                            throws Exception {
                        super.onError(websocket, cause);
                        setKoneksi("ERROR: " + cause.getMessage());
                    }
                })
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                .connectAsynchronously();
    }



    public void sendMessage(String message) {
        if (ws.isOpen()) {
            ws.sendText(message);
        }
        else {
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setMessage("Perangkat tidak terhubung ke server. Hubungkan lagi ?")
                    .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ws = null;
                            try {
                                ws = connect();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .setNegativeButton("Tidak", null)
                    .show();

        }
    }

    private void setStatusOn(){

        status.post(new Runnable() {
            @Override
            public void run() {
                status.setText("ON");
                status.startAnimation(fadeIn);
                status.setShadowLayer(30,2,2, Color.rgb(red, green, blue));
                status.setTextColor(Color.parseColor("#ffffff"));
                img_color.setVisibility(View.VISIBLE);
                btn_on.setBackgroundResource(R.drawable.button_on_active);
                btn_off.setBackgroundResource(R.drawable.button_off_disable);
            }
        });
    }

    private void setStatusOff(){
        status.post(new Runnable() {
            @Override
            public void run() {
                status.setText("OFF");
                status.getPaint().clearShadowLayer();
                status.invalidate();
                status.setTextColor(Color.parseColor("#4Dffffff"));
                img_color.setVisibility(View.INVISIBLE);
                btn_on.setBackgroundResource(R.drawable.button_on_disable);
                btn_off.setBackgroundResource(R.drawable.button_off_active);
            }
        });

    }

    private void setKoneksi(final String text){
        koneksi.post(new Runnable() {
            public void run() {
                koneksi.setText(text);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ws != null) {
            ws.sendClose(1000, "Closing from client");
            ws.disconnect();
            ws = null;
        }
    }
}