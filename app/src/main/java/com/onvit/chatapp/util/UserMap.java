package com.onvit.chatapp.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class UserMap {
    private static HashMap<String, User> userMap;
    private static List<User> userList;
    private static List<ChatModel.Comment> newComments;
    private static ValueEventListener valueEventListener;
    private static ChildEventListener childEventListener;

    public static void clearComments() {
        if (newComments != null) {
            newComments.clear();
        }
    }

    public static List<ChatModel.Comment> getComments() {
        return newComments;
    }

    public static void setComments(List<ChatModel.Comment> comments) {
        newComments = comments;
    }

    public static HashMap<String, User> getInstance() {
        if (userMap == null) {
            userMap = new HashMap<>();
        }
        return userMap;
    }

    public static List<User> getUser() {
        if (userList == null) {
            userList = new ArrayList<>();
        }
        return userList;
    }

    public static void getUserMap() {
        childEventListener = new ChildEventListener() {
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
        };
        FirebaseDatabase.getInstance().getReference().child("Users").addChildEventListener(childEventListener);
    }

    public static void getUserList() {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // 가입한 유저들의 정보를 가지고옴
                if(userList!=null){
                    userList.clear();
                }
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
        };
        FirebaseDatabase.getInstance().getReference().child("Users").addValueEventListener(valueEventListener);
    }

    public static void clearApp(){
        if(valueEventListener!=null){
            FirebaseDatabase.getInstance().getReference().child("Users").removeEventListener(valueEventListener);
        }
        if(childEventListener!=null){
            FirebaseDatabase.getInstance().getReference().child("Users").removeEventListener(childEventListener);
        }
        if(userList!=null){
            userList.clear();
        }
        if(userMap!=null){
            userMap.clear();
        }
        if(newComments!=null){
            newComments.clear();
        }
    }
}
