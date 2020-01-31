package com.onvit.chatapp.chat;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.LoginActivity;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.LastChat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class SelectGroupChatActivity extends AppCompatActivity {

    private Toolbar chatToolbar;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM월dd일");
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm");
    private ValueEventListener valueEventListener;
    private FirebaseAuth firebaseAuth;
    private List<String> userCount = new ArrayList<>();
    private ValueEventListener countEventListener;
    private List<LastChat> chatModels = new ArrayList<>();
    private List<String> keys = new ArrayList<>();
    private List<String> count = new ArrayList<>();
    private Map<String, Object> countMap = new HashMap<>();
    private String uid;
    private String text = null;
    private Uri uri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group_chat);
        chatToolbar = findViewById(R.id.chat_toolbar);
        firebaseAuth = FirebaseAuth.getInstance();
        setSupportActionBar(chatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("공유할 채팅방을 선택하세요.");
        if (firebaseAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            firebaseAuth.signOut();
            startActivity(intent);
            finish();
        }
        getIntent().addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        if (getIntent().getStringExtra("text") != null) {
            text = getIntent().getStringExtra("text");
        }
        if (getIntent().getParcelableExtra("shareUri") != null) {
            uri = getIntent().getParcelableExtra("shareUri");
            Log.d("공유img", uri.toString());

        }

        RecyclerView recyclerView = findViewById(R.id.selectGroupChat_recyclerview);
        recyclerView.setAdapter(new SelectGroupChatRecyclerAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            databaseReference.child("lastChat").removeEventListener(valueEventListener);
        }
        if (countEventListener != null) {
            databaseReference.child("groupChat").child("normalChat").child("comments").removeEventListener(countEventListener);
            databaseReference.child("groupChat").child("officerChat").child("comments").removeEventListener(countEventListener);
        }
    }

    class SelectGroupChatRecyclerAdapter extends RecyclerView.Adapter<SelectGroupChatRecyclerAdapter.CustomViewHolder> {

        public SelectGroupChatRecyclerAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();// 클라이언트uid
            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) { // 해당되는 chatrooms들의 키값들이 넘어옴.
                    chatModels.clear();
                    count.clear();
                    keys.clear();
                    userCount.clear();
//                    Log.d("등급11", grade);
                    for (final DataSnapshot item : dataSnapshot.getChildren()) {// normalChat, officerChat
                        final LastChat lastChat = item.getValue(LastChat.class);
                        chatModels.add(lastChat);// 채팅방 밖에 표시할 내용들.
                        keys.add(item.getKey());// normalChat, officerChat 채팅방 이름.
                        count.add(lastChat.getUsers().get(uid) + ""); // 안읽은 숫자
                        userCount.add(lastChat.getExistUsers().size() + "");

                        countEventListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                countMap.put(item.getKey(), dataSnapshot.getChildrenCount());
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        };
                        databaseReference.child("groupChat").child(item.getKey()).child("comments").orderByChild("existUser/" + uid).equalTo(true).addValueEventListener(countEventListener);

                    }
                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            databaseReference.child("lastChat").orderByChild("existUsers/" + uid).equalTo(true).addValueEventListener(valueEventListener);
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_chat, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomViewHolder holder, final int position) {
            //position0번 부터 붙음

            holder.textView_count.setVisibility(View.INVISIBLE);
            GradientDrawable gradientDrawable = (GradientDrawable) SelectGroupChatActivity.this.getDrawable(R.drawable.radius);
            holder.imageView.setBackground(gradientDrawable);
            holder.imageView.setClipToOutline(true);


            holder.textView_title.setText(chatModels.get(position).getChatName());

            holder.textView_user_count.setText(userCount.get(position));
            //마지막으로 보낸 메세지

            String lastChat = chatModels.get(position).getLastChat();
            holder.textView_last_message.setText(lastChat);
            //보낸 시간
            if (chatModels.get(position).getTimestamp() == null) {
                holder.textView_timestamp.setVisibility(View.INVISIBLE);
                holder.textView_timestamp2.setVisibility(View.INVISIBLE);
            } else {
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                long unixTime = (long) chatModels.get(position).getTimestamp();
                Date date = new Date(unixTime);
                Date date2 = new Date();

                SimpleDateFormat sd = new SimpleDateFormat("yyyyMMddHHmm");

                String dS = sd.format(date);
                String dS2 = sd.format(date2);
                holder.textView_timestamp.setVisibility(View.VISIBLE);
                holder.textView_timestamp2.setVisibility(View.VISIBLE);
                if (dS2.substring(0, 8).equals(dS.substring(0, 8))) {
                    if (Integer.parseInt(dS.substring(8)) < 1200) {
                        holder.textView_timestamp.setText("오전");
                        holder.textView_timestamp2.setText(simpleDateFormat2.format(date));
                    } else {
                        holder.textView_timestamp.setText("오후");
                        holder.textView_timestamp2.setText(simpleDateFormat2.format(date));
                    }
                } else {
                    holder.textView_timestamp.setText(simpleDateFormat.format(date));
                    holder.textView_timestamp2.setText(simpleDateFormat2.format(date));
                }
            }

            //안읽은 메세지 숫자
            if (!count.get(position).equals("0") && !count.get(position).equals("null")) {
                holder.textView_count.setText(count.get(position));
                holder.textView_count.setVisibility(View.VISIBLE);
            }

            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        if (countMap == null || countMap.size() == 0) {
                            return;
                        } else {
                            //채팅방들어갈때 안읽은 메세지들 모두 읽음으로 처리해서 넘어감.
                            databaseReference.child("groupChat").child(keys.get(position)).child("comments").orderByChild("readUsers/" + uid).equalTo(false).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Map<String, Object> map = new HashMap<>();
                                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                                        ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);
                                        comment.readUsers.put(uid, true);
                                        map.put(item.getKey(), comment);
                                    }
                                    databaseReference.child("groupChat").child(keys.get(position)).child("comments").updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Intent intent = null;
                                            intent = new Intent(SelectGroupChatActivity.this, GroupMessageActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                            intent.putExtra("toRoom", keys.get(position)); // 방이름
                                            intent.putExtra("chatCount", (Long) countMap.get(keys.get(position)));// 채팅숫자
                                            if (text != null) {
                                                Log.d("공유Select", text);
                                                intent.putExtra("shareText", text);
                                            }
                                            if (uri != null) {
                                                Log.d("파일Select", uri.toString());
                                                Log.d("파일 경로c", getIntent().getStringExtra("filePath"));
                                                intent.putExtra("filePath", getIntent().getStringExtra("filePath"));
                                                intent.putExtra("shareUri", uri);
                                            }

                                            ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(SelectGroupChatActivity.this, R.anim.frombottom, R.anim.totop);
                                            startActivity(intent, activityOptions.toBundle());
                                            finish();
                                        }
                                    });

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                        }


                    } else {

                    }
                }
            });

        }

        @Override
        public int getItemCount() {
            return chatModels.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView_title;
            public TextView textView_last_message;
            public TextView textView_timestamp;
            public TextView textView_count;
            public CheckBox checkBox;
            private TextView textView_timestamp2;
            private TextView textView_user_count;

            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.chatitem_imageview);
                textView_title = view.findViewById(R.id.chatitem_textview_title);
                textView_last_message = view.findViewById(R.id.chatitem_textview_lastMessage);
                textView_timestamp = view.findViewById(R.id.chatitem_textview_timestamp);
                textView_count = view.findViewById(R.id.chatitem_textview_count);
                textView_timestamp2 = view.findViewById(R.id.chatitem_textview_timestamp2);
                checkBox = view.findViewById(R.id.frienditem_checkbox);
                textView_user_count = view.findViewById(R.id.user_count);
            }
        }
    }
}
