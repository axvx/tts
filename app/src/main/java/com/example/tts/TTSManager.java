package com.example.tts;


// TTSManager.java
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSManager {
    private TextToSpeech tts;
    private String text;
    public TTSManager(final Context context, String text) {
        this.text=text;
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(new Locale("es", "MX"));
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });
    }

    public void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}

