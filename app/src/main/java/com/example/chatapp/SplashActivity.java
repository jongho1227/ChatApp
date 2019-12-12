package com.example.chatapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.model.ChatModel;
import com.example.chatapp.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {

    private LinearLayout linearLayout;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAuth firebaseAuth;
    private ValueEventListener valueEventListener;
    private List<User> userList;
    private String text = null;
    private Uri uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.parseColor("#050099"));
        linearLayout = findViewById(R.id.splashactivity_linearlayout);
        firebaseAuth = FirebaseAuth.getInstance();
//        firebaseAuth.signOut();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.default_config);


        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d("원격", "Config params updated: " + updated);


                        } else {
                            Toast.makeText(SplashActivity.this, "Fetch failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                        displayMessage();
                    }
                });
        if (PreferenceManager.getString(this, "name") == null || PreferenceManager.getString(this, "name").equals("")) {
            firebaseAuth.signOut();
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            userList = new ArrayList<>();
            final Date date = new Date();
            long twoM = (24L * 60 * 60 * 1000 * 60);
            long oldDate = date.getTime()-twoM;
            //맨 처음채팅부터 200개씩 가져와서 두달지난거 삭제함.
            String chatName = "normalChat";
            deleteChat(oldDate,chatName);
            chatName = "officerChat";
            deleteChat(oldDate,chatName);

            //한달 지난 공지사항 삭제함.
//            long oneM = (24L * 60 * 60 * 1000)*30;
//            long noticeDate = date.getTime()-oneM;
//            deleteNotice(noticeDate);



            valueEventListener = new ValueEventListener() { // Users데이터의 변화가 일어날때마다 콜백으로 호출됨.
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // 가입한 유저들의 정보를 가지고옴.

                    userList.clear();
                    User user = null;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(snapshot.getValue(User.class).getUid())) {
                            user = snapshot.getValue(User.class);
                            continue;
                        }

                    }

                    if (user == null) {
                        firebaseAuth.signOut();
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
//                    if(getIntent().getStringExtra("fcm")!=null){
//                        Log.d("들어옴?", "");
//                        Intent intent2 = new Intent(SplashActivity.this, MainActivity.class);
//                        intent2.putExtra("fcm", "fcm");
//                        intent2.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent2);
//                        finish();
//                    }

                    Intent intent = getIntent();
                    String action = intent.getAction();
                    String type = intent.getType();
                    if (Intent.ACTION_SEND.equals(action) && type != null) {
                        if ("text/plain".equals(type)) {
                            text = intent.getStringExtra(Intent.EXTRA_TEXT);
                            Intent intent1 = new Intent(SplashActivity.this, MainActivity.class);
                            intent1.putExtra("text", text);
//                            intent1.putParcelableArrayListExtra("userList", (ArrayList<? extends Parcelable>) userList);
                            intent1.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent1);
                            finish();

                            Log.d("공유", text);
                        } else if (type.startsWith("image/")) {
                            Intent intent1 = new Intent(SplashActivity.this, MainActivity.class);
                            uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                            final Uri convertUri = getConvertUri(uri);
                            if (convertUri != null) {
                                intent1.putExtra("shareUri", convertUri);
                            }
//                            intent1.putParcelableArrayListExtra("userList", (ArrayList<? extends Parcelable>) userList);
                            intent1.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent1);
                            finish();
                            Log.d("공유", uri.toString());
                        }
                    } else {
                        Intent intent2 = new Intent(SplashActivity.this, MainActivity.class);
//                        intent2.putParcelableArrayListExtra("userList", (ArrayList<? extends Parcelable>) userList);
                        intent2.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent2);
                        finish();
                    }


                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            FirebaseDatabase.getInstance().getReference().child("Users").addValueEventListener(valueEventListener);
        }
    }

    private void deleteNotice(final long noticeDate) {
        FirebaseDatabase.getInstance().getReference().child("Notice")
                .orderByChild("timestamp")
                .endAt(noticeDate).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> map = new HashMap<>();
                Log.d("삭제갯수", dataSnapshot.getChildrenCount()+"");
                Log.d("삭제뺀거", noticeDate+"");
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    map.put(item.getKey(), null);
                    Log.d("삭제키", item.getKey());
                }
                FirebaseDatabase.getInstance().getReference().child("Notice").updateChildren(map);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void deleteChat(final long date, final String chatName) {
        FirebaseDatabase.getInstance().getReference().child("groupChat").child(chatName).child("comments")
                .orderByChild("timestamp")
                .endAt(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> map = new HashMap<>();
                Log.d("삭제", dataSnapshot.getChildrenCount()+"");
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                        map.put(item.getKey(), null);
                }
                FirebaseDatabase.getInstance().getReference().child("groupChat").child(chatName).child("comments").updateChildren(map);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    void displayMessage() {
        boolean caps = mFirebaseRemoteConfig.getBoolean("splash_message_caps"); // 이거 true로 보내면 서버점검중이라고 띄울 수 있음.
        String splashMessage = mFirebaseRemoteConfig.getString("splash_message");
        if (caps) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(splashMessage).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            builder.create().show();
        } else {
//            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
//            startActivity(intent);
//            finish();

        }
    }

    private Uri getConvertUri(Uri uri) {
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/KCHA", System.currentTimeMillis() + ".jpeg");
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                OutputStream outputStream = new FileOutputStream(file);
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                inputStream.close();
            }
            if (file.length() > 0) {
                return new Uri.Builder().scheme("file").path(file.getAbsolutePath()).build();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            FirebaseDatabase.getInstance().getReference().child("Users").removeEventListener(valueEventListener);
        }
    }


}
