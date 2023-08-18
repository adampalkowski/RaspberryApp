package com.palrasp.raspberryapp

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.palrasp.raspberryapp.ui.theme.RaspberryAppTheme
import kotlinx.coroutines.*
import java.io.OutputStream
import java.net.Socket
import java.net.URL


@OptIn(ExperimentalComposeUiApi::class)

class MainActivity : ComponentActivity() {
    private lateinit var player: SimpleExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ExoPlayer
        player = SimpleExoPlayer.Builder(this).build()

        // Set the content using Jetpack Compose
        setContent {
            RaspberryAppTheme {
                LivestreamDisplay()

            }
        }
        fetchImages()

        // Set up the media item and prepare the player
/*        val mediaItem = MediaItem.fromUri("http://192.168.100.180/html/cam_pic_new.php") // Update the URL
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()*/
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}

@Composable
fun RaspberryAppContent(player: SimpleExoPlayer) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video player
     //   VideoPlayer(modifier=Modifier,player)
        DisplayImageFromUrl(imageUrl = "http://192.168.100.180/html/cam_pic_new.php")
        Spacer(modifier = Modifier.height(24.dp))

        // Control buttons
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { sendCommandToServer("on") },
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(text = "On")
            }
            Button(
                onClick = { sendCommandToServer("off") },
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(text = "Off")
            }
        }
    }
}

@Composable
fun DisplayImageFromUrl(imageUrl: String) {
    AsyncImage(modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        model = imageUrl,
        contentDescription = null,
    )
}

@Composable
fun VideoPlayer(modifier: Modifier = Modifier,exoPlayer:SimpleExoPlayer) {
    val context = LocalContext.current



    // player view
    DisposableEffect(
        AndroidView(
            modifier =
            Modifier.testTag("VideoPlayer") ,
            factory = {

                // exo player view for our video player
                PlayerView(context).apply {
                    player = exoPlayer
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams
                            .MATCH_PARENT,
                        ViewGroup.LayoutParams
                            .MATCH_PARENT
                    ).also { layoutParams = it }
                }
            }
        )
    ) {
        onDispose {
            // relase player when no longer needed
            exoPlayer.release()
        }
    }
}

private fun sendCommandToServer(command: String) {
    val serverAddress = "192.168.100.180" // Replace with your Raspberry Pi's IP address
    val serverPort = 12345 // Replace with the port you're using on the Raspberry Pi

    GlobalScope.launch(Dispatchers.IO) {
        try {
            Socket(serverAddress, serverPort).use { socket ->
                val outputStream: OutputStream = socket.getOutputStream()
                outputStream.write(command.toByteArray())
                outputStream.flush()

                println("Command '$command' sent to server.")
            }
        } catch (e: Exception) {
            println("Error sending command: ${e.message}")
            e.printStackTrace()
        }
    }
}

private val imageBuffer = ImageBuffer()

private fun fetchImages() {
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            val htmlContent = URL("http://192.168.100.180/html/cam_pic_new.php").readText()
            val imageUrl = extractImageUrlFromHtml(htmlContent)
            if (imageUrl != null) {
                imageBuffer.addImageUrl(imageUrl)
                Log.d("ImageBuffer", "Added image to buffer: $imageUrl")
            }
            delay(1000 / 30) // Fetch images approximately 30 times per second
        }
    }
}
private fun extractImageUrlFromHtml(htmlContent: String): String? {
    val pattern = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex()
    val matchResult = pattern.find(htmlContent)
    return matchResult?.groupValues?.getOrNull(1)
}

@Composable
fun LivestreamDisplay() {
    val currentImageUrl = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageBuffer) {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                val imageUrl = imageBuffer.getNextImageUrl()
                currentImageUrl.value = imageUrl
                Log.d("ImageDisplay", "Displaying image: $imageUrl")

            }
        }
    }

    currentImageUrl.value?.let { imageUrl ->
        Image(
            painter = rememberImagePainter(data = imageUrl),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}