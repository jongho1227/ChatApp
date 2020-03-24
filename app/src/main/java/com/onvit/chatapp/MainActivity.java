package com.onvit.chatapp;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.onvit.chatapp.ad.ShoppingFragment;
import com.onvit.chatapp.admin.AdminActivity;
import com.onvit.chatapp.admin.InviteActivity;
import com.onvit.chatapp.admin.SetupFragment;
import com.onvit.chatapp.chat.ChatFragment;
import com.onvit.chatapp.chat.GroupMessageActivity;
import com.onvit.chatapp.chat.SelectGroupChatActivity;
import com.onvit.chatapp.contact.PeopleFragment;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.Img;
import com.onvit.chatapp.model.LastChat;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.UserMap;
import com.onvit.chatapp.notice.NoticeFragment;
import com.onvit.chatapp.util.PreferenceManager;
import com.onvit.chatapp.util.Utiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private final static int PERMISSION_REQUEST_CODE = 1000;
    BottomNavigationMenuView bottomNavigationMenuView;
    BottomNavigationView bottomNavigationView;
    private FirebaseAuth firebaseAuth;
    private String text = null;
    private Uri uri = null;
    private User user;
    private String uid;
    private ValueEventListener valueEventListener;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private Map<String, User> userMap;
    private List<User> userList;
    private List<ChatModel.Comment> newComments = new ArrayList<>();
    private List<Img> img_list = new ArrayList<>();
    private final int firstReadChatCount = Utiles.firstReadChatCount;
    private AlertDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebaseAuth = FirebaseAuth.getInstance();
        //유저없으면 로그인 페이지로
        if (firebaseAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            firebaseAuth.signOut();
            startActivity(intent);
            finish();
        }

        userMap = UserMap.getInstance();
        UserMap.getUserMap();
        userList = UserMap.getUser();
        UserMap.getUserList();

        user = getIntent().getParcelableExtra("user");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        PreferenceManager.setString(MainActivity.this, "name", user.getUserName());
        PreferenceManager.setString(MainActivity.this, "hospital", user.getHospital());
        PreferenceManager.setString(MainActivity.this, "phone", user.getTel());
        PreferenceManager.setString(MainActivity.this, "uid", user.getUid());

        final Fragment notice = new NoticeFragment();
        final Fragment people = new PeopleFragment();
        final Fragment shop = new ShoppingFragment();

        getIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        bottomNavigationView = findViewById(R.id.mainActivity_bottomNavigationView);
        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, notice).commitAllowingStateLoss();

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_notice:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, notice).commitAllowingStateLoss();
                        return true;
                    case R.id.action_people:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, people).commitAllowingStateLoss();
                        return true;
                    case R.id.action_chat:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, new ChatFragment()).commitAllowingStateLoss();
                        bottomNavigationMenuView.getChildAt(2).setEnabled(false);
                        return true;
                    case R.id.action_account:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, shop).commitAllowingStateLoss();
                        return true;
                    case R.id.action_setup:
                        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, new SetupFragment(user)).commitAllowingStateLoss();
                        bottomNavigationMenuView.getChildAt(4).setEnabled(false);
                        return true;

                }
                return false;
            }
        });
        if (getIntent().getStringExtra("text") != null || getIntent().getParcelableExtra("shareUri") != null) {
            text = getIntent().getStringExtra("text");
            uri = getIntent().getParcelableExtra("shareUri");
            String filePath = getIntent().getStringExtra("filePath");
            Intent intent1 = new Intent(MainActivity.this, SelectGroupChatActivity.class);
            intent1.putExtra("text", text);
            intent1.putExtra("shareUri", uri);
            intent1.putExtra("filePath", filePath);
            startActivity(intent1);
        }
        bottomNavigationMenuView = (BottomNavigationMenuView) bottomNavigationView.getChildAt(0);
        requestPermission();
        passPushTokenToServer();

    }

    private void requestPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        ArrayList<String> arrayPermission = new ArrayList<>();

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            arrayPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            arrayPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (arrayPermission.size() > 0) {
            String[] strArray = new String[arrayPermission.size()];
            strArray = arrayPermission.toArray(strArray);
            ActivityCompat.requestPermissions(this, strArray, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length < 1) {
                    Utiles.customToast(this, "권한을 받아오는데 실패하였습니다.").show();
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                    return;
                }
                for (int i = 0; i < grantResults.length; i++) {
                    String permission = permissions[i];
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
                        if (!showRationale) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("앱의 원활한 사용을 위해 권한을 허용해야 합니다. 앱 정보로 이동합니다.\n [저장공간]권한을 허용해주세요.");
                            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("앱의 원활한 사용을 위해 권한을 허용해야 합니다.");
                            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    requestPermission();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        }
                    } else {
                        Utiles.customToast(this, "권한을 허용하였습니다.").show();
                        // Initialize 코드
                    }
                }


            }
            break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestNotificationPolicyAccess();
        if (getIntent().getStringExtra("tag") != null) {
            if (!getIntent().getStringExtra("tag").equals("notice")) {
                final String toRoom = getIntent().getStringExtra("tag");
                getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, new ChatFragment()).commitAllowingStateLoss();
                bottomNavigationView.setSelectedItemId(R.id.action_chat);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                View noticeView = getLayoutInflater().from(MainActivity.this).inflate(R.layout.access, null);
                builder.setView(noticeView);
                dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.setCancelable(false);
                dialog.show();
                UserMap.clearComments();
                new getMessage().execute(toRoom);
            }
        }


        View v = bottomNavigationMenuView.getChildAt(2);
        BottomNavigationItemView itemView = (BottomNavigationItemView) v;
        final View badge = LayoutInflater.from(this).inflate(R.layout.notification_badge, itemView, true);
        final TextView badgeView = badge.findViewById(R.id.badge);

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) { // 해당되는 chatrooms들의 키값들이 넘어옴.
                int count = 0;
                for (final DataSnapshot item : dataSnapshot.getChildren()) {// normalChat, officerChat
                    final LastChat lastChat = item.getValue(LastChat.class);

                    if (lastChat.getUsers() == null) {
                        return;
                    }
                    if (lastChat.getUsers().get(uid) == null) {
                        count += 0;
                    } else {
                        count += Integer.parseInt(lastChat.getUsers().get(uid) + "");
                    }
                }
                if (count > 0) {
                    String c = count + "";
                    badgeView.setText(c);
                    badgeView.setVisibility(View.VISIBLE);
                } else {
                    badgeView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("lastChat").orderByChild("existUsers/" + uid).equalTo(true).addValueEventListener(valueEventListener);



    }

    void passPushTokenToServer() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(MainActivity.this, new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String token = instanceIdResult.getToken();
                user.setPushToken(token);
                FirebaseDatabase.getInstance().getReference().child("Users").child(uid).setValue(user);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_option_menu, menu);
        if (user.getHospital().equals("개발자")) {
            menu.findItem(R.id.admin).setVisible(true);
        }
        if (user.getHospital().equals("개발자")) {
            menu.findItem(R.id.invite).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.logout) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Map<String, Object> map = new HashMap<>();
            map.put("pushToken", "");
            FirebaseDatabase.getInstance().getReference().child("Users").child(uid).updateChildren(map);
            NotificationManagerCompat.from(this).cancelAll();
            UserMap.clearApp();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.putExtra("logOut", "logOut");
            PreferenceManager.clear(this);
            startActivity(intent);
            finish();
        } else if (item.getItemId() == R.id.admin) {
            Intent intent = new Intent(MainActivity.this, AdminActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.invite) {
            Intent intent = new Intent(MainActivity.this, InviteActivity.class);
            startActivity(intent);
        }

        return true;
    }

    private void goChatRoom(String toRoom) {
        Intent intent = null;
        intent = new Intent(MainActivity.this, GroupMessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("toRoom", toRoom); // 방이름
        intent.putExtra("chatCount", newComments.size());// 채팅숫자
        intent.putParcelableArrayListExtra("imgList", (ArrayList<? extends Parcelable>) img_list);
        UserMap.setComments(newComments);
        ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(MainActivity.this, R.anim.frombottom, R.anim.totop);
        getIntent().removeExtra("tag");
        dialog.dismiss();
        startActivity(intent, activityOptions.toBundle());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            databaseReference.child("lastChat").removeEventListener(valueEventListener); // 이벤트 제거.
        }
    }

    private class getMessage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(final String... strings) {
            databaseReference.child("groupChat").child(strings[0]).child("comments").orderByChild("readUsers/" + uid).equalTo(false)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Map<String, Object> map = new HashMap<>();
                            if(dataSnapshot.getChildrenCount()>0){
                                for (DataSnapshot item : dataSnapshot.getChildren()) {
                                    map.put(Objects.requireNonNull(item.getKey())+"/readUsers/"+uid, true);
                                }
                                databaseReference.child("groupChat").child(strings[0]).child("comments").updateChildren(map);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
            databaseReference.child("groupChat").child(strings[0]).child("comments").orderByChild("existUser/" + uid).equalTo(true).limitToLast(firstReadChatCount)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot i : dataSnapshot.getChildren()){
                                ChatModel.Comment c = i.getValue(ChatModel.Comment.class);
                                c.readUsers.put(uid, true);
                                c.setKey(i.getKey());
                                newComments.add(c);
                                Log.d("채팅", c.toString());
                                if (c.getType().equals("img")) {
                                    Img img = new Img();
                                    if (userMap.get(c.getUid()) == null) {
                                        img.setName("(알수없음)");
                                    } else {
                                        img.setName(userMap.get(c.getUid()).getUserName());
                                    }
                                    String uri;
                                    if (c.message.startsWith("http")) {
                                        uri = c.message;
                                    } else {
                                        int firstIndex = c.message.indexOf("/");
                                        int secondIndex = c.message.indexOf("/", firstIndex + 1);
                                        uri = c.message.substring(secondIndex + 1);
                                    }
                                    img.setUri(uri);
                                    String time = String.valueOf((long) c.getTimestamp());
                                    img.setTime(time);
                                    img_list.add(img);
                                }
                            }
                            Map<String, Object> map = new HashMap<>();
                            map.put(uid, 0);
                            databaseReference.child("lastChat").child(strings[0]).child("users").updateChildren(map);
                            goChatRoom(strings[0]);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
            return null;
        }
    }
    private void requestNotificationPolicyAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !isNotificationPolicyAccessGranted()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("앱 설정으로 이동합니다. \n[방해금지권한]을 허용해주세요.");
            builder.setMessage("해당 기종은 알림기능사용을 위해 해당 권한이 필요합니다.");
            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(android.provider.Settings.
                            ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }
    private boolean isNotificationPolicyAccessGranted()  {
        NotificationManager notificationManager = (NotificationManager)
                MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return notificationManager.isNotificationPolicyAccessGranted();
        }
        return true;
    }
}
