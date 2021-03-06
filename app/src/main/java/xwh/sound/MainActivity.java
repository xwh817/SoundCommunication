package xwh.sound;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	public static final int MSG_RESULT = 1;
	public static final int MSG_CURRENT_FREQ = 2;

	private EditText inputHz;
	private int[] cc = {0, 262, 294, 330, 349, 392, 440, 494};

	private TextView recordResult;
	private TextView currentFreq;
	private Record record;

	private Handler mHandler;

	private Button btRecord;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		inputHz = this.findViewById(R.id.input_hz);
		recordResult = this.findViewById(R.id.record_result);
		currentFreq = this.findViewById(R.id.text_current_freq);

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String re = (String) msg.obj;
				if (msg.what == MSG_RESULT) {
					recordResult.append(re + "\n");
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

						testBase64();

						//testCodes();

						//testPu();

						PCMPlayer.getInstance().stop();

						// 变更音量
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);

					}
				}).start();


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
							record.start();
						}
					}).start();
				}


			}
		});

		findViewById(R.id.bt_play).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 播放声音在主线程会阻塞界面刷新，要放在子线程中
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							int hz = Integer.parseInt(inputHz.getText().toString());
							PCMPlayer.getInstance().start(hz, 2000);

							PCMPlayer.getInstance().stop();
						} catch (NumberFormatException e) {
							Toast.makeText(MainActivity.this, "Please input frequency", Toast.LENGTH_SHORT).show();
						}

					}
				}).start();

			}
		});

	}

	private void testBase64() {
		String test = inputHz.getText().toString();
		String str = Base64.encodeToString(test.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING);
		List<Integer> codes = Encoder.convertTextToCodes(str);

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
