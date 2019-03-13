package xwh.sound;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Created by Administrator on 2017/11/20.
 */

public class Record {

	public static final String TAG = "Record";

	public static final int DEFAULT_SAMPLE_RATE = 44100;
	private static final int SAMPLE_STEP = 10;	// 一秒之内多少次Buffer采样

	private AudioRecord mAudioRecord;

	private int readSize = 0;
	private int mBufferSize;
	private byte[] buffer;


	private Decoder mDecoder;

	public Record(Decoder decoder) {
		this.mDecoder = decoder;
	}

	public void getValidSampleRates() {
		for (int rate : new int[] {8000, 11025, 16000, 22050, 44100}) {  // add the rates you wish to check against
			int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
			if (bufferSize > 0) {

			}
		}
	}


	public void start() {

		mBufferSize = (DEFAULT_SAMPLE_RATE / SAMPLE_STEP)  * 2;  // 16Bit，两个字节一个采样值。 mBufferSize是每次获取的录音数据量，这里取采样率的一部分，也就是0.1s的采样数据
		int minBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		if (mBufferSize > minBufferSize) {

			buffer = new byte[mBufferSize];

			mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mBufferSize);

			mAudioRecord.startRecording();


			short[] waveDatas = new short[mBufferSize/2];

			while (isRecording()) {
				readSize = mAudioRecord.read(buffer, 0, mBufferSize);
				if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
					short sh;

					for (int i = 0; i < mBufferSize; i+=2) {

						short sh1 = buffer[i];
						sh1 &= 0xff;
						short sh2 = buffer[i+1];
						sh2 <<= 8;
						sh = (short) ((sh1) | (sh2));     // 16Bit，两个字节一个采样值。

						waveDatas[i/2] = sh;

					}

					mDecoder.countFreq(waveDatas, SAMPLE_STEP);

				}

			}
		}

	}



	public void stopRecord() {
		if (mAudioRecord != null) {
			mAudioRecord.stop();
		}
	}

	public boolean isRecording() {
		return mAudioRecord != null && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING;
	}

}
