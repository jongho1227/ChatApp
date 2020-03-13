package com.onvit.chatapp.admin;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.KCHA;
import com.onvit.chatapp.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InviteActivity extends AppCompatActivity {
    ViewPager viewPager;
    TabLayout tabLayout;
    SignPageAdapter signPageAdapter;
    List<User> signUser = new ArrayList<>();
    List<User> unSignUser = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        FirebaseDatabase.getInstance().getReference().child("KCHA").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    KCHA kcha = item.getValue(KCHA.class);
                    User user = new User();
                    user.setUserName(kcha.getName());
                    user.setTel(kcha.getPhone().replaceAll("-", ""));
                    user.setHospital(kcha.getHospital());
                    unSignUser.add(user);
                }
                Log.d("전체가입자", unSignUser.size()+"");
                FirebaseDatabase.getInstance().getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot item : dataSnapshot.getChildren()) {
                            User u = item.getValue(User.class);
                            u.setUserName(u.getUserName().trim());
                            unSignUser.remove(u);
                            signUser.add(u);
                        }
                        Collections.sort(signUser);
                        Collections.sort(unSignUser);
                        Log.d("전체가입자", signUser.size()+"");
                        Log.d("전체가입자", unSignUser.size()+"");
                        signPageAdapter = new SignPageAdapter(getSupportFragmentManager(), 2, signUser, unSignUser);
                        viewPager = findViewById(R.id.view_pager);
                        tabLayout = findViewById(R.id.tabLayout);
                        signPageAdapter.notifyDataSetChanged();
                        viewPager.setAdapter(signPageAdapter);
                        tabLayout.setupWithViewPager(viewPager);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}