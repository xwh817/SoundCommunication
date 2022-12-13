package xwh.sound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class webpage extends AppCompatActivity {


    private WebView web;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webpage);
        Intent intent=getIntent();
        String name=intent.getStringExtra("context");

        web = this.findViewById(R.id.web_view);
        web.getSettings().setJavaScriptEnabled(true);
        web.loadUrl(name);
        web.setWebViewClient(new WebViewClient());




    }
}