package xwh.sound;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

	public static final int MSG_RESULT = 1;
	public static final int MSG_CURRENT_FREQ = 2;
	public static int BASE_FREQ;
	public static int FREQ_DISTANCE;
	public static int METHOD;
	private EditText inputHz;
	private EditText BaseFreq;
	private EditText FreqDistance;
	private int[] cc = {0, 262, 294, 330, 349, 392, 440, 494};

	private TextView recordResult;
	private TextView Message;
	private TextView currentFreq;
	private Record record;
	private RadioGroup encodeMethod;
	private RadioButton method1;
	private RadioButton method2;
	private RadioButton method3;
	private Handler mHandler;

	private Button btRecord;

	private Queue<String> url_queue = new LinkedList();

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		inputHz = this.findViewById(R.id.input_hz);
		BaseFreq = this.findViewById(R.id.baseFreq);
		FreqDistance = this.findViewById(R.id.freqDistance);
		recordResult = this.findViewById(R.id.record_result);
		Message = this.findViewById(R.id.message);
		currentFreq = this.findViewById(R.id.text_current_freq);
		encodeMethod = this.findViewById(R.id.method);
		method1 = this.findViewById(R.id.encoding1);
		method2 = this.findViewById(R.id.encoding2);
		method3 = this.findViewById(R.id.encoding3);

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String re = (String) msg.obj;
				if (msg.what == MSG_RESULT) {
					recordResult.append(re + "\n");
					url_queue.add(re);
				} else if (msg.what == MSG_CURRENT_FREQ) {
					currentFreq.setText(re + "HZ");
				}
			}
		};

		Decoder decoder = new Decoder(mHandler);

		record = new Record(decoder);

		this.findViewById(R.id.bt_send).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (PCMPlayer.getInstance().isPlaying()) {
					return;
				}

				new Thread(new Runnable() {
					@Override
					public void run() {

						// 将音量调到最大
						AudioManager mAudioManager = (AudioManager) MainActivity.this.getSystemService(Context.AUDIO_SERVICE);
						int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
						int mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
						// 变更音量
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mMaxVolume, 0);

						/*int hz = Integer.parseInt(inputHz.getText().toString());
						PCMPlayer.start(hz, 2000);*/


						//PCMPlayer.start(CodeBook.freqsWave[0], 2000);

						try {
							testBase64();
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}

						//testCodes();

						//testPu();

						PCMPlayer.getInstance().stop();

						// 变更音量
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);

					}
				}).start();


			}
		});

		findViewById(R.id.bt_play).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String encode_method = "";
				if(method1.isChecked()) {
					encode_method = "Raw";
					METHOD = 1;
				}
				else if(method2.isChecked()) {
					encode_method = "74Hamming";
					METHOD = 2;
				}
				else if(method3.isChecked()) {
					encode_method = "SeqHamming";
					METHOD =3;
				}
				Message.setText("");
				BASE_FREQ = Integer.parseInt(BaseFreq.getText().toString());

				FREQ_DISTANCE = Integer.parseInt(FreqDistance.getText().toString());

				Message.append("The base frequency is "+BaseFreq.getText().toString()+" Hz.\n");
				Message.append("The frequency distance is "+FreqDistance.getText().toString() + " Hz.\n");
				Message.append("Current encoding policy is "+encode_method+".");

			}
		});



		btRecord = this.findViewById(R.id.bt_record);
		btRecord.requestFocus();
		btRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (record.isRecording()) {
					record.stopRecord();
					btRecord.setText(R.string.record);
				} else {
					btRecord.setText(R.string.recording);
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								record.start();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}
					}).start();

					new Thread(new Runnable() {
						@Override
						public void run() {
							boolean flag = true;
							while(flag){
								if (url_queue.size() > 0){
									String name = url_queue.poll();
									Runnable networkTask = () -> {
										try{
											URL url = new URL(name);
											HttpURLConnection conn = (HttpURLConnection) url.openConnection();
											if (conn.getResponseCode() == 200) {
												Intent intent = new Intent(MainActivity.this,webpage.class);
												intent.putExtra("context", name);
												startActivity(intent);
											}
										}catch(Exception e){
											e.printStackTrace();
										}
									};
									new Thread(networkTask).start();
								}
							}
						}
					}).start();
				}
			}
		});

	}



	private void testBase64() throws UnsupportedEncodingException {
		String test = inputHz.getText().toString();
		String str = Base64.encodeToString(test.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING);
		List<Integer> codes = Encoder.convertTextToCodes(str);;
		if (METHOD == 2)
			codes = Encoder.convertTextToCode_74hamming(str);
		if (METHOD == 3)
			codes = Encoder.convertTextToCode_SeqHamming(str);

		Log.d("Encode", "encodeArray:" + codes);
		PCMPlayer.getInstance().start(codes, 50);
	}



	private void testCodes() {
		List<Integer> codes = new ArrayList<>();
		codes.add(CodeBook.START_INDEX);
		codes.add(CodeBook.START_INDEX);
		for (int i = 0; i < CodeBook.CODE_BOOK_LENGTH_CONTENT; i++) {
			codes.add(i);
		}
		codes.add(CodeBook.DUPLICATE_INDEX_1);
		codes.add(CodeBook.DUPLICATE_INDEX_2);
		codes.add(CodeBook.END_INDEX);
		codes.add(CodeBook.END_INDEX);
		PCMPlayer.getInstance().start(codes, 50);
	}

	private void testPu() {
		int[] pu = {1, 1, 5, 5, 6, 6, 5, 0, 4, 4, 3, 3, 2, 2, 1};
		int step = 200;

		for (int p : pu) {
			PCMPlayer.getInstance().start(cc[p], step);
			PCMPlayer.getInstance().start(cc[0], step);
		}
	}


	private void stopRecord() {
		if (record != null) {
			record.stopRecord();
		}
	}


	@Override
	protected void onPause() {
		super.onPause();
		stopRecord();
	}
}
