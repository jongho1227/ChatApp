package com.onvit.chatapp.notice;

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.onvit.chatapp.R;
import com.onvit.chatapp.chat.BigPictureActivity;
import com.vlk.multimager.utils.Image;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NoticeActivity2 extends AppCompatActivity implements View.OnClickListener {
    String noticeName, code, uid, writer;
    ImageView profile;
    TextView title, name, time, delete, update;
    EditText content;
    RecyclerView recyclerView;
    NoticeActivityRecyclerAdapter2 noticeActivityRecyclerAdapter;
    ArrayList<String> imgPath = new ArrayList<>();
    DatabaseReference firebaseDatabase;
    ArrayList<String> list = new ArrayList<>();
    ArrayList<String> deleteKey = new ArrayList<>();
    private Toolbar chatToolbar;
    private ArrayList<String> registration_ids = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notice2);
        recyclerView = findViewById(R.id.notice_recyclerView);
        title = findViewById(R.id.title);
        profile = findViewById(R.id.profile_img);
        content = findViewById(R.id.edit_content);
        time = findViewById(R.id.time);
        name = findViewById(R.id.name);
        noticeName = "공지사항";
        title.setText(getIntent().getStringExtra("title"));
        content.setText(getIntent().getStringExtra("content"));
        content.setClickable(false);
        content.setFocusable(false);
        name.setText(getIntent().getStringExtra("name"));
        time.setText(getIntent().getStringExtra("time"));
        String pf = getIntent().getStringExtra("profile");
        code = getIntent().getStringExtra("code");
        registration_ids = getIntent().getStringArrayListExtra("userList");
        firebaseDatabase = FirebaseDatabase.getInstance().getReference();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        writer = getIntent().getStringExtra("writer");
        delete = findViewById(R.id.delete);
        update = findViewById(R.id.update);

        if (!pf.equals("noImg")) {
            Glide.with(this).load(pf).placeholder(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(profile);
            GradientDrawable gradientDrawable = (GradientDrawable) this.getDrawable(R.drawable.notice_profile_radius);
            profile.setBackground(gradientDrawable);
            profile.setClipToOutline(true);
        } else {
            Glide.with(this).load(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(profile);
            GradientDrawable gradientDrawable = (GradientDrawable) this.getDrawable(R.drawable.notice_profile_radius);
            profile.setBackground(gradientDrawable);
            profile.setClipToOutline(true);
        }

        list = getIntent().getStringArrayListExtra("img");
        deleteKey = getIntent().getStringArrayListExtra("deleteKey");
        Log.d("삭제", deleteKey.toString());
        if (list.size() > 0) {
            for (String s : list) {
                if (!s.equals("noImg")) {
                    imgPath.add(s);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        }
        chatToolbar = findViewById(R.id.notice_toolbar);
        chatToolbar.setBackgroundResource(R.color.notice);
        setSupportActionBar(chatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(noticeName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (uid.equals(writer)) {
            delete.setVisibility(View.VISIBLE);
            update.setVisibility(View.VISIBLE);
            delete.setOnClickListener(this);
            update.setOnClickListener(this);
        }


        noticeActivityRecyclerAdapter = new NoticeActivityRecyclerAdapter2();
        recyclerView.setLayoutManager(new LinearLayoutManager(NoticeActivity2.this, RecyclerView.HORIZONTAL, false));
        recyclerView.setAdapter(noticeActivityRecyclerAdapter);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromleft, R.anim.toright);//화면 사라지는 방향
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.update:
                Intent intent = new Intent(NoticeActivity2.this, NoticeActivity.class);
                intent.putExtra("modify", "modify");
                intent.putStringArrayListExtra("userList", registration_ids);
                intent.putExtra("title", title.getText().toString());
                intent.putExtra("content", content.getText().toString());
                intent.putExtra("code", code);
                ArrayList<Image> imgList = new ArrayList<>();
                ArrayList<String> list = new ArrayList<>();
                ArrayList<String> delete = new ArrayList<>();
                //등록된 사진이 있으면
                if (imgPath.size() > 0) {
                    for (int i = 0; i < imgPath.size(); i++) {
                        long id = Long.parseLong(deleteKey.get(i));
                        Uri uri = Uri.parse(imgPath.get(i));
                        String path = "path";
                        boolean b = true;
                        Image image = new Image(id, uri, path, b);
                        imgList.add(image);
                        list.add(imgPath.get(i));
                        delete.add(deleteKey.get(i));
                    }
                }
                intent.putStringArrayListExtra("img", list);
                intent.putStringArrayListExtra("deleteKey", delete);
                intent.putParcelableArrayListExtra("imgList", imgList);
                ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(this, R.anim.fromright, R.anim.toleft);
                startActivity(intent, activityOptions.toBundle());
                finish();
                break;
            case R.id.delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(NoticeActivity2.this);
                builder.setTitle("정말 삭제하시겠습니까?");
                builder.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Map<String, Object> map = new HashMap<>();
                        map.put(code, null);
                        if (imgPath.size() > 0) {
                            for (String key : deleteKey) {
                                FirebaseStorage.getInstance().getReference().child("Notice Img").child(code).child(key).delete();
                            }

                        }
                        firebaseDatabase.child("Notice").updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(NoticeActivity2.this, "삭제하였습니다.", Toast.LENGTH_SHORT).show();
                                onBackPressed();
                            }
                        });
                    }
                }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                builder.show();
                break;
        }
    }


    class NoticeActivityRecyclerAdapter2 extends RecyclerView.Adapter<NoticeActivity2.NoticeActivityRecyclerAdapter2.NoticeViewHolder2> {

        public NoticeActivityRecyclerAdapter2() {

        }

        @NonNull
        @Override
        public NoticeActivity2.NoticeActivityRecyclerAdapter2.NoticeViewHolder2 onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice_activity, parent, false);
            return new NoticeActivity2.NoticeActivityRecyclerAdapter2.NoticeViewHolder2(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final NoticeActivity2.NoticeActivityRecyclerAdapter2.NoticeViewHolder2 holder, final int position) {
            Log.d("이미지", position + "");

            holder.text.setText("클릭하여 확대");
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(NoticeActivity2.this, BigPictureActivity.class);
                    intent.putExtra("uri", imgPath.get(position));
                    intent.putExtra("position", position);
                    intent.putStringArrayListExtra("list", imgPath);
                    intent.putExtra("name", name.getText().toString());
                    startActivity(intent);
                }
            });

            Glide.with(NoticeActivity2.this).load(imgPath.get(position)).placeholder(R.drawable.ic_base_img_24dp).apply(new RequestOptions().centerCrop()).into(holder.imageView);
            GradientDrawable gradientDrawable = (GradientDrawable) NoticeActivity2.this.getDrawable(R.drawable.radius);
            holder.imageView.setBackground(gradientDrawable);
            holder.imageView.setClipToOutline(true);
        }

        @Override
        public int getItemCount() {
            return imgPath.size();
        }


        private class NoticeViewHolder2 extends RecyclerView.ViewHolder {
            private TextView text;
            private ImageView imageView;

            NoticeViewHolder2(View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.layout_img_text);
                imageView = itemView.findViewById(R.id.info_img);

            }
        }
    }
}
