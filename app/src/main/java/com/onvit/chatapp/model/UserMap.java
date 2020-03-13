package com.onvit.chatapp.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class UserMap {
    private static HashMap<String, User> userMap;
    private static List<User> userList;

    public static HashMap<String, User> getInstance(){
        if(userMap==null){
            userMap = new HashMap<>();
        }
        return userMap;
    }

    public static List<User> getUser(){
        if(userList==null){
            userList = new ArrayList<>();
        }
        return userList;
    }

    public static void getUserMap(){
        FirebaseDatabase.getInstance().getReference().child("Users").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                User user = dataSnapshot.getValue(User.class);
                userMap.put(dataSnapshot.getKey(), user);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                User user = dataSnapshot.getValue(User.class);
                userMap.put(dataSnapshot.getKey(), user);

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public static void getUserList(){
        FirebaseDatabase.getInstance().getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 가입한 유저들의 정보를 가지고옴.
                userList.clear();
                User user = null;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(snapshot.getValue(User.class).getUid())) {
                        user = snapshot.getValue(User.class);
                        continue;
                    }
                    userList.add(snapshot.getValue(User.class));
                }
                // 유저들의 정보를 가나순으로 정렬하고 자신의 정보는 첫번째에 넣음.
                Collections.sort(userList);
                userList.add(0, user);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
