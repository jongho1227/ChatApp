package com.onvit.chatapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.model.KCHA;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.SHA256Util;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jxl.Sheet;
import jxl.Workbook;

public class AdminActivity extends AppCompatActivity implements View.OnClickListener {
    Button updateBtn, deleteChatBtn, updateUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        updateBtn = findViewById(R.id.updateBtn);
        deleteChatBtn = findViewById(R.id.deleteChat);
        updateUser = findViewById(R.id.updateUser);


        updateBtn.setOnClickListener(this);
        deleteChatBtn.setOnClickListener(this);
        updateUser.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.updateBtn:
                //회원들 정보 넣음.
                insertExel();
//                updateInfo();
//                aaa();//엑셀에 표시된 등급에 맞게 수정하는 쿼리.
                break;
            case R.id.deleteChat:
                final Date date = new Date();
                long twoM = (24L * 60 * 60 * 1000 * 60);
                long oldDate = date.getTime() - twoM;
                //두달지난거 삭제함.
                String chatName = "normalChat";
                deleteChat(oldDate, chatName);
                chatName = "officerChat";
                deleteChat(oldDate, chatName);
                break;
            case R.id.updateUser:
                updateUser();
                break;
        }
    }

    private void updateInfo() {
        //병원이름 엑셀파일이랑 맞춤.
        FirebaseDatabase.getInstance().getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    final User user = item.getValue(User.class);
                    final String key = item.getKey();
                    String phone = user.getTel().substring(0, 3) + "-" + user.getTel().substring(3, 7) + "-" + user.getTel().substring(7);
                    FirebaseDatabase.getInstance().getReference().child("KCHA").orderByChild("phone").equalTo(phone).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                if (dataSnapshot.getChildrenCount() == 1) {
                                    Map<String, Object> allMap = new HashMap<>();
                                    KCHA kcha = item.getValue(KCHA.class);
                                    user.setHospital(kcha.getHospital());
                                    allMap.put(key, user);
                                    FirebaseDatabase.getInstance().getReference().child("Users").updateChildren(allMap);
                                    FirebaseDatabase.getInstance().getReference().child("groupChat").child("normalChat").child("userInfo").updateChildren(allMap);
                                    if (user.getGrade().equals("임원")) {
                                        FirebaseDatabase.getInstance().getReference().child("groupChat").child("officerChat").child("userInfo").updateChildren(allMap);
                                    }

                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void updateUser() {
        //유저 일반으로 들어가있는사람 -> 회원으로 변경
        //비밀번호 없앴음.
        FirebaseDatabase.getInstance().getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> allMap = new HashMap<>();
                Map<String, Object> officerMap = new HashMap<>();
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    String key = item.getKey();
                    User user = item.getValue(User.class);
                    if (user.getGrade().equals("일반")) {
                        user.setGrade("회원");
                    } else {
                        officerMap.put(key, user);
                    }
                    allMap.put(key, user);
                }
                FirebaseDatabase.getInstance().getReference().child("Users").updateChildren(allMap);
                FirebaseDatabase.getInstance().getReference().child("groupChat").child("normalChat").child("userInfo").updateChildren(allMap);
                FirebaseDatabase.getInstance().getReference().child("groupChat").child("officerChat").child("userInfo").updateChildren(officerMap);
                Toast.makeText(AdminActivity.this, "업데이트완료", Toast.LENGTH_SHORT).show();


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
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    map.put(item.getKey(), null);
                }
                FirebaseDatabase.getInstance().getReference().child("groupChat").child(chatName).child("comments").updateChildren(map);
                Toast.makeText(AdminActivity.this, "지워진 채팅갯수" + dataSnapshot.getChildrenCount() + "개", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void insertExel() {
        try {
            InputStream is = getBaseContext().getResources().getAssets().open("대한지역병원협의회 회원명단.xls");
            Workbook wb = Workbook.getWorkbook(is);

            Map<String, Object> list = new HashMap<>();
            Sheet sheet = wb.getSheet(0);   // 시트 불러오기
            if (sheet != null) {
                int colTotal = sheet.getColumns();    // 전체 컬럼
                int rowIndexStart = 3;                  // row 인덱스 시작
                int rowTotal = sheet.getRows();
                int total = 0;
                int officer = 0;
                int normal = 0;
                KCHA sb;
                for (int row = rowIndexStart; row < rowTotal; row++) {
                    sb = new KCHA();
                    for (int col = 1; col < colTotal - 3; col++) {
                        String contents = sheet.getCell(col, row).getContents();
                        switch (col) {
                            case 1:
                                sb.setName(contents);
                                break;
                            case 2:
                                sb.setHospital(contents);
                                break;
                            case 3:
                                sb.setPhone(contents);
                                break;
                            case 4:
                                sb.setMajor(contents);
                                break;
                            case 5:
                                sb.setAddress(contents);
                                break;
                            case 6:
                                sb.setEmail(contents);
                                break;
                            case 7:
                                sb.setTel(contents);
                                break;
                            case 8:
                                sb.setFax(contents);
                                break;
                            case 9:
                                sb.setmNo(contents);
                                break;
                            case 10:
                                sb.setGrade(contents);
                                break;
                        }
                    }
                    list.put(sb.getName(), sb);
                    if (sb.getGrade().equals("0")) {
                        normal++;
                    } else {
                        officer++;
                    }
                    total++;
                }
                FirebaseDatabase.getInstance().getReference().child("KCHA").updateChildren(list);
                Toast.makeText(AdminActivity.this, "임원 : " + officer + "명," + "회원 : " + normal + "명," + "총원 : " + total + "명", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void aaa() {
        FirebaseDatabase.getInstance().getReference().child("KCHA").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    final KCHA kcha = item.getValue(KCHA.class);
                    final String phone = kcha.getPhone().replaceAll("-", "");
                    final String grade;
                    if (kcha.getGrade().equals("0")) {
                        grade = "회원";
                    } else {
                        grade = "임원";
                    }
                    FirebaseDatabase.getInstance().getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                User user = item.getValue(User.class);
                                if (phone.equals(user.getTel())) {
                                    if (!user.getGrade().equals(grade)) {
                                        user.setGrade(grade);
                                        Map<String, Object> map = new HashMap<>();
                                        map.put(item.getKey(), user);
                                        FirebaseDatabase.getInstance().getReference().child("Users").updateChildren(map);
                                    }
                                    Map<String, Object> remove = new HashMap<>();
                                    Map<String, Object> remove2 = new HashMap<>();
                                    remove.put("normalChat/userInfo/" + user.getUid(), null);
                                    remove.put("officerChat/userInfo/" + user.getUid(), null);
                                    remove.put("normalChat/users/" + user.getUid(), null);
                                    remove.put("officerChat/users/" + user.getUid(), null);
                                    remove2.put("normalChat/chatName", "회원채팅방");
                                    remove2.put("officerChat/chatName", "임원채팅방");
                                    remove2.put("normalChat/users/" + user.getUid(), null);
                                    remove2.put("officerChat/users/" + user.getUid(), null);
                                    remove2.put("normalChat/existUsers/" + user.getUid(), null);
                                    remove2.put("officerChat/existUsers/" + user.getUid(), null);
                                    FirebaseDatabase.getInstance().getReference().child("groupChat").updateChildren(remove);
                                    //lastChat방에 uid와 안읽은 메세지수 0으로 집어넣음.
                                    FirebaseDatabase.getInstance().getReference().child("lastChat").updateChildren(remove2);

                                    Map<String, Object> map = new HashMap<>();
                                    Map<String, Object> map2 = new HashMap<>();
                                    //각각의 그룹채팅방에 유저 정보 / 접속여부를 넣음
                                    if (grade.equals("임원")) {
                                        map.put("normalChat/userInfo/" + user.getUid(), user);
                                        map.put("officerChat/userInfo/" + user.getUid(), user);
                                        map.put("normalChat/users/" + user.getUid(), false);
                                        map.put("officerChat/users/" + user.getUid(), false);
                                        map2.put("normalChat/chatName", "회원채팅방");
                                        map2.put("officerChat/chatName", "임원채팅방");
                                        map2.put("normalChat/users/" + user.getUid(), 0);
                                        map2.put("officerChat/users/" + user.getUid(), 0);
                                        map2.put("normalChat/existUsers/" + user.getUid(), true);
                                        map2.put("officerChat/existUsers/" + user.getUid(), true);
                                    } else {
                                        map.put("normalChat/userInfo/" + user.getUid(), user);
                                        map.put("normalChat/users/" + user.getUid(), false);
                                        map2.put("normalChat/chatName", "회원채팅방");
                                        map2.put("normalChat/users/" + user.getUid(), 0);
                                        map2.put("normalChat/existUsers/" + user.getUid(), true);
                                    }

                                    FirebaseDatabase.getInstance().getReference().child("groupChat").updateChildren(map);

                                    //lastChat방에 uid와 안읽은 메세지수 0으로 집어넣음.
                                    FirebaseDatabase.getInstance().getReference().child("lastChat").updateChildren(map2);
                                }

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
