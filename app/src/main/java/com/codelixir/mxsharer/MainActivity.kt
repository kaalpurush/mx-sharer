package com.codelixir.mxsharer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.codelixir.mxsharer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var uri: Uri? = null

    private val OPEN_PLAYER_REQUEST = 786

    private var back_pressed: Long = 0

    private var confirmBeforeExit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Get intent, action and MIME type
        handleIntent(intent)

        binding.btnLink.setOnClickListener {
            val uriString = binding.tvLink.text.toString()
            if (URLUtil.isValidUrl(uriString)) {
                val uri = Uri.parse(uriString)
                if (uri != null)
                    openMXPlayer(uri)
            } else
                Toast.makeText(this, "Invalid url!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    //    @Override
    //    protected void onNewIntent(Intent intent) {
    //        super.onNewIntent(intent);
    //        handleIntent(intent);
    //    }

    internal fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                uri = handleSendText(intent) // Handle text being sent
            } else if (type.startsWith("video/")) {
                uri = handleSendVideo(intent) // Handle single image being sent
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            if (type.startsWith("video/")) {
                uri = handleSendMultipleVideos(intent) // Handle multiple images being sent
            }
        } else {
            val lastUri = getSetting("last_uri", "")
            if (!TextUtils.isEmpty(lastUri))
                uri = Uri.parse(lastUri)
        }

        binding.tvLink.text = uri?.toString()

    }

    private fun handleSendText(intent: Intent): Uri? {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (sharedText != null) {
            val uri = Uri.parse(sharedText)
            if (uri != null) {
                return openMXPlayer(uri)
            }
        }
        return null
    }

    private fun handleSendVideo(intent: Intent): Uri? {
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        return if (uri != null) {
            openMXPlayer(uri)
        } else null
    }

    private fun handleSendMultipleVideos(intent: Intent): Uri? {
        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        return if (uris != null && !uris.isEmpty()) {
            openMXPlayer(uris[0])
        } else null
    }

    private fun openMXPlayer(uri: Uri): Uri {
        confirmBeforeExit = true

        saveSetting("last_uri", uri.toString())

        val i = Intent(Intent.ACTION_VIEW)
        i.setDataAndType(uri, "video/*")
        i.setPackage("com.mxtech.videoplayer.pro")
        i.putExtra("return_result", true)
        if (isIntentValid(this, i)) {
            startActivityForResult(i, OPEN_PLAYER_REQUEST)
        } else {
            i.setPackage("com.mxtech.videoplayer.ad")
            if (isIntentValid(this, i))
                startActivityForResult(i, OPEN_PLAYER_REQUEST)
        }

        return uri
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OPEN_PLAYER_REQUEST) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    if (data != null && data.hasExtra("end_by") && data.getStringExtra("end_by") == "playback_completion")
                        Toast.makeText(this, R.string.playback_end, Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this, R.string.playback_closed, Toast.LENGTH_SHORT).show()
                    confirmBeforeExit = false
                }
                Activity.RESULT_CANCELED -> Toast.makeText(this, R.string.playback_calcelled, Toast.LENGTH_SHORT).show()
                Activity.RESULT_FIRST_USER -> Toast.makeText(this, R.string.playback_error, Toast.LENGTH_SHORT).show()
            }

            if (data != null && data.hasExtra("duration"))
                Toast.makeText(this, getString(R.string.playback_duration, data.getIntExtra("duration", 0) / 1000000), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (!confirmBeforeExit || back_pressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
        } else {
            Toast.makeText(baseContext,
                    resources.getString(R.string.press_back), Toast.LENGTH_SHORT)
                    .show()
        }
        back_pressed = System.currentTimeMillis()
    }

    private fun getSetting(name: String, default_value: String): String? {
        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        return settings.getString(name, default_value)
    }

    /**
     * Save setting.
     *
     * @param name  the name
     * @param value the value
     */
    private fun saveSetting(name: String, value: String) {
        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val editor = settings.edit()
        editor.putString(name, value)
        editor.apply()
    }

    companion object {

        internal fun isIntentValid(context: Context, intent: Intent): Boolean {
            val packageManager = context.packageManager
            return intent.resolveActivity(packageManager) != null
        }
    }
}
