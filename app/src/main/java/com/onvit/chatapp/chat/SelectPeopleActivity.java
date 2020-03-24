package com.onvit.chatapp.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.Img;
import com.onvit.chatapp.model.LastChat;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.UserMap;
import com.onvit.chatapp.util.Utiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectPeopleActivity extends AppCompatActivity {
    List<User> userlist;
    private Toolbar chatToolbar;
    private ValueEventListener valueEventListener;
    private List<User> selectUserList = new ArrayList<>();
    private Map<String, Object> messageReadUsers = new HashMap<>();
    private Map<String, Object> existUserGroupChat = new HashMap<>();
    private PeopleFragmentRecyclerAdapter pf = new PeopleFragmentRecyclerAdapter();
    private String uid;
    private List<User> pList = new ArrayList<>();
    private Button b;
    private EditText e;
    private DatabaseReference databaseReference;
    private Map<String, User> users = new HashMap<>();
    private String toRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_people);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatToolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("인원 선택");
        actionBar.setDisplayHomeAsUpEnabled(true);
        RecyclerView recyclerView = findViewById(R.id.peoplefragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(pf);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        b = findViewById(R.id.create_chat);
        e = findViewById(R.id.chat_name);
        users = UserMap.getInstance();
        if (getIntent().getStringExtra("plus") == null) {
            messageReadUsers.clear();
            existUserGroupChat.clear();
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String chatName = e.getText().toString().trim();
                    if (chatName.equals("")) {
                        Utiles.customToast(SelectPeopleActivity.this, "채팅방이름을 입력해주세요.").show();
                        return;
                    }
                    final AlertDialog d = Utiles.createLoadingDialog(SelectPeopleActivity.this, "채팅방을 생성하는 중입니다.");
                    databaseReference.child("groupChat").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            d.dismiss();
                            List<Integer> count = new ArrayList<>();
                            for (DataSnapshot i : dataSnapshot.getChildren()) {
                                if (chatName.equals(i.getKey())) {
                                    Utiles.customToast(SelectPeopleActivity.this, "중복된 채팅방이름이 존재합니다.").show();
                                    return;
                                }
                                ChatModel chatModel = i.getValue(ChatModel.class);
                                if (chatModel.id==0){
                                    continue;
                                }
                                count.add(chatModel.id);
                            }
                            Collections.sort(count);

                            int c = 3;
                            if(count.size()>0){
                                c = count.get(count.size()-1)+1;
                            }

                            ChatModel chatModel = new ChatModel();
                            LastChat lastChat = new LastChat();
                            lastChat.setChatName(chatName);
                            String message = "";
                            Map<String, Boolean> existUser = new HashMap<>();
                            Map<String, Integer> users = new HashMap<>();
                            for (User u : pList) {
                                chatModel.users.put(u.getUid(), false);
                                existUser.put(u.getUid(), true);
                                users.put(u.getUid(), 0);
                                message = message + String.format("%s(%s)님, ",u.getUserName(), u.getHospital());
                                messageReadUsers.put(u.getUid(), true);
                                existUserGroupChat.put(u.getUid(), true);
                            }
                            final String message2 = message.substring(0, message.length()-2)+"이 채팅방에 참여하였습니다.";
                            chatModel.id = c;
                            lastChat.setExistUsers(existUser);
                            lastChat.setUsers(users);

                            databaseReference.child("groupChat").child(chatName).setValue(chatModel);
                            databaseReference.child("lastChat").child(chatName).setValue(lastChat).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    List<ChatModel.Comment> newComments = new ArrayList<>();
                                    List<Img> img_list = new ArrayList<>();
                                    Utiles.customToast(SelectPeopleActivity.this, "채팅방을 생성하였습니다.").show();
                                    Intent intent = new Intent(SelectPeopleActivity.this, GroupMessageActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    intent.putExtra("toRoom", chatName); // 방이름
                                    intent.putExtra("chatCount", 0);// 채팅숫자
                                    UserMap.getComments().clear();
                                    intent.putParcelableArrayListExtra("imgList", (ArrayList<? extends Parcelable>) img_list);
                                    ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(SelectPeopleActivity.this, R.anim.frombottom, R.anim.totop);

                                    ChatModel.Comment comment = new ChatModel.Comment();
                                    comment.uid = uid;
                                    comment.message = message2;
                                    comment.timestamp = new Date().getTime();
                                    comment.type = "io";
                                    comment.readUsers = messageReadUsers;
                                    comment.existUser = existUserGroupChat;
                                    databaseReference.child("groupChat").child(chatName).child("comments").push().setValue(comment);
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
            });
        } else {
            userlist = getIntent().getParcelableArrayListExtra("userlist");
            messageReadUsers = (Map<String, Object>) getIntent().getSerializableExtra("readUser");

            for(User u : userlist){
                messageReadUsers.put(u.getUid(),true);
            }
            existUserGroupChat = (Map<String, Object>) getIntent().getSerializableExtra("existUser");
            toRoom = getIntent().getStringExtra("room");
            String chatName = getIntent().getStringExtra("room");
            if(chatName.equals("normalChat")){
                chatName = "회원채팅방";
            }else if(chatName.equals("officerChat")){
                chatName = "임원채팅방";
            }
            e.setText(chatName);
            e.setFocusable(false);
            e.setClickable(false);
            b.setText("초대하기");
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String message = "";
                    if(users.get(uid)!=null){
                        message = String.format("%s(%s)님이 ",users.get(uid).getUserName(),users.get(uid).getHospital());
                    }
                    Map<String, Object> map = new HashMap<>();
                    Map<String, Object> map2 = new HashMap<>();
                    for (User u : pList) {
                        map.put("users/" + u.getUid(), false);
                        map2.put("existUsers/" + u.getUid(), true);
                        map2.put("users/" + u.getUid(), 0);
                        if(u.getUid().equals(uid)){
                            continue;
                        }
                        messageReadUsers.put(u.getUid(),true);
                        existUserGroupChat.put(u.getUid(), true);
                        message = message + String.format("%s(%s)님, ",u.getUserName(), u.getHospital());
                    }
                    message = message.substring(0, message.length()-2)+"을 초대하였습니다.";
                    databaseReference.child("groupChat").child(getIntent().getStringExtra("room")).updateChildren(map);
                    ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = message;
                    comment.timestamp = new Date().getTime();
                    comment.type = "io";
                    comment.readUsers = messageReadUsers;
                    comment.existUser = existUserGroupChat;
                    databaseReference.child("groupChat").child(toRoom).child("comments").push().setValue(comment);
                    databaseReference.child("lastChat").child(getIntent().getStringExtra("room")).updateChildren(map2).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            finish();
                        }
                    });
                }
            });
        }
    }
    //뒤로가기 눌렀을때
    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromtop, R.anim.tobottom);//화면 사라지는 방향
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            FirebaseDatabase.getInstance().getReference().child("Users").removeEventListener(valueEventListener); // 이벤트 제거.
        }
    }

    class PeopleFragmentRecyclerAdapter extends RecyclerView.Adapter<PeopleFragmentRecyclerAdapter.CustomViewHolder> {

        public PeopleFragmentRecyclerAdapter() {
            valueEventListener = new ValueEventListener() { // Users데이터의 변화가 일어날때마다 콜백으로 호출됨.
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // 가입한 유저들의 정보를 가지고옴.
                    selectUserList.clear();
                    User user = null;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        user = snapshot.getValue(User.class);
                        if (uid.equals(user.getUid())) {
                            pList.add(user);
                            continue;
                        }

                        selectUserList.add(user);
                    }
                    if (getIntent().getStringExtra("plus") != null) {
                        for (User u : userlist) {
                            selectUserList.remove(u);
                        }
                    }
                    // 유저들의 정보를 가나순으로 정렬하고 자신의 정보는 첫번째에 넣음.
                    Collections.sort(selectUserList);
                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            FirebaseDatabase.getInstance().getReference().child("Users").addValueEventListener(valueEventListener);
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final CustomViewHolder holder, final int position) {
            Log.d("홀더붙는순서(연락처)", position + "");
            //position0번 부터 붙음
            holder.check.setVisibility(View.VISIBLE);
            holder.check.setChecked(false);

            if(pList.contains(selectUserList.get(position))){
                holder.check.setChecked(true);
            }

            holder.lineText.setVisibility(View.GONE);

            if (position == 1) {// 본인이랑 다음사람이랑 구분선.
                holder.lineText.setVisibility(View.VISIBLE);
            }
            //사진에 곡률넣음.
            if (selectUserList.get(position).getUserProfileImageUrl().equals("noImg")) {
                Glide.with(holder.itemView.getContext()).load(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(holder.imageView);
            } else {
                Glide.with(holder.itemView.getContext()).load(selectUserList.get(position).getUserProfileImageUrl()).placeholder(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(holder.imageView);
            }
            GradientDrawable gradientDrawable = (GradientDrawable) SelectPeopleActivity.this.getDrawable(R.drawable.radius);
            holder.imageView.setBackground(gradientDrawable);
            holder.imageView.setClipToOutline(true);

            holder.textView.setText(selectUserList.get(position).getUserName());

            holder.textView_hospital.setText("[" + selectUserList.get(position).getHospital() + "]");

            holder.check.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (holder.check.isChecked()) {
                        pList.add(selectUserList.get(position));
                    } else {
                        pList.remove(selectUserList.get(position));
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return selectUserList.size();
        }


        private class CustomViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;
            private TextView textView;
            private TextView textView_hospital;
            private TextView lineText;
            private CheckBox check;

            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.frienditem_imageview);
                textView = view.findViewById(R.id.frienditem_textview);
                textView_hospital = view.findViewById(R.id.frienditem_textview_hospital);
                lineText = view.findViewById(R.id.line_text);
                check = view.findViewById(R.id.check);
            }
        }
    }
}
