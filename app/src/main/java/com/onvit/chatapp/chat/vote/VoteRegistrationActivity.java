package com.onvit.chatapp.chat.vote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.model.Vote;
import com.onvit.chatapp.util.PreferenceManager;
import com.onvit.chatapp.util.Utiles;
import com.skyhope.eventcalenderlibrary.CalenderEvent;
import com.skyhope.eventcalenderlibrary.listener.CalenderDayClickListener;
import com.skyhope.eventcalenderlibrary.model.DayContainerModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VoteRegistrationActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RadioGroup radioGroup;
    private RadioButton radio_text, radio_date;
    private LinearLayout text_layout, date_layout;
    private TextView plus, register, deadline;
    private EditText vote_title;
    private String date, toRoom, uid;
    private ValueEventListener accessChatMemberEventListener;
    private DatabaseReference databaseReference;
    Map<String, Object> messageReadUsers = new HashMap<>();
    Map<String, Object> existUserGroupChat = new HashMap<>();
    private List<String> registration_ids;
    private Map<String, Object> userList;
    SimpleDateFormat sd = new SimpleDateFormat("yyyy년 MM월 dd일");
    ArrayList<User> userList2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote_registration);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundResource(R.color.notice);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        String chatName;
        toRoom = getIntent().getStringExtra("room");
        if (toRoom.equals("normalChat")) {
            chatName = "회원채팅방 투표등록";
        } else if (toRoom.equals("officerChat")){
            chatName = "임원채팅방 투표등록";
        } else{
            chatName = toRoom +"투표등록";
        }
        actionBar.setTitle(chatName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        userList2 = getIntent().getParcelableArrayListExtra("userList");
        userList = new HashMap<>();
        registration_ids = new ArrayList<>();
        radioGroup = findViewById(R.id.radio_group);
        radio_text = findViewById(R.id.radio_text);
        radio_date = findViewById(R.id.radio_date);
        text_layout = findViewById(R.id.text_group);
        date_layout = findViewById(R.id.date_group);
        plus = findViewById(R.id.plus);
        vote_title = findViewById(R.id.vote_title);
        register = findViewById(R.id.register);
        deadline = findViewById(R.id.deadline);

        databaseReference = FirebaseDatabase.getInstance().getReference();

        uid = PreferenceManager.getString(VoteRegistrationActivity.this, "uid");

        final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        //라디오 버튼 클릭시 이벤트
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i==R.id.radio_text){
                    removeView(date_layout, i);
                    text_layout.setVisibility(View.VISIBLE);
                    date_layout.setVisibility(View.GONE);
                }else if(i==R.id.radio_date){
                    removeView(text_layout, i);
                    text_layout.setVisibility(View.GONE);
                    date_layout.setVisibility(View.VISIBLE);

                }
            }
        });

        //항목추가 버튼 클릭
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(radio_text.isChecked()){
                    EditText editText = (EditText) layoutInflater.inflate(R.layout.radio_text, text_layout, false);
                    text_layout.addView(editText);
                }else if(radio_date.isChecked()){
                    final TextView textView = (TextView) layoutInflater.inflate(R.layout.radio_date, date_layout, false);
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            selectDate(textView);
                        }
                    });
                    date_layout.addView(textView);
                }
            }
        });

        //마감시간설정
        deadline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectDate(deadline);

            }
        });

        //날짜선택 달력나오는거.
        int count = date_layout.getChildCount();
        for(int a=0; a<count; a++){
            final TextView content = (TextView) date_layout.getChildAt(a);
            content.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectDate(content);
                }
            });
        }

        for(User u : userList2){
            userList.put(u.getUid(), false);
            if(u.getUid().equals(uid)){
                continue;
            }
            registration_ids.add(u.getPushToken());

        }

        //투표등록하기
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(VoteRegistrationActivity.this);
                builder.setMessage("투표를 등록하시겠습니까?");
                builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        final AlertDialog a = Utiles.createLoadingDialog(VoteRegistrationActivity.this, "투표를 등록중입니다.");

                        final String title = vote_title.getText().toString();

                        if(title.trim().equals("")){
                            a.dismiss();
                            Toast.makeText(VoteRegistrationActivity.this, "제목을 입력하여 주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(deadline.getText().toString().trim().equals("")){
                            a.dismiss();
                            Toast.makeText(VoteRegistrationActivity.this, "마감시간을 정해주세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Date d = new Date();
                        long todayTime = d.getTime();
                        long endDayTime = (long) deadline.getTag(R.id.deadline);
                        Date d1 = new Date(endDayTime);
                        String today = sd.format(d);
                        String endDay = sd.format(d1);


                        if(today.equals(endDay) || endDayTime-todayTime<0){
                            a.dismiss();
                            Toast.makeText(VoteRegistrationActivity.this, "마감시간은 오늘 이전으로 할 수 없습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Vote vote = new Vote();
                        vote.setTitle(title); //투표제목
                        vote.setDeadline((Long) deadline.getTag(R.id.deadline));//투표마감시간
                        Map<String, Object> listMap = new HashMap<>();
                        if(radio_text.isChecked()){
                            vote.setType("텍스트");
                            for(int j=0; j<text_layout.getChildCount(); j++){
                                EditText content = (EditText) text_layout.getChildAt(j);
                                String c = content.getText().toString();
                                if(c.trim().equals("")){
                                    continue;
                                }
                                listMap.put(c,userList);
                            }

                        }else if(radio_date.isChecked()){
                            vote.setType("날짜");
                            for(int j=0; j<date_layout.getChildCount(); j++){
                                TextView content = (TextView) date_layout.getChildAt(j);
                                String c = content.getText().toString();
                                if(c.trim().equals("")){
                                    continue;
                                }
                                listMap.put(c,userList);
                            }
                        }
                        if(listMap.size()==0){
                            a.dismiss();
                            Toast.makeText(VoteRegistrationActivity.this, "하나이상의 항목을 등록하세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        vote.setContent(listMap);
                        vote.setRegistrant(uid);
                        final DatabaseReference userMessageKeyRef = databaseReference.child("Vote").child(toRoom).push();
                        databaseReference.child("Vote").child(toRoom).child(userMessageKeyRef.getKey()).setValue(vote).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                accessChatMemberEventListener = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                                            Log.d("순서", "accessChatMemberEventListener");
                                            messageReadUsers.put(item.getKey(), item.getValue());
                                            existUserGroupChat.put(item.getKey(), true);
                                        }
                                        final ChatModel.Comment comment = new ChatModel.Comment();
                                        comment.uid = uid; // 채팅친사람
                                        comment.message = title +"!@#!@#"+deadline.getTag(R.id.deadline); // 채팅친내용
                                        comment.timestamp = new Date().getTime(); // 채팅친 시간
                                        comment.type = "vote"; // 채팅 친 종류
                                        comment.readUsers = messageReadUsers; // 읽은사람들
                                        comment.existUser = existUserGroupChat;

                                        databaseReference.child("groupChat").child(toRoom).child("comments").child(userMessageKeyRef.getKey()).setValue(comment);

                                        databaseReference.child("lastChat").child(toRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                Map<String, Object> unreadUser = (Map<String, Object>) dataSnapshot.getValue();
                                                Map<String, Object> read = comment.readUsers; // 읽은사람구분
                                                Set<String> keys = read.keySet();
                                                Iterator<String> it = keys.iterator();
                                                while (it.hasNext()) {
                                                    String key1 = it.next();
                                                    Object value = read.get(key1);
                                                    if ((Boolean) value == false) {
                                                        //메세지 안읽었으면 기존꺼에서 1추가함.
                                                        unreadUser.put(key1, ((int) (long) unreadUser.get(key1)) + 1);
                                                    }
                                                }
                                                Map<String, Object> lastMap = new HashMap<>();
                                                lastMap.put("lastChat", "투표 : "+comment.message.split("!@#!@#")[0]);
                                                lastMap.put("timestamp", comment.timestamp);
                                                lastMap.put("users", unreadUser);
                                                FirebaseDatabase.getInstance().getReference().child("lastChat").child(toRoom).updateChildren(lastMap); // 마지막 메세지 표시
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                        a.dismiss();
                                        Toast.makeText(VoteRegistrationActivity.this, "투표를 등록하였습니다.", Toast.LENGTH_SHORT).show();
                                        Utiles.sendFcm(registration_ids, "투표를 등록하였습니다.", VoteRegistrationActivity.this, toRoom);
                                        finish();
                                        overridePendingTransition(R.anim.fromright, R.anim.toleft);//화면 사라지는 방향
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                };
                                databaseReference.child("groupChat").child(toRoom).child("users").addValueEventListener(accessChatMemberEventListener);
                            }
                        });
                    }
                }).setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (accessChatMemberEventListener != null) {
            databaseReference.child("groupChat").child(toRoom).child("users").removeEventListener(accessChatMemberEventListener);
        }
    }

    private void selectDate(final TextView dateView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(VoteRegistrationActivity.this);
        View v = LayoutInflater.from(VoteRegistrationActivity.this).inflate(R.layout.calendar, null);
        builder.setView(v);
        final AlertDialog dialog = builder.create();
        dialog.show();
        final long[] time = new long[1];
        CalenderEvent calenderEvent = v.findViewById(R.id.calender_event);
        calenderEvent.initCalderItemClickCallback(new CalenderDayClickListener() {
            @Override
            public void onGetDay(DayContainerModel dayContainerModel) {
                sd = new SimpleDateFormat("yyyy년 MM월 dd일");
                Date d = new Date(dayContainerModel.getTimeInMillisecond());
                time[0] = dayContainerModel.getTimeInMillisecond();
                date = sd.format(d);
            }
        });
        Button select = v.findViewById(R.id.select);
        Button cancel = v.findViewById(R.id.cancel);

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dateView.setText(date);
                dateView.setTag(R.id.deadline, time[0]);
                dialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    private void removeView(LinearLayout layout, int flag){
        int count = layout.getChildCount();
        for(int a=0; a<count; a++){
            if(a>2){
                layout.removeViewAt(3);
            }else{
                if(flag==R.id.radio_text){
                    TextView content = (TextView) layout.getChildAt(a);
                    content.setText("");
                }else if(flag == R.id.radio_date){
                    EditText content = (EditText) layout.getChildAt(a);
                    content.setText("");

                }

            }
        }
    }

    //뒤로가기 눌렀을때
    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(VoteRegistrationActivity.this);
        builder.setMessage("작성을 취소하시겠습니까?");
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
                overridePendingTransition(R.anim.fromright, R.anim.toleft);//화면 사라지는 방향
            }
        }).setNegativeButton("아니요", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //툴바에 뒤로가기 버튼
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
