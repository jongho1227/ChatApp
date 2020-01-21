package com.onvit.chatapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAuth firebaseAuth;
    private ValueEventListener valueEventListener;
    private String text = null;
    private Uri uri = null;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        firebaseAuth = FirebaseAuth.getInstance();
//        firebaseAuth.signOut();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0) // 한시간에 최대 한번 요청할 수 있음. 한시간의 캐싱타임을 가짐.
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);


        //다 메인으로 옮겨서 실행.

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
                        versionCheck();
                    }
                });


    }

    void versionCheck() {
        long versionCode = mFirebaseRemoteConfig.getLong("version_code");
        String updateMessage = mFirebaseRemoteConfig.getString("update_message");
        PackageInfo p = null;
        try {
            p = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        long version = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            version = p.getLongVersionCode();
        } else {
            version = p.versionCode;
        }
        Log.d("버전코드", version + "");
        if (versionCode != version) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(updateMessage).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.onvit.chatapp")));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.onvit.chatapp")));
                    } finally {
                        finish();
                    }
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        } else {
            createNotificationChannel();
            initSplash();

        }
    }

    private void createNotificationChannel() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(getString(R.string.vibrate),
                    "진동",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setVibrationPattern(new long[]{0, 500}); // 진동없애는거? 삭제하고 다시 깔아야 적용.
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(getString(R.string.noVibrate),
                    "무음",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setVibrationPattern(new long[]{0}); // 진동없애는거? 삭제하고 다시 깔아야 적용.
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initSplash() {

        if (PreferenceManager.getString(SplashActivity.this, "name") == null || PreferenceManager.getString(SplashActivity.this, "name").equals("")
                || FirebaseAuth.getInstance().getCurrentUser() == null) {
            firebaseAuth.signOut();
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            valueEventListener = new ValueEventListener() { // Users데이터의 변화가 일어날때마다 콜백으로 호출됨.
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // 가입한 유저들의 정보를 가지고옴.
                    User user = null;
                    String key = null;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(snapshot.getValue(User.class).getUid())) {
                            user = snapshot.getValue(User.class);
                            key = snapshot.getKey();
                        }
                        if (snapshot.getValue(User.class).getUserName() == null) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("Users/" + snapshot.getKey(), null);
                            map.put("groupChat/normalChat/userInfo/" + snapshot.getKey(), null);
                            map.put("groupChat/officerChat/userInfo/" + snapshot.getKey(), null);
                            map.put("groupChat/normalChat/users/" + snapshot.getKey(), null);
                            map.put("groupChat/officerChat/users/" + snapshot.getKey(), null);
                            map.put("lastChat/normalChat/existUsers/" + snapshot.getKey(), null);
                            map.put("lastChat/officerChat/existUsers/" + snapshot.getKey(), null);
                            map.put("lastChat/normalChat/users/" + snapshot.getKey(), null);
                            map.put("lastChat/officerChat/users/" + snapshot.getKey(), null);
                            FirebaseDatabase.getInstance().getReference().updateChildren(map);
                        }

                    }

                    if (user == null) {
                        firebaseAuth.signOut();
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = getIntent();
                        String action = intent.getAction();
                        String type = intent.getType();
                        if (Intent.ACTION_SEND.equals(action) && type != null) {
                            if ("text/plain".equals(type)) {
                                text = intent.getStringExtra(Intent.EXTRA_TEXT);
                                Intent intent1 = new Intent(SplashActivity.this, MainActivity.class);
                                intent1.putExtra("text", text);
                                intent1.putExtra("user", user);
                                intent1.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent1);
                                finish();
                            } else if (type.startsWith("image/")) {
                                Intent intent1 = new Intent(SplashActivity.this, MainActivity.class);
                                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                                final Uri convertUri = getConvertUri(uri);
                                Log.d("이미지 공유", uri + "");
                                Log.d("이미지 공유", convertUri + "");
                                if (convertUri != null) {
                                    intent1.putExtra("shareUri", convertUri);
                                    intent1.putExtra("filePath", filePath);
                                }
                                intent1.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent1.putExtra("user", user);
                                startActivity(intent1);
                                finish();
                            }
                        } else if (getIntent().getStringExtra("tag") != null) {
                            Intent intent2 = new Intent(SplashActivity.this, MainActivity.class);
                            intent2.putExtra("tag", getIntent().getStringExtra("tag"));
                            intent2.putExtra("user", user);
                            intent2.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent2);
                            finish();

                        } else {
                            Intent intent2 = new Intent(SplashActivity.this, MainActivity.class);
                            intent2.putExtra("user", user);
                            intent2.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent2);
                            finish();
                        }


                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            FirebaseDatabase.getInstance().getReference().child("Users").addValueEventListener(valueEventListener);
        }
    }

    private Uri getConvertUri(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            File file = getCacheDir();
            String fileName = System.currentTimeMillis() + ".jpeg";
            File tempFile = new File(file, fileName);
            OutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            getFilePath(tempFile.getAbsolutePath());
            return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", tempFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getFilePath(String absolutePath) {
        filePath = absolutePath;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            FirebaseDatabase.getInstance().getReference().child("Users").removeEventListener(valueEventListener);
        }
    }
}
