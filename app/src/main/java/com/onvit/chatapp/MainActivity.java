package com.onvit.chatapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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
import com.onvit.chatapp.chat.SelectGroupChatActivity;
import com.onvit.chatapp.contact.PeopleFragment;
import com.onvit.chatapp.model.LastChat;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.notice.NoticeFragment;
import com.onvit.chatapp.util.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final static int PERMISSION_REQUEST_CODE = 1000;
    BottomNavigationMenuView bottomNavigationMenuView;
    Fragment notice = new NoticeFragment();
    Fragment people = new PeopleFragment();
    Fragment shop = new ShoppingFragment();
    private FirebaseAuth firebaseAuth;
    private String text = null;
    private Uri uri = null;
    private User user;
    private String uid;
    private ValueEventListener valueEventListener;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    BottomNavigationView bottomNavigationView;

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
        user = getIntent().getParcelableExtra("user");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        PreferenceManager.setString(MainActivity.this, "name", user.getUserName());
        PreferenceManager.setString(MainActivity.this, "hospital", user.getHospital());
        PreferenceManager.setString(MainActivity.this, "phone", user.getTel());
        PreferenceManager.setString(MainActivity.this, "uid", user.getUid());


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
                    Toast.makeText(this, "권한을 받아오는데 실패하였습니다.", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, "권한을 허용하였습니다.", Toast.LENGTH_SHORT).show();
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
        if (getIntent().getStringExtra("tag") != null) {
            if (getIntent().getStringExtra("tag").equals("normalChat") || getIntent().getStringExtra("tag").equals("officerChat")) {
                getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_fragmentLayout, new ChatFragment()).commitAllowingStateLoss();
                bottomNavigationView.setSelectedItemId(R.id.action_chat);
                getIntent().removeExtra("tag");
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
                    count += Integer.parseInt(lastChat.getUsers().get(uid) + "");
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
                FirebaseDatabase.getInstance().getReference().child("groupChat").child("normalChat").child("userInfo").child(uid).setValue(user);

                if (user.getGrade().equals("임원")) {
                    FirebaseDatabase.getInstance().getReference().child("groupChat").child("officerChat").child("userInfo").child(uid).setValue(user);
                    ;
                }
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
        if (user.getHospital().equals("개발자") || user.getHospital().equals("사무국장")) {
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
            FirebaseDatabase.getInstance().getReference().child("groupChat").child("normalChat").child("userInfo").child(uid).updateChildren(map);
            if (user.getGrade().equals("임원")) {
                FirebaseDatabase.getInstance().getReference().child("groupChat").child("officerChat").child("userInfo").child(uid).updateChildren(map);
            }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            databaseReference.child("lastChat").removeEventListener(valueEventListener); // 이벤트 제거.
        }
    }
}
