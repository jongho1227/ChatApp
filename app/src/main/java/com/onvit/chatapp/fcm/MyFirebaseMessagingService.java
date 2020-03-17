package com.onvit.chatapp.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
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
        if (remoteMessage.getData().size() > 0) {

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
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }
    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send token to your app server.
    }
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    private void sendNotification(final RemoteMessage remoteMessage) {
        final int[] id = new int[1];
        final String tag = remoteMessage.getData().get("tag");
        if(tag.equals("normalChat")){
            id[0] =0;
            sendFcm(remoteMessage, id[0], tag);
        }else if(tag.equals("officerChat")){
            id[0] =1;
            sendFcm(remoteMessage, id[0], tag);
        }else if(tag.equals("notice")){
            id[0] =2;
            sendFcm(remoteMessage, id[0], tag);
        }else{
            FirebaseDatabase.getInstance().getReference().child("groupChat").child(tag).child("id").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Long id1 = dataSnapshot.getValue(Long.class);
                    long id2 = id1;
                    id[0] = (int) id2;
                    sendFcm(remoteMessage, id[0], tag);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    private void sendFcm(RemoteMessage remoteMessage, int id, String tag) {
        Intent intent = new Intent(this, SplashActivity.class);//알림 누르면 열리는 창
        String channelId;
        int v = getSharedPreferences(getPackageName(), MODE_PRIVATE).getInt("vibrate", 0);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(v==0){
            channelId = getString(R.string.vibrate2);
            //오레오버전 이하 진동 및 무음설정하는거.
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            }
        }else{
            channelId = getString(R.string.noVibrate);
            //오레오버전 이하 진동 및 무음설정하는거.
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        }


        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("tag", tag);
//        개별적인 작업을 하기 위해서는 pendingIntent를 생성할때마다 requestCode를 다르게 할당하고
//        서로의 충돌을 피하기 위해서 flags는 FLAG_CANCEL_CURRENT 로 호출해야 한다.
        PendingIntent pendingIntent = PendingIntent.getActivity(this, id /* Request code */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT); // 원격으로 인텐트 실행하는거 앱이 꺼져있어도 실행하는거

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_kcha)
                .setContentTitle(remoteMessage.getData().get("title"))
                .setContentText(remoteMessage.getData().get("body"))
                .setAutoCancel(true) // 누르면 알림 없어짐
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        //오레오버전 이하 진동 및 무음설정하는거.
        if(channelId.equals(getString(R.string.vibrate2))){
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_VIBRATE);
        }
        if(channelId.equals(getString(R.string.noVibrate))){
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_LOW);
        }


        notificationManager.notify(id /* ID of notification */, notificationBuilder.build());
    }
}
