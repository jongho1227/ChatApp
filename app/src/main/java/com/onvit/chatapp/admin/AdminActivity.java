package com.onvit.chatapp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.KCHA;
import com.onvit.chatapp.model.User;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jxl.Sheet;
import jxl.Workbook;

public class AdminActivity extends AppCompatActivity implements View.OnClickListener {
    Button updateBtn, deleteChatBtn, updateUser;
    Map<String, Object> nameList = new HashMap<>();
    private List<String> deleteKey = new ArrayList<>();
    private List<String> deleteKey2 = new ArrayList<>();
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

                break;
            case R.id.deleteChat:
                final Date date = new Date();
                long twoM = (24L * 60 * 60 * 1000 * 60);
                final long oldDate = date.getTime() - twoM;
                //두달지난거 삭제함.
                FirebaseDatabase.getInstance().getReference().child("groupChat").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot i : dataSnapshot.getChildren()){
                            String chatName = i.getKey();
                            Log.d("채팅방", chatName);
                            deleteChat(oldDate, chatName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


                break;
            case R.id.updateUser:
                aaa();//엑셀에 표시된 등급에 맞게 수정하는 쿼리.
                break;
        }
    }

    private void deleteChat(final long date, final String chatName) {
        FirebaseDatabase.getInstance().getReference().child("groupChat").child(chatName).child("comments")
                .orderByChild("timestamp")
                .endAt(date).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> map = new HashMap<>();
                Log.d("삭제", dataSnapshot.getChildrenCount() + "");
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    map.put(item.getKey(), null);
                    ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);

                    if (comment.getType().equals("img")) {
                        deleteKey.add(item.getKey());
                    }
                    if (comment.getType().equals("file")) {
                        int a = comment.getMessage().lastIndexOf("https");
                        int b = comment.getMessage().substring(0, a).lastIndexOf(".");
                        String ext = comment.getMessage().substring(0, a).substring(b + 1);
                        deleteKey2.add(item.getKey() + "." + ext);
                    }
                }
                for (String d : deleteKey) {
                    FirebaseStorage.getInstance().getReference().child("Image Files").child(chatName).child(d).delete();
                }
                for (String d : deleteKey2) {
                    FirebaseStorage.getInstance().getReference().child("Document Files").child(chatName).child(d).delete();
                }
                FirebaseDatabase.getInstance().getReference().child("groupChat").child(chatName).child("comments").updateChildren(map);
                FirebaseDatabase.getInstance().getReference().child("Vote").child(chatName).updateChildren(map);

                Toast.makeText(AdminActivity.this, String.format("%s채팅방 지워진 채팅갯수 : %d개", chatName, dataSnapshot.getChildrenCount()), Toast.LENGTH_SHORT).show();
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
                    for (int col = 1; col < colTotal-3; col++) {
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
                    Log.d("인원", sb.toString());
                    if(sb.getName()==null || sb.getName().trim().equals("")){
                        Log.d("dd", "nodata");
                    }else{
                        total++;
                        list.put(sb.getName()+total, sb);
                        if (sb.getGrade().equals("0")) {
                            normal++;
                        } else {
                            officer++;
                        }
                        Log.d("인원목록", sb.getName()+" : "+total+" : "+list.size());
                    }
                }
                Log.d("인원목록", list.toString());
                Log.d("인원목록", list.size()+"");
                nameList = list;
                FirebaseDatabase.getInstance().getReference().child("KCHA").setValue(null);
                FirebaseDatabase.getInstance().getReference().child("KCHA").setValue(list);
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
                                if (phone.equals(user.getTel()) && kcha.getName().equals(user.getUserName())) {

                                    Map<String, Object> map = new HashMap<>();
                                    Map<String, Object> map2 = new HashMap<>();
                                    Map<String, Object> map3 = new HashMap<>();
                                    user.setUserName(kcha.getName());
                                    user.setHospital(kcha.getHospital());

                                    if(!grade.equals(user.getGrade())){
                                        if(user.getGrade().equals("회원")){
                                            // 회원->임원이 됐을경우는 임원쪽에 추가만 하면됨
                                            user.setGrade(grade);
                                            map.put("normalChat/users/" + user.getUid(), false);
                                            map2.put("officerChat/users/" + user.getUid(), 0);
                                            map2.put("officerChat/existUsers/" + user.getUid(), true);
                                        }else if(user.getGrade().equals("임원")){
                                            // 임원->회원이 됐을경우는 임원쪽에서 삭제하면됨.
                                            user.setGrade(grade);
                                            map.put("officerChat/users/" + user.getUid(), null);
                                            map2.put("officerChat/users/" + user.getUid(), null);
                                            map2.put("officerChat/existUsers/" + user.getUid(), null);

                                        }
                                    }else{
                                        if(user.getGrade().equals("회원")){
                                            map.put("normalChat/users/" + user.getUid(), false);
                                        }else if(user.getGrade().equals("임원")){
                                            map.put("normalChat/users/" + user.getUid(), false);
                                            map.put("officerChat/users/" + user.getUid(), false);
                                        }
                                    }
                                    map3.put(item.getKey(), user);
                                    FirebaseDatabase.getInstance().getReference().child("Users").updateChildren(map3);
                                    FirebaseDatabase.getInstance().getReference().child("groupChat").updateChildren(map);
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
