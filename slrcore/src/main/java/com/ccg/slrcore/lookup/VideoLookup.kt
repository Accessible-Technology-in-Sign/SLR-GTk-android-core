

package com.ccg.slrcore.lookup

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class ASLVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var playerView: PlayerView? = null
    private var exoPlayer: ExoPlayer? = null
    private var dialog: AlertDialog? = null
    private var wordTextView: TextView? = null
    private var word: String? = null // Store the word as a class property

    init {
        Log.d("ASLVideoView", "Initialized")
    }

    fun setWord(word: String?) {
        // Store the word as a class property
        this.word = word

        // Dismiss any existing dialog if it's showing
        dialog?.dismiss()

        if (word.isNullOrEmpty()) {
            // If the word is null or empty, there's no need to proceed further
            return
        }

        // Initialize ExoPlayer if not already initialized
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }

        // Handle special cases for certain words
        var sign = word
        if (sign == "for" || sign == "if") sign = "_$sign"

        val resourceId = context.resources.getIdentifier(sign.lowercase(), "raw", context.packageName)

        if (resourceId == 0) {
            // Invalid word, show an error dialog
            showInvalidWordDialog()
            return
        }

        Log.d("MEDIA ITEM FOUND", "Preparing media item")

        // Prepare media item from resource URI
        val resourceUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .path(resourceId.toString())
            .build()

        val mediaItem = MediaItem.fromUri(resourceUri)

        // Configure ExoPlayer
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == ExoPlayer.STATE_READY) {
                        // Show the dialog once the player is ready
                        showPlayerInDialog(word)
                    }
                }
            })
            playWhenReady = true
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }


    // Method to release the ExoPlayer when no longer needed
    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
        dialog?.dismiss()
    }

    @OptIn(UnstableApi::class) private fun showPlayerInDialog(word: String) {
        // Convert word to camel case
        val camelCaseWord = word.split(" ").joinToString("") {
            it.lowercase().replaceFirstChar(Char::uppercase)
        }

        // Dismiss any existing dialog to prevent stacking multiple dialogs
        dialog?.dismiss()

        // Initialize the PlayerView if needed
        if (playerView == null) {
            playerView = PlayerView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT) // Set shutter background to transparent
                setBackgroundColor(android.graphics.Color.TRANSPARENT) // Set PlayerView background to transparent
            }
        }

        // Initialize the TextView if needed
        if (wordTextView == null) {
            wordTextView = TextView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
                textSize = 20f
                setTextColor(context.getColor(android.R.color.black)) // Set text color as desired
            }
        }

        // Initialize attribution TextView if needed
        val attributionTextView: TextView = TextView(context).apply {
            text = "Video provided by the Deaf Professional Arts Network"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            textSize = 14f
            setPadding(0, 16, 0, 0) // Add padding at the top
            setTextColor(context.getColor(android.R.color.darker_gray)) // Set text color as desired
        }

        // Set the word text and assign the player to the playerView
        wordTextView?.text = camelCaseWord
        playerView?.player = exoPlayer

        // Remove views from their previous parents if they have one
        (playerView?.parent as? ViewGroup)?.removeView(playerView)
        (wordTextView?.parent as? ViewGroup)?.removeView(wordTextView)

        // Create a LinearLayout to hold the TextView, PlayerView, and attribution TextView
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(32, 32, 32, 32) // Add padding to the container for a more visually appealing dialog
        }

        // Add views to the container
        container.addView(wordTextView)
        container.addView(playerView)
        container.addView(attributionTextView)

        // Create a new dialog each time to ensure correct view rendering
        dialog = AlertDialog.Builder(context).apply {
            setView(container)
            setCancelable(true)
        }.create()

        dialog?.apply {
            // Set layout params to ensure it appears correctly
            window?.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            window?.setGravity(Gravity.CENTER)
            show()
        }
    }


    // Show an "Invalid word" dialog
    private fun showInvalidWordDialog() {
        // Dismiss any existing dialog if it's showing
        dialog?.dismiss()

        // Create and show a new "Invalid Word" dialog
        val newDialog = AlertDialog.Builder(context)
            .setTitle("Invalid Word")
            .setMessage("The word you entered is invalid.")
            .setPositiveButton("OK") { dialogInterface, _ ->
                dialogInterface.dismiss()
                dialog?.dismiss()
            }
            .create()

        newDialog.show()

        // Store reference to the dialog in case we need to dismiss it later
        dialog = newDialog
    }

    // Handle layout of the PlayerView within this ViewGroup
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // We don't need to layout PlayerView as it will be shown in a dialog
    }

    // Handle measurement of this custom ViewGroup
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // No need to measure PlayerView in this layout as it's in a dialog
        setMeasuredDimension(0, 0) // Set this view as not taking any space
    }
}
