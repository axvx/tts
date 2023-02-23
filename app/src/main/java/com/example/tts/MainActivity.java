package com.example.tts;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int SPEECH_REQUEST_CODE = 0;
    int trad=1;
    int convAnterior=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the "Speak" button to initiate speech recognition
        findViewById(R.id.speak).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSpeechRecognition();
            }
        });
    }

    private void startSpeechRecognition() {
        // Create an intent to initiate the speech recognition
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");

        // Start the speech recognition activity
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            // Get the results of the speech recognition
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            // Display the results in the TextView
            TextView textView = findViewById(R.id.speech_text);
            // textView.setText(results.get(0));

            //Uncomment if you don't want to translate from spanish to english
           // new SendQuestionTask().execute(results.get(0), "replace-with-chatgpt-api-key");


            //En caso de que se le pida codigo fuente no habra una traduccion como tal
            if (results.get(0).contains("código fuente")){
            //no traducir
                trad=0;

            }

            if (results.get(0).contains("basado en nuestra conversación previa")){
               convAnterior=1;


            }
            TransSpanToEnglish(results.get(0));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public class SendQuestionTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String question = params[0];
            String apiKey = params[1];
            String requestBody;


            try {
                // Create a connection to the GPT-3 API
                URL url = new URL("https://api.openai.com/v1/completions");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);

                // Set the body of the request
                // String requestBody = "{\"prompt\":\"" + question + "\"}";
                if(convAnterior!=1){
                  requestBody = "{\"model\": \"text-davinci-003\", \"prompt\": \"" + question + "\", \"max_tokens\": 1024}";

                }
                else{
                    TextView textView = findViewById(R.id.speech_text);
                    question="Based in the following conversation: " + textView.getText()+" " + "can you detail the point number 1?";
                    requestBody = "{\"model\": \"text-davinci-003\", \"prompt\": \"" + question + "\", \"max_tokens\": 1024}";
                    convAnterior=0;

                }

                connection.setDoOutput(true);
                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.writeBytes(requestBody);
                outputStream.flush();
                outputStream.close();

                // Send the request and get the response
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);

                    }
                    reader.close();


                    Log.e("GPT3", "res " + response.toString());

                    return response.toString();
                } else {
                    Log.e("GPT3", "Failed to send question: " + responseCode);
                    return null;
                }
            } catch (Exception e) {
                Log.e("GPT3", "Failed to send question", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            if (response != null) {
                // Process the response here

                TextView textView = findViewById(R.id.speech_text);
                textView.setText(response);

                JSONObject parser = new JSONObject();
                JSONObject obj = null;
                try {
                    // JSONArray jsonarray = new JSONArray(response);
                    JSONObject jObject = new JSONObject(response);
                    JSONArray jObject2 = jObject.getJSONArray("choices");

                    for (int i = 0; i < jObject2.length(); i++) {
                        String text = jObject2.getJSONObject(i).getString("text");
                      if (trad==0){
                          textView.setText(text);
                          trad=1;
                      }
                      else{
                          textView.setText(TransEnglishToSpanish(text));

                      }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }


//extractText(response.toString());

                // ...
            } else {
                // Handle error
            }
        }
    }

    public String extractText(String jsonString) {
        try {
            // Parse the JSON string and create a JSONObject object
            JSONObject json = new JSONObject(jsonString);

            // Extract the value of the "text" field
            String text = json.getString("text:");
            TextView textView = findViewById(R.id.speech_text);

            textView.setText(text);
        } catch (JSONException e) {
            // If there's an error parsing the JSON, return an empty string

        }
        return "";
    }

    private void TransSpanToEnglish(String Spanish) {
        // Create an English-German translator:
        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.SPANISH)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build();
        final Translator englishGermanTranslator =
                Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
        englishGermanTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(
                        new OnSuccessListener() {
                            @Override
                            public void onSuccess(Object o) {

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Model couldn’t be downloaded or other internal error.
                                // ...
                            }
                        });

        englishGermanTranslator.translate(Spanish).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        System.out.println("gelier:" + s);
                        updateUI2(s);
                    }
                })


                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Error.
                                // ...
                            }
                        });

    }


    private String TransEnglishToSpanish(String Spanish) {
        final String[] Traduccion = new String[1];
        // Create an English-German translator:
        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(TranslateLanguage.SPANISH)
                        .build();
        final Translator englishGermanTranslator =
                Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
        englishGermanTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(
                        new OnSuccessListener() {
                            @Override
                            public void onSuccess(Object o) {

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Model couldn’t be downloaded or other internal error.
                                // ...
                            }
                        });

        englishGermanTranslator.translate(Spanish).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        //System.out.println("Traduccion:" + s);
                        Traduccion[0] = s;
                        updateUI(s);

                    }
                })


                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Error.
                                // ...
                            }
                        });

        return Traduccion[0];

    }

    private void updateUI(String translatedText) {
        TextView textView = findViewById(R.id.speech_text);

        textView.setText(translatedText);
        // System.out.println("Traduccion:" + translatedText);
         TTSManager ttsManager;
         ttsManager = new TTSManager(this,translatedText);

    }

    private void updateUI2(String translatedText) {

       // System.out.println("Traduccion:" + translatedText);
        new SendQuestionTask().execute(translatedText, "replace-with-chatgpt-api-key");

    }

}