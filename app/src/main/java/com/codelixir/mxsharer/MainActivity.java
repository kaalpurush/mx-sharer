package com.codelixir.mxsharer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Uri uri;

    private final int OPEN_PLAYER_REQUEST = 786;

    long back_pressed = 0;

    boolean confirmBeforeExit=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        handleIntent(intent);

        findViewById(R.id.btnLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uriString = ((TextView) findViewById(R.id.tvLink)).getText().toString();
                if (URLUtil.isValidUrl(uriString)) {
                    Uri uri = Uri.parse(uriString);
                    if (uri != null)
                        openMXPlayer(uri);
                } else
                    Toast.makeText(MainActivity.this, "Invalid url!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                uri = handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("video/")) {
                uri = handleSendVideo(intent); // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("video/")) {
                uri = handleSendMultipleVideos(intent); // Handle multiple images being sent
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }

        if (uri != null) {
            ((TextView) findViewById(R.id.tvLink)).setText(uri.toString());
        }
    }

    Uri handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            Uri uri = Uri.parse(sharedText);
            if (uri != null) {
                return openMXPlayer(uri);
            }
        }
        return null;
    }

    Uri handleSendVideo(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri != null) {
            return openMXPlayer(uri);
        }
        return null;
    }

    Uri handleSendMultipleVideos(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (uris != null && !uris.isEmpty()) {
            return openMXPlayer(uris.get(0));
        }
        return null;
    }

    private Uri openMXPlayer(Uri uri) {
        confirmBeforeExit=true;
        Intent i = new Intent(Intent.ACTION_VIEW);
        Uri videoUri = uri;
        i.setDataAndType(videoUri, "video/*");
        i.setPackage("com.mxtech.videoplayer.pro");
        if (isIntentValid(this, i)) {
            startActivityForResult(i, 786);
        } else {
            i.setPackage("com.mxtech.videoplayer.ad");
            if (isIntentValid(this, i))
                startActivityForResult(i, 786);
        }

        return uri;
    }

    static boolean isIntentValid(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        return intent.resolveActivity(packageManager) != null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OPEN_PLAYER_REQUEST) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    if (data.hasExtra("end_by") && data.getStringExtra("end_by").equals("playback_completion"))
                        Toast.makeText(this, R.string.playback_end, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, R.string.playback_closed, Toast.LENGTH_SHORT).show();
                    confirmBeforeExit=false;
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(this, R.string.playback_calcelled, Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_FIRST_USER:
                    Toast.makeText(this, R.string.playback_error, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!confirmBeforeExit || back_pressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(getBaseContext(),
                    getResources().getString(R.string.press_back), Toast.LENGTH_SHORT)
                    .show();
        }
        back_pressed = System.currentTimeMillis();
    }
}
