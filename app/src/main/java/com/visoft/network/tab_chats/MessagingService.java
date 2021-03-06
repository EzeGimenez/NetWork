package com.visoft.network.tab_chats;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.visoft.network.MainActivityNormal;
import com.visoft.network.R;
import com.visoft.network.funcionalidades.GsonerUser;
import com.visoft.network.objects.User;
import com.visoft.network.util.Constants;

import static com.visoft.network.MainActivityNormalFragment.RECEIVER_INTENT;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(final String s) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DatabaseReference ds = FirebaseDatabase.getInstance().getReference();
            ds.child(Constants.FIREBASE_USERS_PRO_CONTAINER_NAME).child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    User user = GsonerUser.getGson().fromJson(dataSnapshot.getValue(String.class), User.class);
                    user.setInstanceID(s);

                    dataSnapshot.getRef().setValue(GsonerUser.getGson().toJson(user, User.class));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREF_NAME, MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("unreadMessages", true).commit();

        if (!MainActivityNormal.isRunning && !SpecificChatActivity.isRunning) {
            createNotification(remoteMessage.getData().get("sender"), remoteMessage.getData().get("body"), remoteMessage.getData().get("title"));
        } else {
            Intent intent = new Intent(RECEIVER_INTENT);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private void createNotification(String sender, String body, String title) {
        NotificationManager notifManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final int NOTIFY_ID = 1002;

        String name = Constants.NOTIFICATION_CHAT_CHANNEL_NAME;
        String id = Constants.NOTIFICATION_CHAT_CHANNEL_ID;
        String description = "New Message";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, id);

        Intent intent = new Intent(this, MainActivityNormal.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        RemoteViews notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.notification);

        notificationLayoutExpanded.setTextViewText(R.id.sender, title);
        notificationLayoutExpanded.setTextViewText(R.id.body, body);

        builder
                .setSmallIcon(R.drawable.arrow_back_2)
                .setCustomContentView(notificationLayoutExpanded)
                .setCustomBigContentView(notificationLayoutExpanded)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel mChannel = notifManager.getNotificationChannel(id);
            if (mChannel == null) {
                mChannel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
                mChannel.setDescription(description);
                mChannel.enableVibration(true);
                mChannel.setShowBadge(true);
                mChannel.setLightColor(Color.GREEN);
                notifManager.createNotificationChannel(mChannel);
            }

        } else {
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        Notification notification = builder.build();
        notifManager.notify(NOTIFY_ID, notification);
    }
}
