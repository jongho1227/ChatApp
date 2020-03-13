package com.onvit.chatapp.chat.vote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

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
    private Toolbar toolbar;
    Map<String, Boolean> join = new HashMap<>();
    Map<String, List<String>> detailUser = new HashMap<>();
    Map<String, String> cUser = new HashMap<>();
    String toRoom;
    List<String> joinUser;
    List<User> joinUserList = new ArrayList<>();
    List<User> notJoinUserList = new ArrayList<>();
    List<User> allUserList = new ArrayList<>();
    ArrayList<User> userList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote_detail);
        join = (Map<String, Boolean>) getIntent().getSerializableExtra("join");
        detailUser = (Map<String, List<String>>) getIntent().getSerializableExtra("detail");
        cUser = (Map<String,String>) getIntent().getSerializableExtra("cUser");
        userList = getIntent().getParcelableArrayListExtra("userList");
        toRoom = getIntent().getStringExtra("room");
        joinUser = new ArrayList<>();

        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundResource(R.color.notice);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("투표상세보기");
        actionBar.setDisplayHomeAsUpEnabled(true);

        Set<String> keys = join.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()){
            String key = it.next();
            joinUser.add(key);
        }

        for(User user : userList){
            if(cUser.get(user.getUid())==null){
                continue;
            }

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
        VotePageAdapter votePageAdapter = new VotePageAdapter(getSupportFragmentManager(),2,joinUserList, notJoinUserList, detailUserMap);
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tabLayout);
        votePageAdapter.notifyDataSetChanged();
        viewPager.setAdapter(votePageAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    //뒤로가기 눌렀을때
    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromright, R.anim.toleft);//화면 사라지는 방향
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
