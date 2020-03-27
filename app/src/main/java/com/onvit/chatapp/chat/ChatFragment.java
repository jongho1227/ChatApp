package com.onvit.chatapp.chat;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.MainActivity;
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
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

public class ChatFragment extends Fragment {
    private final int firstReadChatCount = Utiles.firstReadChatCount;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM월dd일");
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm");
    private ChatRecyclerViewAdapter chatRecyclerViewAdapter;
    private AppCompatActivity activity;
    private Toolbar chatToolbar;
    private List<LastChat> chatModels = new ArrayList<>();
    private String uid;// 클라이언트uid
    private ToggleButton btn;
    private FloatingActionButton creatChat;
    private List<ChatModel.Comment> newComments = new ArrayList<>();
    private List<Img> img_list = new ArrayList<>();
    private AlertDialog dialog;
    private Map<String, User> users = new HashMap<>();
    private ValueEventListener valueEventListener;

    public ChatFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_chat, container, false);
        chatToolbar = view.findViewById(R.id.chat_toolbar);
        activity = (MainActivity) getActivity();
        activity.setSupportActionBar(chatToolbar);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setTitle("단체 채팅");
        btn = view.findViewById(R.id.vibrate_btn);
        creatChat = view.findViewById(R.id.plus_chat);
        users = UserMap.getInstance();
        creatChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SelectPeopleActivity.class);
                intent.putExtra("uid", uid);
                ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.fromleft, R.anim.toright);
                startActivity(intent, activityOptions.toBundle());
            }
        });
        UserMap.setComments(newComments);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btn.isChecked()) {
                    btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_notifications_vibrate));
                    activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).edit().putInt("vibrate", 0).apply();
                    Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(700);
                    Utiles.customToast(getActivity(), "앱의 알림이 설정되었습니다.").show();
                } else {
                    btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_notifications_no_vibrate));
                    activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).edit().putInt("vibrate", 1).apply();
                    Utiles.customToast(getActivity(), "앱의 알림이 해제되었습니다.").show();
                }
            }
        });
        if (activity.getSharedPreferences(activity.getPackageName(), Context.MODE_PRIVATE).getInt("vibrate", 0) == 0) {
            btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_notifications_vibrate));
            btn.setChecked(true);
        } else {
            btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_notifications_no_vibrate));
            btn.setChecked(false);
        }
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) { // 해당되는 chatrooms들의 키값들이 넘어옴.
                chatModels.clear(); // 채팅방에 표현할 리스트.
                for (final DataSnapshot item : dataSnapshot.getChildren()) {// normalChat, officerChat
                    final LastChat lastChat = item.getValue(LastChat.class);
                    chatModels.add(lastChat);// 채팅방 밖에 표시할 내용들.
                }
                Collections.sort(chatModels);
                chatRecyclerViewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("lastChat").orderByChild("existUsers/" + uid).equalTo(true).addValueEventListener(valueEventListener);

        chatRecyclerViewAdapter = new ChatRecyclerViewAdapter();
        RecyclerView recyclerView = view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setAdapter(chatRecyclerViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            databaseReference.child("lastChat").removeEventListener(valueEventListener); // 이벤트 제거.
        }
    }

    private void goChatRoom(String toRoom) {
        Intent  intent = new Intent(getActivity(), GroupMessageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("toRoom", toRoom); // 방이름
        intent.putExtra("chatCount", newComments.size());// 채팅숫자
        UserMap.setComments(newComments);
        intent.putParcelableArrayListExtra("imgList", (ArrayList<? extends Parcelable>) img_list);
        ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(getActivity(), R.anim.frombottom, R.anim.totop);
        startActivity(intent, activityOptions.toBundle());
        dialog.dismiss();
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatRecyclerViewAdapter.ChatViewHolder> {
        public ChatRecyclerViewAdapter() {

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

            holder.textView_title.setText(chatModels.get(position).getChatName());

            holder.textView_user_count.setText(chatModels.get(position).getExistUsers().size() + "");
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

                SimpleDateFormat sd = new SimpleDateFormat("yyyyMMddHHmm");
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
            if (chatModels.get(position).getUsers().get(uid) != 0) {
                holder.textView_count.setText(chatModels.get(position).getUsers().get(uid) + "");
                holder.textView_count.setVisibility(View.VISIBLE);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    //채팅방들어갈때 안읽은 메세지들 모두 읽음으로 처리해서 넘어감.
                    UserMap.clearComments();
                    img_list.clear();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    View noticeView = getLayoutInflater().from(getContext()).inflate(R.layout.access, null);
                    builder.setView(noticeView);
                    dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setCancelable(false);
                    dialog.show();
                    if (chatModels.get(position).getChatName().equals("회원채팅방")) {
                        chatModels.get(position).setChatName("normalChat");
                    } else if (chatModels.get(position).getChatName().equals("임원채팅방")) {
                        chatModels.get(position).setChatName("officerChat");
                    }
                    new getMessage().execute(chatModels.get(position).getChatName());
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