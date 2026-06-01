package com.profconq.app.media

import android.media.MediaPlayer
import java.io.File

/** Plays a local audio file once; releases the player when finished. */
fun playAudioFile(path: String) {
    val file = File(path)
    if (!file.exists()) return
    MediaPlayer().apply {
        setDataSource(path)
        prepare()
        start()
        setOnCompletionListener { release() }
        setOnErrorListener { _, _, _ ->
            release()
            true
        }
    }
}
