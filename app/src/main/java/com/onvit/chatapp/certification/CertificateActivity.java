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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onvit.chatapp.LoginActivity;
import com.onvit.chatapp.R;
import com.onvit.chatapp.SignUpActivity;
import com.onvit.chatapp.SplashActivity;
import com.onvit.chatapp.model.KCHA;
import com.onvit.chatapp.model.User;

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
                                user.setGrade("일반");
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

        }

    };

    WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.d("TestCert", "onPageFinished: " + errorCode);
            view.loadUrl("about:blank");
            AlertDialog.Builder builder = new AlertDialog.Builder(CertificateActivity.this);
            builder.setTitle("오류 발생");
/*
            switch(errorCode) {
                case ERROR_AUTHENTICATION: break;
                // 서버에서 사용자 인증 실패
                case ERROR_BAD_URL: break;
                // 잘못된 URL
                case ERROR_CONNECT: break;
                // 서버로 연결 실패
                case ERROR_FAILED_SSL_HANDSHAKE: break;
                // SSL handshake 수행 실패
                case ERROR_FILE: break;
                // 일반 파일 오류
                case ERROR_FILE_NOT_FOUND: break;
                // 파일을 찾을 수 없습니다
                case ERROR_HOST_LOOKUP: break;
                // 서버 또는 프록시 호스트 이름 조회 실패
                case ERROR_IO: break;
                // 서버에서 읽거나 서버로 쓰기 실패
                case ERROR_PROXY_AUTHENTICATION: break;
                // 프록시에서 사용자 인증 실패
                case ERROR_REDIRECT_LOOP: break;
                // 너무 많은 리디렉션
                case ERROR_TIMEOUT: break;
                // 연결 시간 초과
                case ERROR_TOO_MANY_REQUESTS: break;
                // 페이지 로드중 너무 많은 요청 발생
                case ERROR_UNKNOWN: break;
                // 일반 오류
                case ERROR_UNSUPPORTED_AUTH_SCHEME: break;
                // 지원되지 않는 인증 체계
                case ERROR_UNSUPPORTED_SCHEME: break;
                // URI가 지원되지 않는 방식
            }
*/
            Log.d("WebViewError", Integer.toString(errorCode));
            if (errorCode == ERROR_HOST_LOOKUP) {
                builder.setMessage("페이지를 불러오는데 실패했습니다.\n네트워크 상태를 확인해주세요");
            } else {
                Log.d("ERRORCODE", errorCode + "");
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
            Log.d("TestCert", "onPageFinished: " + url);
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