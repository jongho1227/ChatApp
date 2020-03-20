package com.onvit.chatapp.notice;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
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
import com.onvit.chatapp.model.Notice;
import com.onvit.chatapp.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class NoticeFragment extends Fragment {
    private AppCompatActivity activity;
    private Toolbar chatToolbar;
    private List<Notice> noticeLists = new ArrayList<>();
    private DatabaseReference firebaseDatabase;
    private String uid;
    private NoticeFragmentRecyclerAdapter noticeFragmentRecyclerAdapter;
    private RecyclerView recyclerView;
    private ArrayList<String> registration_ids = new ArrayList<>();
    private ValueEventListener valueEventListener;


    public NoticeFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notice, container, false);
        chatToolbar = view.findViewById(R.id.chat_toolbar);
        activity = (MainActivity) getActivity();
        activity.setSupportActionBar(chatToolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d("내uid", uid);


        recyclerView = view.findViewById(R.id.fragment_notice_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        noticeFragmentRecyclerAdapter = new NoticeFragmentRecyclerAdapter(noticeLists);
        recyclerView.setAdapter(noticeFragmentRecyclerAdapter);

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                noticeLists.clear();
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    Notice noticeList = item.getValue(Notice.class);
                    noticeList.setCode(item.getKey());
                    noticeLists.add(noticeList);
                }
                Collections.reverse(noticeLists);
                noticeFragmentRecyclerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        firebaseDatabase.child("Notice").addValueEventListener(valueEventListener);
        firebaseDatabase.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                registration_ids.clear();
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    User user = item.getValue(User.class);
                    if (user.getUid().equals(uid)) {
                        Log.d("내uid", "dd");
                        continue;
                    }
                    if(user.getPushToken().equals("")){
                        continue;
                    }
                    registration_ids.add(user.getPushToken());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        actionBar.setTitle("공지사항");

        FloatingActionButton make = view.findViewById(R.id.plus_notice);
        make.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), NoticeActivity.class);
                intent.putExtra("insert", "insert");
                intent.putStringArrayListExtra("userList", registration_ids);
                for(String d : registration_ids){
                    Log.d("내uid", d);
                }
                ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.frombottom, R.anim.totop);
                startActivity(intent, activityOptions.toBundle());
            }
        });

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        NotificationManagerCompat.from(activity).cancel("notice", 0);
        NotificationManagerCompat.from(activity).cancel(2);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(valueEventListener!=null){
            firebaseDatabase.child("Notice").removeEventListener(valueEventListener);
        }
    }

    class NoticeFragmentRecyclerAdapter extends RecyclerView.Adapter<NoticeFragmentRecyclerAdapter.NoticeViewHolder> {
        List<Notice> noticeList;

        public NoticeFragmentRecyclerAdapter(List<Notice> list) {
            noticeList = list;
        }

        @NonNull
        @Override
        public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
            return new NoticeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final NoticeViewHolder holder, final int position) {
            holder.time.setVisibility(View.VISIBLE);
            holder.newicon.setVisibility(View.VISIBLE);
            holder.name.setText(noticeList.get(position).getName());
            holder.name.setTextColor(Color.GRAY);
            holder.title.setText(noticeList.get(position).getTitle());
            if (uid.equals(noticeList.get(position).getUid())) {
                holder.name.setTextColor(Color.BLUE);
            }

            long nTime = (long) (noticeList.get(position).getTimestamp());
            long cTime = new Date().getTime();

            int nSecond = (int) (nTime / 1000);
            int cSeconde = (int) (cTime / 1000);

            int diffSecond = cSeconde - nSecond;

            if (diffSecond < 360) {
                holder.time.setText("방금 전");
            } else if (diffSecond < 3600) {
                holder.time.setText(diffSecond / 60 + "분 전");
            } else if (diffSecond < 86400) {
                holder.time.setText(diffSecond / 3600 + "시간 전");
            } else if (diffSecond < 259200) {
                holder.time.setText(diffSecond / 86400 + "일 전");
            } else if (diffSecond < 604800) {
                holder.time.setText(diffSecond / 86400 + "일 전");
                holder.newicon.setVisibility(View.INVISIBLE);
            } else {
                holder.time.setText(noticeList.get(position).getTime());
                holder.newicon.setVisibility(View.INVISIBLE);
            }
            holder.layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    View noticeView = getLayoutInflater().from(getContext()).inflate(R.layout.info_notice, null);
                    builder.setView(noticeView);
                    final AlertDialog dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setCancelable(false);
                    dialog.show();

                    firebaseDatabase.child("Users").orderByChild("uid").equalTo(noticeList.get(position).getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getChildrenCount() == 0) {
                                Intent intent = new Intent(getActivity(), NoticeActivity2.class);
                                intent.putExtra("profile", "noImg");
                                startIntent(intent, view, dialog, position);
                            } else {
                                for (DataSnapshot item : dataSnapshot.getChildren()) {
                                    User user = item.getValue(User.class);
                                    Intent intent = new Intent(getActivity(), NoticeActivity2.class);
                                    if (user.getUserProfileImageUrl() != null) {
                                        intent.putExtra("profile", user.getUserProfileImageUrl());
                                    } else {
                                        intent.putExtra("profile", "noImg");
                                    }
                                    startIntent(intent, view, dialog, position);
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                private void startIntent(Intent intent, View view, AlertDialog dialog, int position) {
                    intent.putExtra("title", noticeList.get(position).getTitle());
                    intent.putExtra("content", noticeList.get(position).getContent());
                    intent.putExtra("time", noticeList.get(position).getTime());
                    intent.putExtra("name", noticeList.get(position).getName());
                    intent.putExtra("code", noticeList.get(position).getCode());
                    intent.putExtra("writer", noticeList.get(position).getUid());
                    intent.putStringArrayListExtra("userList", registration_ids);
                    if (noticeList.get(position).getImg() != null) {
                        ArrayList<String> list = new ArrayList<>();
                        ArrayList<String> deletekey = new ArrayList<>();
                        Map<String, String> hashMap = noticeList.get(position).getImg();

                        TreeMap<String, String> treeMap = new TreeMap<>(hashMap);
                        Set<String> keys = treeMap.keySet();
                        for (String key1 : keys) {
                            if (key1.equals("noImg")) {
                                break;
                            } else {
                                long id = Long.parseLong(key1);
                                Object value = noticeList.get(position).getImg().get(key1);
                                list.add(value.toString());
                                deletekey.add(id + "");
                            }
                        }
                        intent.putStringArrayListExtra("img", list);
                        intent.putStringArrayListExtra("deleteKey", deletekey);
                    }
                    ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(view.getContext(), R.anim.frombottom, R.anim.totop);
                    dialog.dismiss();
                    startActivity(intent, activityOptions.toBundle());
                }
            });
        }

        @Override
        public int getItemCount() {
            return noticeList.size();
        }


        private class NoticeViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView name;
            TextView time;
            ImageView newicon;
            ImageView notice;
            LinearLayout layout;

            public NoticeViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.notice_title);
                time = itemView.findViewById(R.id.notice_time);
                name = itemView.findViewById(R.id.notice_name);
                newicon = itemView.findViewById(R.id.new_icon);
                notice = itemView.findViewById(R.id.notice);
                layout = itemView.findViewById(R.id.layout_title);
            }
        }
    }
}
