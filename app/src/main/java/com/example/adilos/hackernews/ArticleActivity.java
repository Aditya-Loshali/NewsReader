package com.example.adilos.hackernews;

/**
 * Created by ADILOS on 26-07-2017.
 */

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ArticleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        //get the web view
        WebView webView = (WebView) findViewById(R.id.webview);

        //enable javascript for website
        webView.getSettings().setJavaScriptEnabled(true);

        //create and set a new web view client
        webView.setWebViewClient(new WebViewClient());

        //get data from previous intent ant set html content to webview
        Intent intent = getIntent();
        webView.loadData(intent.getStringExtra("content"), "text/html", "UTF-8");


    }
}