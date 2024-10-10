# ASLVideoView Module Usage Guide

## Overview

The `ASLVideoView` module is a custom Android `ViewGroup` that provides functionality for displaying American Sign Language (ASL) videos based on a given word. It utilizes ExoPlayer for video playback and displays the video inside an `AlertDialog`. This module is intended to assist developers in creating accessible applications by providing an easy way to show ASL video content for specific words.

## Features
- Play ASL videos for a given word, or an invalid state otherwise.
- Display videos in a dialog with proper attribution and the word being signed.
- Automatically handle invalid word inputs.
- Reuse video player resources to optimize performance.

## Usage Example
The following example demonstrates how to use `ASLVideoView` in an activity with Jetpack Compose.

## Installation
Add the following packages to your app level build.gradle
```kotlin
implementation("androidx.media3:media3-exoplayer:1.3.1")
implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
implementation("androidx.media3:media3-ui:1.3.1")
```

### Step 1: Add `ASLVideoView` to Your Layout
You can use `ASLVideoView` inside a `Composable` using `AndroidView` from Jetpack Compose. Here is a simple example that shows entering a word into a text field and showing the ASL video on click of a button:

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ccg.slrcore.lookup.ASLVideoView

class MainActivity : ComponentActivity() {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         SLRView()
      }
   }

   @Composable
   fun SLRView() {
      var word by remember { mutableStateOf<String?>(null) }
      var inputText by remember { mutableStateOf("") }
      var trigger by remember { mutableStateOf(0) } // Trigger to force dialog show
      val context = LocalContext.current

      Scaffold(
         modifier = Modifier.fillMaxSize(),
         floatingActionButton = {
            ExtendedFloatingActionButton(
               onClick = {
                  word = inputText
                  trigger++ // Increment the trigger to force the dialog to be shown again
               },
               text = { Text("Show ASL Video") }
            )
         },
         floatingActionButtonPosition = FabPosition.Center
      ) { padding ->
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(padding)
         ) {
            Column(
               horizontalAlignment = Alignment.CenterHorizontally,
               modifier = Modifier.fillMaxSize()
            ) {
               // Input field for the word
               TextField(
                  value = inputText,
                  onValueChange = { inputText = it },
                  label = { Text("Enter a word") },
                  modifier = Modifier
                     .padding(16.dp)
                     .fillMaxWidth()
               )

               // Button to trigger video playback
               Button(onClick = {
                  word = inputText
                  trigger++ // Increment the trigger to force the dialog to be shown again
               }) {
                  Text("Play Video")
               }

               // Show ASLVideoView if the word is set
               if (!word.isNullOrEmpty()) {
                  AndroidView(
                     factory = {
                        ASLVideoView(context).apply {
                           setWord(word)
                        }
                     },
                     modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                     update = { aslVideoView ->
                        aslVideoView.setWord(word)
                        // Forcefully show the dialog again using the trigger
                        if (trigger > 0) {
                           aslVideoView.setWord(word)
                        }
                     }
                  )
               } else {
                  Text("No video to display", Modifier.padding(16.dp))
               }
            }
         }
      }
   }
}
```

### Note on Trigger Variable
The `trigger` variable is used to force the dialog to show again when the same word is used multiple times. Without this trigger, the dialog will not reappear if the word has not changed, as Jetpack Compose optimizes recompositions based on state changes.

### Step 2: Handle Lifecycle Events
Ensure that you properly release the player resources when the activity or view is no longer needed. You can call `releasePlayer()` to release the ExoPlayer instance.

```kotlin
override fun onDestroy() {
   super.onDestroy()
   aslVideoViewRef?.releasePlayer()
}
```

## Public Methods
### `setWord(word: String?)`
Sets the word for which the ASL video should be played. This method initializes the video player and displays the video in a dialog.
- **Parameter**: `word` - The word for which the ASL video should be displayed. If null or empty, no video is played. If the word is invalid (no corresponding video), a dialog appears stating that the word is invalid.

### `releasePlayer()`
Releases the ExoPlayer and dismisses any active dialog. This method should be called when `ASLVideoView` is no longer needed to free system resources.

## Conclusion
The `ASLVideoView` module is a powerful and flexible tool for displaying ASL videos in Android applications. By following the usage guidelines and best practices provided in this document, you can easily integrate ASL video playback into your app to enhance accessibility and user experience.