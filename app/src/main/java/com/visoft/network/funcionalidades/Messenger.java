package com.visoft.network.funcionalidades;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.visoft.network.R;
import com.visoft.network.objects.ChatOverview;
import com.visoft.network.objects.Message;
import com.visoft.network.objects.MessageContractFinished;
import com.visoft.network.objects.MessageMap;
import com.visoft.network.objects.MessageText;
import com.visoft.network.util.Constants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class Messenger {
    private DatabaseReference database;
    private Context context;
    private String fromUID, toUID;
    private ViewGroup rootView;
    private View loseFocusView;
    private boolean notify;

    public Messenger(Context context, String from, String to, ViewGroup rootView, View loseFocusView, DatabaseReference database) {
        this.context = context;
        this.fromUID = from;
        this.toUID = to;
        this.rootView = rootView;
        this.database = database;
        this.loseFocusView = loseFocusView;
        this.notify = true;

        setUp();
    }

    private void setUp() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.chat_layout, rootView);

        final TextView etChat = view.findViewById(R.id.etChat);
        final FloatingActionButton fab = view.findViewById(R.id.btnSend);
        final ImageView sendLocation = view.findViewById(R.id.btnSendLocation);

        etChat.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                Objects.requireNonNull(imm).hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });

        loseFocusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) v.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                etChat.clearFocus();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etChat.getText().toString();
                if (text.length() > 0) {
                    MessageText message = new MessageText();
                    message.setTimeStamp(new Date().getTime()).setAuthor(fromUID);
                    message.setMessage(text);
                    sendMessage(message);
                }
                etChat.setText("");
            }
        });

        sendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMapDialog();
            }
        });
    }

    public void finishChat() {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
        Date date = new Date();
        sendMessage(new MessageContractFinished(context.getString(R.string.finished_at) + " " + dateFormat.format(date)).setAuthor(fromUID));
    }

    private void sendMessage(final Message msg) {
        database
                .child(Constants.FIREBASE_CHATS_CONTAINER_NAME)
                .child(fromUID)
                .child(toUID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        ChatOverview chatOverview = dataSnapshot.getValue(ChatOverview.class);

                        if (chatOverview == null) {
                            String chatID = database
                                    .child(Constants.FIREBASE_MESSAGES_CONTAINER_NAME)
                                    .push().getKey();
                            chatOverview = new ChatOverview();
                            chatOverview.setChatID(chatID);
                        }

                        chatOverview.setFinished(msg instanceof MessageContractFinished);
                        chatOverview.setAuthor(fromUID);
                        chatOverview.setReceiverUID(toUID);
                        chatOverview.setLastMessage(msg.getOverview());
                        chatOverview.setTimeStamp(new Date().getTime());
                        saveInChats(chatOverview, msg);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void saveInChats(ChatOverview chatOverview, Message msg) {
        database
                .child(Constants.FIREBASE_CHATS_CONTAINER_NAME)
                .child(chatOverview.getAuthor())
                .child(chatOverview.getReceiver())
                .setValue(chatOverview);

        database
                .child(Constants.FIREBASE_CHATS_CONTAINER_NAME)
                .child(chatOverview.getReceiver())
                .child(chatOverview.getAuthor())
                .setValue(chatOverview);

        Gson gson = GsonerMessages.getGson();
        String data = gson.toJson(msg, Message.class);

        database
                .child(Constants.FIREBASE_MESSAGES_CONTAINER_NAME)
                .child(chatOverview.getChatID())
                .push()
                .setValue(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (notify) {
                    Toast.makeText(context, context.getString(R.string.mensaje_enviado), Toast.LENGTH_SHORT).show();
                }
            }
        });

        database
                .child(Constants.FIREBASE_NOTIFICATIONS_CONTAINER_NAME)
                .child(Constants.FIREBASE_MESSAGES_CONTAINER_NAME)
                .push()
                .setValue(chatOverview);
    }

    private void showMapDialog() {
        final Marker[] marker = {null};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendLocationAsMessage(marker[0].getPosition());
            }
        });

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialogmap, null);
        builder.setView(view);
        Dialog dialog = builder.create();
        dialog.show();

        final GoogleMap[] googleMap = new GoogleMap[1];
        MapView mMapView = dialog.findViewById(R.id.mapView);
        MapsInitializer.initialize(context);

        final MarkerOptions markerOptions = new MarkerOptions();

        mMapView.onCreate(dialog.onSaveInstanceState());
        mMapView.onResume();
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap g) {
                googleMap[0] = g;

                g.getUiSettings().setZoomControlsEnabled(true);
                g.getUiSettings().setCompassEnabled(true);

                markerOptions.position(googleMap[0].getCameraPosition().target);
                marker[0] = googleMap[0].addMarker(markerOptions);

                googleMap[0].setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override
                    public void onCameraMove() {
                        marker[0].setPosition(googleMap[0].getCameraPosition().target);
                    }
                });
            }
        });


    }

    private void sendLocationAsMessage(LatLng position) {
        MessageMap message = new MessageMap();
        message.setTimeStamp(new Date().getTime())
                .setAuthor(fromUID);
        message.setPosition(position);
        sendMessage(message);
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }
}