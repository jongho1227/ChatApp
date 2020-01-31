package com.onvit.chatapp.certification;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.AdminActivity;
import com.onvit.chatapp.LoginActivity;
import com.onvit.chatapp.R;
import com.onvit.chatapp.SignUpActivity;
import com.onvit.chatapp.SplashActivity;
import com.onvit.chatapp.model.KCHA;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.notice.NoticeActivity;

import org.json.JSONException;

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateActivity extends AppCompatActivity {


    private WebView webView;
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate);

        webView = findViewById(R.id.certificate_webview);
//        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webView.clearCache(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(mObject, "jsName");
        webView.setWebViewClient(mWebViewClient);
        webView.setWebChromeClient(mWebChromeClient);
        webView.getSettings().setDomStorageEnabled(true);
        webView.loadUrl(getString(R.string.selfcerti_url));
    }

    Object mObject = new Object() {

        @android.webkit.JavascriptInterface
        public void getResult(final String name, final String phone, final String birth, final String gender, final String nation) throws JSONException {
            if(getIntent().getStringExtra("search")==null){
                final User user = new User();
                final String newPhone = phone.substring(0,3)+"-"+phone.substring(3,7)+"-"+phone.substring(7);
//            인증된사람만넘어감.
                FirebaseDatabase.getInstance().getReference().child("KCHA").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot item : dataSnapshot.getChildren()){
                            KCHA kcha = item.getValue(KCHA.class);
                            if(kcha.getPhone().equals(newPhone)){
                                user.setUserName(kcha.getName());
                                user.setHospital(kcha.getHospital());
                                user.setUserEmail(kcha.getEmail());
                                user.setTel(phone);
                                if(kcha.getGrade().equals("1")){
                                    user.setGrade("임원");
                                }else{
                                    user.setGrade("회원");
                                }

                            }
                        }
                        if(user.getUserName()==null || user.getUserName().equals("")){
                            AlertDialog.Builder builder = new AlertDialog.Builder(CertificateActivity.this);
                            builder.setMessage("협의회 회원만 가입 가능합니다.");
                            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(CertificateActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                            builder.setCancelable(false);
                            builder.create().show();
                        }else{
                            Intent intent = new Intent(CertificateActivity.this, SignUpActivity.class);
                            intent.putExtra("user", user);
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }else{
                AlertDialog.Builder b = new AlertDialog.Builder(CertificateActivity.this);
                View noticeView = getLayoutInflater().from(CertificateActivity.this).inflate(R.layout.search,null);
                b.setView(noticeView);
                final AlertDialog d = b.create();
                d.setCanceledOnTouchOutside(false);
                d.setCancelable(false);
                d.show();
                FirebaseDatabase.getInstance().getReference().child("Users").orderByChild("tel").equalTo(phone).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getChildrenCount()==0){
                            d.dismiss();
                            AlertDialog.Builder builder = new AlertDialog.Builder(CertificateActivity.this);
                            builder.setMessage("해당 번호로 가입된 정보가 없습니다.");
                            builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.setCancelable(false);
                            dialog.show();
                        }else{
                            for(DataSnapshot item : dataSnapshot.getChildren()){
                                final User user = item.getValue(User.class);
                                if(user!=null){
                                    if(user.getTel().equals(phone)){
                                        FirebaseAuth auth = FirebaseAuth.getInstance();
                                        String emailAddress = user.getUserEmail();
                                        auth.sendPasswordResetEmail(emailAddress).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                d.dismiss();
                                                AlertDialog.Builder builder = new AlertDialog.Builder(CertificateActivity.this);
                                                builder.setTitle("메일 발송");
                                                builder.setMessage("가입하신 이메일("+user.getUserEmail()+")로 메일을 발송하였습니다.\n 비밀번호를 재설정 후 이용해주세요.");
                                                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        finish();
                                                    }
                                                });
                                                AlertDialog dialog = builder.create();
                                                dialog.setCanceledOnTouchOutside(false);
                                                dialog.setCancelable(false);
                                                dialog.show();
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }
    };

    WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            view.loadUrl("about:blank");
            AlertDialog.Builder builder = new AlertDialog.Builder(CertificateActivity.this);
            builder.setTitle("오류 발생");
            if (errorCode == ERROR_HOST_LOOKUP) {
                builder.setMessage("페이지를 불러오는데 실패했습니다.\n네트워크 상태를 확인해주세요");
            } else {
                builder.setMessage("죄송합니다\n페이지를 불러오는데 실패했습니다.\n진행하시려면 다시 시작해주세요");
            }
            builder.setPositiveButton("재시작", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(CertificateActivity.this, SplashActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
            builder.setNegativeButton("종료", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            builder.setCancelable(false);
            alertDialog = builder.create();
            alertDialog.show();
        }


        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

            X509Certificate cert = null;
            try {
                InputStream caInput_rosemary = getResources().openRawResource(R.raw.samsun);
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                cert = (X509Certificate) certificateFactory.generateCertificate(caInput_rosemary);
            } catch (CertificateException e) {
                e.printStackTrace();
            }

            String sslCertificate = error.getCertificate().toString();
            String mySslCertificate = new SslCertificate(cert).toString();

            if (sslCertificate.equals(mySslCertificate)) {
                handler.proceed();
                return;
            }

            StringBuilder sb = new StringBuilder();
            switch (error.getPrimaryError()) {
                case SslError.SSL_EXPIRED:
                    sb.append("이 사이트의  보안 인증서가 만료되었습니다.\n");
                    break;
                case SslError.SSL_IDMISMATCH:
                    sb.append("이 사이트의 보안 인증서 ID가 일치하지 않습니다.\n");
                    break;
                case SslError.SSL_NOTYETVALID:
                    sb.append("이 사이트의 보안 인증서가 아직 유효하지 않습니다.\n");
                    break;
                case SslError.SSL_UNTRUSTED:
                    sb.append("이 사이트의 보안 인증서는 신뢰할 수 없습니다.\n");
                    break;
                default:
                    sb.append("보안 인증서에 오류가 있습니다.\n");
                    break;
            }

            sb.append("계속 진행하시겠습니까?");
            final AlertDialog.Builder builder = new AlertDialog.Builder(CertificateActivity.this);
            builder.setMessage(sb.toString());
            builder.setPositiveButton("진행", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.proceed();
                }
            });
            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.cancel();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            if (url.equals("https://www.kmcert.com/kmcis/simpleCert_mobile_v2/kmcisApp01.jsp")) {
                webView.loadUrl("javascript:goUrl('sms')");
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (url.equals("https://www.kmcert.com/kmcis/pass_m/kmcisPass00.jsp")) {
                webView.loadUrl(
                        "javascript:" +
                                "var agree_f = document.agreelistForm;" +
                                "agree_f.agree1.checked=true;" +
                                "agree_f.agree2.checked=true;" +
                                "agree_f.agree3.checked=true;" +
                                "agree_f.agree4.checked=true;");
            }
        }
    };

    WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            return super.onJsAlert(view, url, message, result);
        }
    };

    @Override
    public void finish() {
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        super.finish();
    }
}
