package com.onvit.chatapp.chat.vote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.model.Vote;
import com.onvit.chatapp.util.PreferenceManager;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class VoteActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private String toRoom, vote_key,uid;
    private SimpleDateFormat changeDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.KOREA);
    private String flag;
    private ImageView back;
    ArrayList<User> userList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote);
        databaseReference = FirebaseDatabase.getInstance().getReference();
        toRoom = getIntent().getStringExtra("room");
        vote_key = getIntent().getStringExtra("key");
        uid = PreferenceManager.getString(VoteActivity.this, "uid");
        flag = getIntent().getStringExtra("flag");
        back = findViewById(R.id.back_arrow);
        userList = getIntent().getParcelableArrayListExtra("userList");
        databaseReference.child("Vote").child(toRoom).child(vote_key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                Vote vote = dataSnapshot.getValue(Vote.class);
                TextView textView = findViewById(R.id.vote_title);
                TextView doVote = findViewById(R.id.do_vote);
                TextView deadline = findViewById(R.id.deadline);
                TextView detail = findViewById(R.id.detail);
                final Date d = new Date(vote.getDeadline());
                deadline.setText(changeDateFormat.format(d));
                textView.setText(vote.getTitle());
                LinearLayout vote_layout = findViewById(R.id.toggle_group);
                final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

                Map<String, Object> get = vote.getContent();
                TreeMap<String, Object> read = new TreeMap<>(get);
                Set<String> keys = read.keySet();
                Iterator<String> it = keys.iterator();

                final Map<String, Boolean> join = new HashMap<>();
                final Map<String, List<String>> detailUser = new HashMap<>();
                final Map<String, String> cUser = new HashMap<>();

                final Map<String, Object> update = new HashMap<>();

                while (it.hasNext()) {
                    int count = 0;
                    final String key = it.next();
                    Log.d("항목", key);
                    update.put(key+"/"+uid, false);
                    Map<String, Object> map2 = (Map<String, Object>) read.get(key);
                    Set<String> keys2 = map2.keySet();
                    Iterator<String> it2 = keys2.iterator();
                    List<String> user = new ArrayList<>();
                    while (it2.hasNext()) {
                        String key2 = it2.next();
                        cUser.put(key2,key2);
                        boolean b = (boolean) map2.get(key2);
                        if(b){
                            count++;
                            join.put(key2, true);
                            user.add(key2);
                        }
                    }
                    detailUser.put(key, user);
                    final ToggleButton btn = (ToggleButton) layoutInflater.inflate(R.layout.toggle_vote, vote_layout, false);
                    btn.setText(key.substring(1)+"("+count+"명)");
                    btn.setTextOff(key.substring(1)+"("+count+"명)");
                    btn.setTextOn(key.substring(1)+"("+count+"명)");
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(btn.isChecked()){
                                btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.toggle_border));
                                btn.setCompoundDrawablesWithIntrinsicBounds( 0, 0, R.drawable.ic_check_black_24dp, 0);
                                update.put(key+"/"+uid, true);
                            }else{
                                btn.setBackgroundDrawable(getResources().getDrawable(R.drawable.edit_border));
                                btn.setCompoundDrawablesWithIntrinsicBounds( 0, 0, 0, 0);
                                update.put(key+"/"+uid, false);
                            }
                        }
                    });
                    if(flag.equals("over")){
                        btn.setClickable(false);
                        btn.setFocusable(false);
                    }
                    vote_layout.addView(btn);
                }

                detail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("유저정보", detailUser.toString());
                        Intent intent = new Intent(VoteActivity.this, VoteDetailActivity.class);
                        Log.d("참여", join.toString());
                        intent.putExtra("join", (Serializable) join);
                        intent.putExtra("detail", (Serializable) detailUser);
                        intent.putExtra("cUser", (Serializable) cUser);
                        intent.putParcelableArrayListExtra("userList", userList);
                        intent.putExtra("room", toRoom);
                        startActivity(intent);
                    }
                });

                if(flag.equals("over")){
                    doVote.setText("투표결과");
                }else{
                    doVote.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(VoteActivity.this);
                            builder.setMessage("투표를 하시겠습니까?");
                            builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    databaseReference.child("Vote").child(toRoom).child(vote_key).child("content").updateChildren(update);
                                    Toast.makeText(VoteActivity.this, "투표를 하였습니다.", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }).setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromright, R.anim.toleft);//화면 사라지는 방향
    }
}
