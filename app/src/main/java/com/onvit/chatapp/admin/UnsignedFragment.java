package com.onvit.chatapp.admin;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.User;

import java.util.ArrayList;

public class UnsignedFragment extends Fragment {
    ArrayList<User> user;
    Button button, tButton;
    ArrayList<String> phone = new ArrayList<>();
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    public UnsignedFragment() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sign_fragment, container, false);
        user = getArguments().getParcelableArrayList("unSign");
        RecyclerView recyclerView = view.findViewById(R.id.peoplefragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        UnSignFragmentRecyclerAdapter pf = new UnSignFragmentRecyclerAdapter();
        pf.notifyDataSetChanged();
        recyclerView.setAdapter(pf);

        for (User u : user) {
            phone.add(u.getTel());
        }

        button = view.findViewById(R.id.push);
        tButton = view.findViewById(R.id.test_push);
        tButton.setVisibility(View.VISIBLE);
        button.setVisibility(View.VISIBLE);

//        tButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Dexter.withActivity(getActivity()).withPermission(Manifest.permission.SEND_SMS)
//                        .withListener(new PermissionListener() {
//                            @Override
//                            public void onPermissionGranted(PermissionGrantedResponse response) {
//                                SmsManager smsManager = SmsManager.getDefault();
//                                smsManager.sendTextMessage("01044155014",null,"테스트문자전송",null,null);
//                                Toast.makeText(getContext(), "전송완료", Toast.LENGTH_SHORT).show();
//                            }
//
//                            @Override
//                            public void onPermissionDenied(PermissionDeniedResponse response) {
//
//                            }
//
//                            @Override
//                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
//
//                            }
//                        }).check();
//            }
//        });
//
//
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//                View v = getLayoutInflater().from(getContext()).inflate(R.layout.invite_message, null);
//                final EditText e = v.findViewById(R.id.invite);
//                final EditText google = v.findViewById(R.id.google);
//                final EditText apple = v.findViewById(R.id.apple);
//                final Button send = v.findViewById(R.id.send);
//                final Button cancel = v.findViewById(R.id.cancel);
//                e.setText("대한지역병원협의회 앱이 출시되었습니다.\n" +
//                        "링크를 통해 가입해주세요.\n" +
//                        "감사합니다.");
//                google.setText(mFirebaseRemoteConfig.getString("google_store"));
//                apple.setText(mFirebaseRemoteConfig.getString("apple_store"));
//                builder.setView(v);
//                final AlertDialog dialog = builder.create();
//                dialog.show();
//                send.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        Dexter.withActivity(getActivity()).withPermission(Manifest.permission.SEND_SMS)
//                                .withListener(new PermissionListener() {
//                                    @Override
//                                    public void onPermissionGranted(PermissionGrantedResponse response) {
//                                        String s = e.getText().toString();
//                                        String g = google.getText().toString().trim();
//                                        String a = apple.getText().toString().trim();
//                                        int d = byteCheck(s, 80);
//                                        Log.d("글자제한", d + "");
//                                        if (d > 80) {
//                                            Toast.makeText(getContext(), "글자수 초과 현재:" + d + " 제한:80", Toast.LENGTH_SHORT).show();
//                                            return;
//                                        } else {
//                                            for (String phone : phone) {
//                                                SmsManager smsManager = SmsManager.getDefault();
//                                                if (!s.equals("")) {
//                                                    smsManager.sendTextMessage(phone, null, s, null, null);
//                                                    if (!g.equals("")) {
//                                                        smsManager.sendTextMessage(phone, null, "구글링크\n" + g, null, null);
//                                                    }
//                                                    if (!a.equals("")) {
//                                                        smsManager.sendTextMessage(phone, null, "애플링크\n" + a, null, null);
//                                                    }
//                                                }
//                                            }
//                                            Toast.makeText(getContext(), "전송완료", Toast.LENGTH_SHORT).show();
//                                            dialog.dismiss();
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onPermissionDenied(PermissionDeniedResponse response) {
//
//                                    }
//
//                                    @Override
//                                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
//
//                                    }
//                                }).check();
//                    }
//                });
//                cancel.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        dialog.dismiss();
//                    }
//                });
//            }
//        });

        return view;
    }


    public int byteCheck(String txt, int standardByte) {
        if (TextUtils.isEmpty(txt)) {
            return -1;
        }

        // 바이트 체크 (영문 1, 한글 2, 특문 1)
        int en = 0;
        int ko = 0;
        int etc = 0;

        char[] txtChar = txt.toCharArray();
        for (int j = 0; j < txtChar.length; j++) {
            if (txtChar[j] >= 'A' && txtChar[j] <= 'z') {
                en++;
            } else if (txtChar[j] >= '\uAC00' && txtChar[j] <= '\uD7A3') {
                ko++;
                ko++;
            } else {
                etc++;
            }
        }

        int txtByte = en + ko + etc;
        return txtByte;
    }

    class UnSignFragmentRecyclerAdapter extends RecyclerView.Adapter<UnSignFragmentRecyclerAdapter.CustomViewHolder> {

        public UnSignFragmentRecyclerAdapter() {

        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sign, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final CustomViewHolder holder, final int position) {
            holder.lineText.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.GONE);

            holder.textView.setText(user.get(position).getUserName());

            holder.textView_hospital.setText("[" + user.get(position).getHospital() + "]");


        }

        @Override
        public int getItemCount() {
            return user.size();
        }


        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public TextView textView_hospital;
            public TextView lineText;

            public CustomViewHolder(View view) {
                super(view);
                textView = view.findViewById(R.id.frienditem_textview);
                textView_hospital = view.findViewById(R.id.frienditem_textview_hospital);
                lineText = view.findViewById(R.id.line_text);
                imageView = view.findViewById(R.id.frienditem_imageview);

            }
        }
    }

}