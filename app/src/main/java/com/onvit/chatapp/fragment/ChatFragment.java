package com.onvit.chatapp.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.onvit.chatapp.MainActivity;
import com.onvit.chatapp.PreferenceManager;
import com.onvit.chatapp.R;
import com.onvit.chatapp.chat.GroupMessageActivity;
import com.onvit.chatapp.model.LastChat;
import com.onvit.chatapp.model.ChatModel;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ChatFragment extends Fragment {
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM월dd일");
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm");
    private ChatRecyclerViewAdapter chatRecyclerViewAdapter = new ChatRecyclerViewAdapter();
    private AppCompatActivity activity;
    private Toolbar chatToolbar;
    private ValueEventListener valueEventListener;
    private List<LastChat> chatModels = new ArrayList<>();
    private List<String> keys = new ArrayList<>();
    private List<String> count = new ArrayList<>();
    private String uid;
    private List<String> userCount = new ArrayList<>();
    public ChatFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        chatToolbar = view.findViewById(R.id.chat_toolbar);
        activity = (MainActivity) getActivity();
        activity.setSupportActionBar(chatToolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setTitle("단체 채팅");

        RecyclerView recyclerView = view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setAdapter(chatRecyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        return view;
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatRecyclerViewAdapter.ChatViewHolder> {
        private ValueEventListener countEventListener;
        private Map<String, Object> countMap = new HashMap<>();
        public ChatRecyclerViewAdapter() {
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
                        userCount.add(lastChat.getExistUsers().size()+"");
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
            databaseReference.child("lastChat").orderByChild("existUsers/"+uid).equalTo(true).addValueEventListener(valueEventListener);

        }

        @NonNull
        @Override
        public ChatRecyclerViewAdapter.ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ChatRecyclerViewAdapter.ChatViewHolder holder, final int position) {
            //position0번 부터 붙음

            holder.textView_count.setVisibility(View.INVISIBLE);
            GradientDrawable gradientDrawable = (GradientDrawable) getContext().getDrawable(R.drawable.radius);
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
                    holder.textView_timestamp.setText(simpleDateFormat.format(date));
                    holder.textView_timestamp.setVisibility(View.VISIBLE);
                    holder.textView_timestamp2.setText(simpleDateFormat2.format(date));
                    holder.textView_timestamp2.setVisibility(View.VISIBLE);
                }

                //안읽은 메세지 숫자
                if (!count.get(position).equals("0") && !count.get(position).equals("null")) {
                    holder.textView_count.setText(count.get(position));
                    holder.textView_count.setVisibility(View.VISIBLE);
                }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                        //채팅방들어갈때 안읽은 메세지들 모두 읽음으로 처리해서 넘어감.
                        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        databaseReference.child("groupChat").child(keys.get(position)).child("comments").orderByChild("readUsers/" + uid).equalTo(false)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot == null) {
                                    return;
                                }
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
                                        intent = new Intent(view.getContext(), GroupMessageActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        intent.putExtra("toRoom", keys.get(position)); // 방이름
                                        intent.putExtra("chatCount", (Long) countMap.get(keys.get(position)));// 채팅숫자
                                        ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromright, R.anim.toleft);
                                        startActivity(intent, activityOptions.toBundle());
                                        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                    }
                                });

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                }
            });

        }

        @Override
        public int getItemCount() {
            return chatModels.size();
        }

        private class ChatViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;
            private TextView textView_title;
            private TextView textView_last_message;
            private TextView textView_timestamp;
            private TextView textView_timestamp2;
            private TextView textView_count;
            private TextView textView_user_count;
            private ChatViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.chatitem_imageview);
                textView_title = view.findViewById(R.id.chatitem_textview_title);
                textView_last_message = view.findViewById(R.id.chatitem_textview_lastMessage);
                textView_timestamp = view.findViewById(R.id.chatitem_textview_timestamp);
                textView_timestamp2 = view.findViewById(R.id.chatitem_textview_timestamp2);
                textView_count = view.findViewById(R.id.chatitem_textview_count);
                textView_user_count = view.findViewById(R.id.user_count);
            }
        }
    }


}