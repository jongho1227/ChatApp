package com.onvit.chatapp.chat.vote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VoteDetailActivity extends AppCompatActivity {
    ViewPager viewPager;
    TabLayout tabLayout;
    Map<String, Boolean> join = new HashMap<>();
    Map<String, List<String>> detailUser = new HashMap<>();
    String toRoom;
    List<String> joinUser;
    List<User> joinUserList = new ArrayList<>();
    List<User> notJoinUserList = new ArrayList<>();
    List<User> allUserList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote_detail);
        join = (Map<String, Boolean>) getIntent().getSerializableExtra("join");
        detailUser = (Map<String, List<String>>) getIntent().getSerializableExtra("detail");
        toRoom = getIntent().getStringExtra("room");
        joinUser = new ArrayList<>();

        Log.d("참여", join.toString());

        Set<String> keys = join.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()){
            String key = it.next();
            joinUser.add(key);
        }

        FirebaseDatabase.getInstance().getReference().child("groupChat").child(toRoom).child("userInfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot i : dataSnapshot.getChildren()){
                    User user = i.getValue(User.class);
                    allUserList.add(user);
                    if(joinUser.size()==0){
                        notJoinUserList.add(user);
                    }else{
                        for(int j=0; j<joinUser.size(); j++){
                            String uid = joinUser.get(j);
                            if(user.getUid().equals(uid)){
                                joinUserList.add(user);
                                break;
                            }
                            if(j==joinUser.size()-1){
                                notJoinUserList.add(user);
                                break;
                            }
                        }
                    }

                }
                Map<String, List<User>> detailUserMap = new HashMap<>();
                Set<String> key1 = detailUser.keySet();
                Iterator<String> its = key1.iterator();
                while (its.hasNext()){
                    List<User> l = new ArrayList<>();
                    String key = its.next();
                    List<String> list = detailUser.get(key);

                    for(String a : list){
                        for(User u : allUserList){
                            if(u.getUid().equals(a)){
                                l.add(u);
                            }
                        }
                    }
                    detailUserMap.put(key, l);
                }
                Log.d("참여", joinUserList.toString());
                Log.d("참여", notJoinUserList.toString());
                VotePageAdapter votePageAdapter = new VotePageAdapter(getSupportFragmentManager(),2,joinUserList, notJoinUserList, detailUserMap);
                viewPager = findViewById(R.id.view_pager);
                tabLayout = findViewById(R.id.tabLayout);
                votePageAdapter.notifyDataSetChanged();
                viewPager.setAdapter(votePageAdapter);
                tabLayout.setupWithViewPager(viewPager);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
