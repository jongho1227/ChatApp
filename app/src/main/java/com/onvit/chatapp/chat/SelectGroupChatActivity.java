package com.onvit.chatapp.chat;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.LoginActivity;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.Img;
import com.onvit.chatapp.model.LastChat;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.UserMap;
import com.onvit.chatapp.util.Utiles;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

public class SelectGroupChatActivity extends AppCompatActivity {

    private final int firstReadChatCount = Utiles.firstReadChatCount;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM월dd일", Locale.KOREA);
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm", Locale.KOREA);
    private ValueEventListener valueEventListener;
    private List<LastChat> chatModels = new ArrayList<>();
    private String uid;
    private String text = null;
    private Uri uri = null;
    private List<ChatModel.Comment> newComments = new ArrayList<>();
    private List<Img> img_list = new ArrayList<>();
    private AlertDialog dialog;
    private Map<String, User> users = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group_chat);
        Toolbar chatToolbar = findViewById(R.id.chat_toolbar);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        setSupportActionBar(chatToolbar);
        ActionBar actionBar = getSupportActionBar();
        users = UserMap.getInstance();
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

        }
        UserMap.setComments(newComments);
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
    }

    private void goChatRoom(String toRoom) {
        Intent intent = null;
        intent = new Intent(SelectGroupChatActivity.this, GroupMessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("toRoom", toRoom); // 방이름
        intent.putExtra("chatCount", newComments.size());// 채팅숫자
        intent.putParcelableArrayListExtra("imgList", (ArrayList<? extends Parcelable>) img_list);
        UserMap.setComments(newComments);
        if (text != null) {
            intent.putExtra("shareText", text);
        }
        if (uri != null) {
            intent.putExtra("filePath", getIntent().getStringExtra("filePath"));
            intent.putExtra("shareUri", uri);
        }
        ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(SelectGroupChatActivity.this, R.anim.frombottom, R.anim.totop);
        dialog.dismiss();
        startActivity(intent, activityOptions.toBundle());
        finish();

    }

    class SelectGroupChatRecyclerAdapter extends RecyclerView.Adapter<SelectGroupChatRecyclerAdapter.CustomViewHolder> {

        private SelectGroupChatRecyclerAdapter() {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();// 클라이언트uid
            valueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) { // 해당되는 chatrooms들의 키값들이 넘어옴.
                    chatModels.clear();
                    for (final DataSnapshot item : dataSnapshot.getChildren()) {// normalChat, officerChat
                        final LastChat lastChat = item.getValue(LastChat.class);
                        chatModels.add(lastChat);// 채팅방 밖에 표시할 내용들.

                    }
                    Collections.sort(chatModels);
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
        public void onBindViewHolder(@NonNull final CustomViewHolder holder, final int position) {
            //position0번 부터 붙음

            holder.textView_count.setVisibility(View.INVISIBLE);
            GradientDrawable gradientDrawable = (GradientDrawable) SelectGroupChatActivity.this.getDrawable(R.drawable.radius);
            holder.imageView.setBackground(gradientDrawable);
            holder.imageView.setClipToOutline(true);


            holder.textView_title.setText(chatModels.get(position).getChatName());

            holder.textView_user_count.setText(chatModels.get(position).getExistUsers().size()+"");
            //마지막으로 보낸 메세지

            String lastChat = chatModels.get(position).getLastChat();
            holder.textView_last_message.setText(lastChat);
            //보낸 시간
            if (chatModels.get(position).getTimestamp() == 0) {
                holder.textView_timestamp.setVisibility(View.INVISIBLE);
                holder.textView_timestamp2.setVisibility(View.INVISIBLE);
            } else {
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                simpleDateFormat2.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                long unixTime = (long) chatModels.get(position).getTimestamp();
                Date date = new Date(unixTime);
                Date date2 = new Date();
                SimpleDateFormat sd = new SimpleDateFormat("yyyyMMddHHmm", Locale.KOREA);
                sd.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
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
            if (chatModels.get(position).getUsers().get(uid)!=0) {
                holder.textView_count.setText(chatModels.get(position).getUsers().get(uid)+"");
                holder.textView_count.setVisibility(View.VISIBLE);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.checkBox.setChecked(true);
                }
            });
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        enterRoom(position);
                    }
                }
            });
        }

        private void enterRoom(int position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SelectGroupChatActivity.this);
            View noticeView = getLayoutInflater().from(SelectGroupChatActivity.this).inflate(R.layout.access, null);
            builder.setView(noticeView);
            dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
            UserMap.clearComments();
            new getMessage().execute(chatModels.get(position).getChatName());
        }

        @Override
        public int getItemCount() {
            return chatModels.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;
            private TextView textView_title;
            private TextView textView_last_message;
            private TextView textView_timestamp;
            private TextView textView_count;
            private CheckBox checkBox;
            private TextView textView_timestamp2;
            private TextView textView_user_count;

            private CustomViewHolder(View view) {
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

    private class getMessage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(final String... strings) {
            databaseReference.child("groupChat").child(strings[0]).child("comments").orderByChild("readUsers/" + uid).equalTo(false)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Map<String, Object> map = new HashMap<>();
                            if (dataSnapshot.getChildrenCount() > 0) {
                                for (DataSnapshot item : dataSnapshot.getChildren()) {
                                    map.put(Objects.requireNonNull(item.getKey()) + "/readUsers/" + uid, true);
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
                            for (DataSnapshot i : dataSnapshot.getChildren()) {
                                ChatModel.Comment c = i.getValue(ChatModel.Comment.class);
                                c.readUsers.put(uid, true);
                                c.setKey(i.getKey());
                                newComments.add(c);

                                if (c.getType().equals("img")) {
                                    Img img = new Img();
                                    if (users.get(c.getUid()) == null) {
                                        img.setName("(알수없음)");
                                    } else {
                                        img.setName(users.get(c.getUid()).getUserName());
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

}
