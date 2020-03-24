package com.onvit.chatapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.onvit.chatapp.certification.CertificateActivity;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.PreferenceManager;
import com.onvit.chatapp.util.Utiles;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {
    private final static int PERMISSION_REQUEST_CODE = 1000;
    private EditText id;
    private EditText password;
    private Button login;
    private Button signup;
    private Button search;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        String logOut = getIntent().getStringExtra("logOut");
        if (logOut != null && logOut.equals("logOut")) {
            firebaseAuth.signOut();
        }

        id = findViewById(R.id.loginactivity_edittext_id);
        password = findViewById(R.id.loginactivity_edittext_password);
        login = findViewById(R.id.loginactivity_button_login);
        signup = findViewById(R.id.loginactivity_button_signup);
        search = findViewById(R.id.loginactivity_button_search);
        requestPermission();
        password.setImeOptions(EditorInfo.IME_ACTION_DONE);
        password.setOnEditorActionListener(new TextView.OnEditorActionListener() { // 완료눌러도 회원가입기능되게~
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    loginEvent();
                }
                return false;
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginEvent();
            }
        });

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, CertificateActivity.class));
                finish();
            }
        });
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, CertificateActivity.class);
                intent.putExtra("search", "search");
                startActivity(intent);
            }
        });

        //로그인 인터페이스 리스너
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //로그인
                    FirebaseDatabase.getInstance().getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            // 가입한 유저들의 정보를 가지고옴.
                            User user = null;
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(snapshot.getValue(User.class).getUid())) {
                                    user = snapshot.getValue(User.class);
                                    PreferenceManager.setString(LoginActivity.this, "name", user.getUserName());
                                    PreferenceManager.setString(LoginActivity.this, "hospital", user.getHospital());
                                    PreferenceManager.setString(LoginActivity.this, "phone", user.getTel());
                                    PreferenceManager.setString(LoginActivity.this, "uid", user.getUid());
//                                    PreferenceManager.setString(LoginActivity.this, "grade", user.getGrade());
                                }
                            }
                            if (user != null) {
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("user", user);
                                intent.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                dialog.dismiss();
                                finish();
                            } else {
                                Utiles.customToast(LoginActivity.this, "회원정보를 찾을 수 없습니다.").show();
                                dialog.dismiss();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                } else {
                }
            }
        };
    }
    void loginEvent() {
        if (id.getText().toString() == null || id.getText().toString().equals("") || password.getText().toString() == null || password.getText().toString().equals("")) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setView(R.layout.login);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
        firebaseAuth.signInWithEmailAndPassword(id.getText().toString(), password.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            //로그인 실패한부분
                            Utiles.customToast(LoginActivity.this, "이메일과 비밀번호를 정확하게 입력하세요.").show();
                            dialog.dismiss();
                        } else {
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }

    }

    private void requestPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        ArrayList<String> arrayPermission = new ArrayList<>();

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            arrayPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            arrayPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (arrayPermission.size() > 0) {
            String[] strArray = new String[arrayPermission.size()];
            strArray = arrayPermission.toArray(strArray);
            ActivityCompat.requestPermissions(this, strArray, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length < 1) {
                    Utiles.customToast(LoginActivity.this, "권한을 받아오는데 실패하였습니다.").show();
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                    return;
                }
                for (int i = 0; i < grantResults.length; i++) {
                    String permission = permissions[i];
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
                        if (!showRationale) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                            builder.setMessage("앱의 원활한 사용을 위해 권한을 허용해야 합니다. 앱 정보로 이동합니다.\n [저장공간]권한을 허용해주세요.");
                            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                            builder.setMessage("앱의 원활한 사용을 위해 권한을 허용해야 합니다.");
                            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    requestPermission();

                                }
                            });
                            final AlertDialog dialog = builder.create();
                            dialog.setCancelable(false);
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                        }
                    } else {
                        Utiles.customToast(LoginActivity.this, "권한을 허용하였습니다.").show();
                        // Initialize 코드
                    }
                }
            }
            break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
