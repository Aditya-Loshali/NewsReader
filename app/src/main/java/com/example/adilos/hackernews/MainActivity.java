package com.example.adilos.hackernews;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    //array list to store news titles
    ArrayList <String> titles = new ArrayList<>();
    //array list to store news html content
    ArrayList <String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    SQLiteDatabase articlesDb;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();
        if (itemThatWasClickedId == R.id.action_refresh) {
            DownloadTask task = new DownloadTask();

            try {
                task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateListView();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);

        //bind array adapter to arrar list
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        //set array adapter on list view
        listView.setAdapter(arrayAdapter);

        //set a click listener on list view items , which on click will
        // open news content in a webview on a new activity
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                //send html content to the content activity
                intent.putExtra("content", content.get(i));
                startActivity(intent);
            }
        });

        //opens or creates(if not already created) a database "Articles"
        articlesDb = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        //creates a table to store news data if not already exists
        articlesDb.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");

        //call function to update listview content
        updateListView();

        DownloadTask task = new DownloadTask();

        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateListView() {
        //retrieve data from database
        Cursor c = articlesDb.rawQuery("SELECT * FROM articles", null);

        //retrieve the index of content and title columns in database
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        //retrieve and add data to titles and content array list
        if (c.moveToFirst()) {
            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());

            //notify array adapter of new data
            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings){

            String result = "";

            URL url;

            HttpURLConnection urlConnection = null;

            try {

                url = new URL(strings[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();
                Scanner scanner = new Scanner(in);
                scanner.useDelimiter("\\A");
                String articleInfo="";

                boolean hasInput = scanner.hasNext();
                if (hasInput) {
                    result = scanner.next();
                } else {
                    //return null;
                }

                Log.i("URLContent", result);

                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 20;

                if (jsonArray.length() < 20) {

                    numberOfItems = jsonArray.length();

                }

                articlesDb.execSQL("DELETE FROM articles");

                for (int i = 0; i < numberOfItems; i++) {

                    String articleId = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();

                    in = urlConnection.getInputStream();
                    scanner = new Scanner(in);
                    scanner.useDelimiter("\\A");

                    hasInput = scanner.hasNext();
                    if (hasInput) {
                         articleInfo = scanner.next();
                    } else {
                        return null;
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {

                        String articleTitle = jsonObject.getString("title");

                        String articleContent = "";

                        String articleURL = jsonObject.getString("url");

                        url = new URL(articleURL);

                        urlConnection = (HttpURLConnection) url.openConnection();

                        in = urlConnection.getInputStream();
                        scanner = new Scanner(in);
                        scanner.useDelimiter("\\A");

                        hasInput = scanner.hasNext();
                        if (hasInput) {
                            articleContent = scanner.next();
                        } else {
                            return null;
                        }

                        Log.i("articleContent", articleContent);

                        String sql = "INSERT INTO articles (articleId, title, content) VALUES (?, ?, ?)";

                        SQLiteStatement statement = articlesDb.compileStatement(sql);

                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent);

                        statement.execute();

                    }

                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }

}

