package com.onvit.chatapp.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.onvit.chatapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class BigPictureActivity extends AppCompatActivity {
    private static final int WRITE_EXTERNAL_STORAGE_CODE = 1;
    private Toolbar chatToolbar;
    private PhotoView imageView;
    private LinearLayout imageView2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_big_picture);

        chatToolbar = findViewById(R.id.chat_toolbar);
        chatToolbar.setBackgroundResource(R.color.black);
        setSupportActionBar(chatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getIntent().getStringExtra("name"));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
        String uri = getIntent().getStringExtra("uri");

        imageView = findViewById(R.id.picture_img);
        imageView2 = findViewById(R.id.img_share);

        LinearLayout downImg = findViewById(R.id.img_down);
        Glide.with(this).load(uri).apply(new RequestOptions().fitCenter()).into(imageView);
        downImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imgDownLoad();
            }
        });
        imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imgShare(imageView);
            }
        });
    }

    void imgShare(ImageView img){
        Bitmap bitmap = ((BitmapDrawable) img.getDrawable()).getBitmap();
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(System.currentTimeMillis());
        String imagename = time + ".PNG";
        File path = getFilesDir();
        File file = new File(path, imagename);
        OutputStream out;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {// API 24 이상 일경우..
                uri = FileProvider.getUriForFile(BigPictureActivity.this, getApplicationContext().getPackageName() + ".fileprovider", file);
            } else {// API 24 미만 일경우..
                uri = Uri.fromFile(file);
            }
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("image/*");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "친구에게 공유하기"));

        } catch (Exception e) {
            Toast.makeText(BigPictureActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void imgDownLoad(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                String[] permission = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, WRITE_EXTERNAL_STORAGE_CODE);
            } else {
                Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(System.currentTimeMillis());
                File path = Environment.getExternalStorageDirectory();
                File dir = new File(path + "/KCHA");
                dir.mkdirs();
                String imagename = time + ".PNG";
                File file = new File(dir, imagename);
                OutputStream out;
                try {
                    out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();

                    Toast.makeText(BigPictureActivity.this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    BigPictureActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                } catch (Exception e) {
                    Toast.makeText(BigPictureActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "Permission enable", Toast.LENGTH_SHORT).show();
                }
        }
    }

    //툴바에 뒤로가기 버튼
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
               finish();
               return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
