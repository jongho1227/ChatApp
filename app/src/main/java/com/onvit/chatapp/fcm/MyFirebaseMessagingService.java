package com.onvit.chatapp.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.onvit.chatapp.R;
import com.onvit.chatapp.SplashActivity;
import com.onvit.chatapp.model.LastChat;

import java.util.List;

import me.leolin.shortcutbadger.ShortcutBadger;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) { // 알림 받는부분, 포그라운드꺼만 받음.
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
//            if (/* Check if data needs to be processed by long running job */ true) {
//                // For long-running tasks (10 seconds or more) use WorkManager.
//                scheduleJob();
//            } else {
//                // Handle message within 10 seconds
//                handleNow();
//            }

        }
        if (remoteMessage.getNotification() != null) {

        }
        sendNotification(remoteMessage);
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference().child("lastChat").orderByChild("existUsers/" + uid).equalTo(true).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int count = 0;
                for(DataSnapshot item : dataSnapshot.getChildren()){
                    LastChat chatList = item.getValue(LastChat.class);
                    count +=  chatList.getUsers().get(uid);
                }
                ShortcutBadger.applyCount(getApplicationContext(),count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    @Override
    public void onNewToken(String token) {
        sendRegistrationToServer(token);
    }
    private void sendRegistrationToServer(String token) {
    }
    private void handleNow() {
    }

    private void sendNotification(RemoteMessage remoteMessage) {
        Intent intent = new Intent(this, SplashActivity.class);//알림 누르면 열리는 창
        String tag = remoteMessage.getData().get("tag");
        String channelId;
        int id = 1;
        if(tag.equals("normalChat")){
            id=0;
        }else if(tag.equals("officerChat")){
            id=1;
        }else if(tag.equals("notice")){
            id=2;
        }
        int v = getSharedPreferences(getPackageName(), MODE_PRIVATE).getInt("vibrate", 0);
        if(v==0){
            channelId = getString(R.string.vibrate);
        }else{
            channelId = getString(R.string.noVibrate);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("tag", tag);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT); // 원격으로 인텐트 실행하는거 앱이 꺼져있어도 실행하는거

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_kcha)
                .setContentTitle(remoteMessage.getData().get("title"))
                .setContentText(remoteMessage.getData().get("body"))
                .setAutoCancel(true) // 누르면 알림 없어짐
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        if(v==0){
            notificationBuilder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND).setVibrate(new long[]{0, 500});
        }
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(id /* ID of notification */, notificationBuilder.build());
    }
}
