/*
 * ProfileActivity.java
 *
 * Copyright (c) 2015 Auth0 (http://auth0.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.auth0.sample;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.core.Token;
import com.auth0.core.UserProfile;
import com.auth0.lock.Lock;
import com.auth0.sample.client.TweetClient;
import com.auth0.sample.model.Tweet;
import com.flaviofaria.kenburnsview.KenBurnsView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class ProfileActivity extends ActionBarActivity {

    private static final String SAMPLE_API_URL = "http://localhost:3001/secured/ping";
    private static final String TAG = ProfileActivity.class.getName();

    private SharedPreferences shares;
    private SharedPreferences.Editor editor;

    private SampleApplication app;
    private UserProfile profile;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private View header;
    private ImageView proPic;
    private TextView username;
    private TextView email;
    private TextView create;
    private boolean nav = false;
    private RecyclerView mRecyclerView;
    private ArrayList<Tweet> items;
    private TweetAdapter mAdapter;
    private AsyncHttpClient client;
    private Token access_token;
    private boolean refresh = false;

    private Handler refresh_handler;
    private Runnable refresh_runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        shares = getSharedPreferences("USER", 0);

        editor = shares.edit();

        profile = getIntent().getParcelableExtra(Lock.AUTHENTICATION_ACTION_PROFILE_PARAMETER);

        access_token = getIntent().getParcelableExtra(Lock.AUTHENTICATION_ACTION_TOKEN_PARAMETER);
        Log.e("Access_Token", access_token.getAccessToken());
        editor.putString("ACCESS_TOKEN", access_token.getAccessToken());
        editor.commit();

        client  = new AsyncHttpClient();
        app = (SampleApplication) getApplication();


        toolbar = (Toolbar) findViewById(R.id.toolbar);

        create = (TextView) toolbar.findViewById(R.id.create_tweet);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createTweet(ProfileActivity.this);
            }
        });

        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        header = navigationView.inflateHeaderView(R.layout.header);

        username = (TextView) header.findViewById(R.id.username);
        proPic = (ImageView) header.findViewById(R.id.profile_image);
        ((TextView)header.findViewById(R.id.email)).setText(profile.getEmail());

        /*
        client.get(((HashMap) profile.getExtraInfo().get("cover")).get("source").toString(), new FileAsyncHttpResponseHandler(ProfileActivity.this.getApplicationContext()) {
            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, File file) {

            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, final File response) {
                ProfileActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap image = BitmapFactory.decodeFile(response.getPath());

                    }
                });


            }
        });
        */
        username.setText(profile.getName());

        if (profile.getPictureURL() != null) {
            ImageLoader.getInstance().displayImage(profile.getPictureURL(), proPic);
        }


        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {


                //Checking if the item is in checked state or not, if not make it in checked state

                nav = true;

                menuItem.setChecked(false);

                //Closing drawer on item click
                drawerLayout.closeDrawers();


                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment fragment;
                //Check to see which item was being clicked and perform appropriate action
                switch (menuItem.getItemId()) {
                    case R.id.log_out:
                        editor.remove("ENDPOINT_URL").remove("ACCESS_TOKEN").commit();
                        Intent intent = getApplicationContext().getPackageManager()
                                .getLaunchIntentForPackage(getApplicationContext().getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);

                    default:
                        Toast.makeText(getApplicationContext(), "Somethings Wrong", Toast.LENGTH_SHORT).show();
                        break;

                }
                return false;
            }
        });


        // Initializing Drawer Layout and ActionBarToggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, (Toolbar) toolbar, R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
                drawerLayout.setSelected(false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank

                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessay or else your hamburger icon wont show up
        actionBarDrawerToggle.syncState();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(ProfileActivity.this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mRecyclerView.setLayoutManager(layoutManager);

        items = new ArrayList<Tweet>();

        try {
            items.add(new Tweet(new JSONObject("{\n" +
                    "      \"id\": \"f808d40b-4c54-4824-b3e0-8217e0840067\",\n" +
                    "      \"type\": \"tweets\",\n" +
                    "      \"attributes\": {\n" +
                    "        \"author\": \"Tang Rufus\",\n" +
                    "        \"body\": \"Hi all\",\n" +
                    "        \"created-at\": \"2015-02-17T01:28:32.402Z\"\n" +
                    "      }\n" +
                    "    }")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // specify an adapter (see also next example)
        mAdapter = new TweetAdapter(items);
        mRecyclerView.setAdapter(mAdapter);

        setEndpointURL(this);

        refresh_handler = new Handler();
        refresh_runnable = new Runnable() {
            @Override
            public void run() {
                if(!refresh) {
                    ProfileActivity.this.get();
                    refresh_handler.postDelayed(this, 50000);
                    refresh = true;
                }
            }
        };



    }

    private void get() {
        client.get(shares.getString("ENDPOINT_URL", "ERROR"), new RequestParams(), new JsonHttpResponseHandler() {

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                setEndpointURL(ProfileActivity.this);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                items = new ArrayList<Tweet>();
                try {
                    JSONArray response_array = response.getJSONArray("data");
                    for (int i = 0; i < response_array.length(); i++) {
                        items.add(new Tweet(response_array.getJSONObject(i)));
                    }

                } catch (JSONException e) {
                    Log.e("GET ERROR", e.toString());
                }
                mAdapter = new TweetAdapter(items);
                mRecyclerView.setAdapter(mAdapter);
            }


        });
    }

    private void create() {

        client.post("tweets/", new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                items = new ArrayList<Tweet>();
                try {
                    JSONArray response_array = response.getJSONArray("data");
                    for (int i = 0; i < response_array.length(); i++) {
                        items.add(new Tweet(response_array.getJSONObject(i)));
                    }

                } catch (JSONException e) {
                    Log.e("GET ERROR", e.toString());
                }
                mAdapter = new TweetAdapter(items);
                mRecyclerView.setAdapter(mAdapter);
            }
        });
    }


    public AlertDialog showAlertDialog(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);


        builder
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        refresh = false;
                    }
                });

        return builder.show();
    }

    public AlertDialog createTweet(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final EditText input = new EditText(ProfileActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(50,50,50,50);
        input.setLayoutParams(lp);

        builder
                .setView(input)
                .setTitle("Create A Tweet")
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RequestParams params = new RequestParams();
                        params.put("author", profile.getName());
                        params.put("body", input.getText().toString());
                        client.post(shares.getString("ENDPOINT_URL", "ERROR"), params, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, JSONObject response) {
                                Toast.makeText(ProfileActivity.this, "Created Tweet Successfully", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                Toast.makeText(ProfileActivity.this, "Failed to Create Tweet", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return builder.show();
    }

    public AlertDialog setEndpointURL(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final EditText input = new EditText(ProfileActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(50,50,50,50);
        input.setLayoutParams(lp);

        builder
                .setView(input)
                .setTitle("Set Endpoint URL")
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (input.getText().toString() != "") {
                            editor.putString("ENDPOINT_URL", input.getText().toString());
                            editor.commit();
                            refresh_handler.postDelayed(refresh_runnable, 50000);
                        } else {
                            Toast.makeText(ProfileActivity.this, "Enter An Url", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


        return builder.show();
    }

}
