package com.example.camilledahdah.manzili.Activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;
import com.example.camilledahdah.manzili.R;
import com.example.camilledahdah.manzili.ReadImageSequences;
import com.example.camilledahdah.manzili.WavRecorder;
import com.example.camilledahdah.manzili.api.speechrecognition.SpeechRecognitionAPI;
import com.example.camilledahdah.manzili.models.Post.SpeechAudio;
import com.example.camilledahdah.manzili.models.Post.SpeechConfiguration;
import com.example.camilledahdah.manzili.models.Post.SpeechRecognitionInfo;
import com.example.camilledahdah.manzili.models.Response.SpeechResponse;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {


    private SpeechRecognitionAPI speechRecognitionAPI;
    final int REQUEST_MICROPHONE = 1;
    TextView speechText;
    boolean microphonePermission;
    Bitmap buttonEnabled, buttonDisabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeVariables();


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE);
        }else{
            microphonePermission = true;
        }

        ReadImageSequences readImageSequences = new ReadImageSequences(this);
        readImageSequences.animateImages("Chair");

        final WavRecorder wavRecorder = new WavRecorder(this);

        if(microphonePermission) {

            final Recorder recorder = OmRecorder.wav(
                    new PullTransport.Default(new PullableSource.Default(
                            new AudioRecordConfig.Default(
                                    MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                                    AudioFormat.CHANNEL_IN_MONO, 16000
                            )
                    ), new PullTransport.OnAudioChunkPulledListener() {
                        @Override
                        public void onAudioChunkPulled(AudioChunk audioChunk) {
                            animateVoice((float) (audioChunk.maxAmplitude() / 200.0));
                        }
                    }), new File(getFilesDir(), "testing.wav"));

            recorder.startRecording();


            new Thread(new Runnable() {

                @Override
                public void run() {

                    try {
                        Thread.sleep(5000);

                        recorder.stopRecording();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    SpeechRecognitionInfo speechRecognitionInfo = new SpeechRecognitionInfo();
                    SpeechConfiguration speechConfiguration = new SpeechConfiguration();
                    SpeechAudio speechAudio = new SpeechAudio();

                    speechConfiguration.setEncoding("LINEAR16");
                    speechConfiguration.setLanguage_code("ar-LB");
                    speechConfiguration.setSampleRateHertz("16000");


                    File file = new File(wavRecorder.getNewFileName());

                    String base64Speech = "";

                    try {

                        byte[] bytes = FileUtils.readFileToByteArray(new File(getFilesDir(), "testing.wav"));
                        base64Speech = Base64.encodeToString(bytes, Base64.NO_WRAP);//STUPID BUG Base64.NO_WRAP

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    speechAudio.setContent(base64Speech);
                    speechRecognitionInfo.setSpeechAudio(speechAudio);
                    speechRecognitionInfo.setSpeechConfiguration(speechConfiguration);

                    speechRecognitionAPI = new SpeechRecognitionAPI();

                    speechRecognitionAPI.postSpeechTextResult(speechRecognitionInfo, "application/json").enqueue(new Callback<SpeechResponse>() {

                        @Override
                        public void onResponse(Call<SpeechResponse> call, Response<SpeechResponse> response) {

                            if (response.isSuccessful()) {

                                SpeechResponse speechResponse = response.body();

                                if (speechResponse != null && speechResponse.getSpeechResultsDataList() != null) {

                                    final String text = speechResponse.getSpeechResultsDataList().get(0).getAlternativeSpeechResultList().get(0).getTranscript();

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            speechText.setText(text);
                                        }
                                    });

                                }

                            } else {


                            }
                        }

                        @Override
                        public void onFailure(Call<SpeechResponse> call, Throwable t) {

                        }
                    });

                }
            }).start();
        }

    }

    private void animateVoice(float v) {
        //Log.d("voice", "voice: " + v);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        if(requestCode == REQUEST_MICROPHONE){

            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                microphonePermission = true;
            }

        }

    }

    private void initializeVariables() {
        microphonePermission = false;
        speechText = findViewById(R.id.speech_text);
        Button speakButton = findViewById(R.id.speak_button);
        Typeface englishTypeFace = Typeface.createFromAsset(getAssets(), "fonts/gogono_cocoa_mochi.ttf");
        Typeface arabicTypeFace = Typeface.createFromAsset(getAssets(), "fonts/b_arabics.ttf");

        speakButton.setBackgroundResource(R.drawable.blackbutton);

        speakButton.setTypeface(englishTypeFace);
        speechText.setTypeface(arabicTypeFace);
    }

}
