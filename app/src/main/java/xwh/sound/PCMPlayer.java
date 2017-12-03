package xwh.sound;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.List;

/**
 * Created by Administrator on 2017/11/20.
 */

public class PCMPlayer {
	/** 正弦波的高度 **/
	public static final int HEIGHT = 32768;
	public final static int DEFAULT_SAMPLE_RATE = 44100;

	private AudioTrack mAudioTrack;

	private static PCMPlayer instance;
	public static PCMPlayer getInstance() {
		if (instance == null) {
			instance = new PCMPlayer();
		}
		return instance;
	}

	private void init() {
		int  minBufSize = AudioTrack.getMinBufferSize(DEFAULT_SAMPLE_RATE,
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT);

		mAudioTrack=new AudioTrack(AudioManager.STREAM_MUSIC,
				DEFAULT_SAMPLE_RATE,
				AudioFormat.CHANNEL_OUT_MONO, // CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				minBufSize,
				AudioTrack.MODE_STREAM);
	}

	/**
	 * 生成正弦波
	 * @param wave
	 * @param start
	 * @param freq
	 * @param length
	 * @return
	 */
	public byte[] sin(byte[] wave, int start, int freq, int length) {
		for (int i = 0; i < length; i+=2) {
			int index = i+start;
			if (freq == 0) {
				wave[index] = (byte) 0;
				wave[index+1] = (byte) 0;
			} else {
				double angle = i * freq * Math.PI / DEFAULT_SAMPLE_RATE;
				int value = (int) (Math.sin(angle) * HEIGHT);
				wave[index] = (byte) (value & 0xff);
				wave[index+1] = (byte) ((value >> 8) & 0xff);
			}
		}
		return wave;
	}


	/**
	 * 设置频率
	 */
	public void start(int hz, int during){

		int length = DEFAULT_SAMPLE_RATE * during / 1000 * 2;

		//生成正弦波
		byte[] wave =  new byte[length];

		if(hz>0){
			if (mAudioTrack == null) {
				init();
			}

			mAudioTrack.play();

			sin(wave, 0, hz, length);


		}else{
			//sin(wave, 0, 0, length);
			sin(wave, 0, 0, length);
		}

		mAudioTrack.write(wave, 0, length);
	}


	public void start(List<Integer> codeIndexs, int during){

		if (mAudioTrack == null) {
			init();
		}

		mAudioTrack.play();

		int lengthItem = DEFAULT_SAMPLE_RATE  * during / 1000 * 2;

		//生成正弦波

		byte[] waveAll =  new byte[lengthItem * codeIndexs.size()];

		for(int i=0; i<codeIndexs.size(); i++) {
			int hz = CodeBook.encode(codeIndexs.get(i));

			sin(waveAll, lengthItem * i, hz, lengthItem);
		}

		mAudioTrack.write(waveAll, 0, waveAll.length);
	}


	public boolean isPlaying() {
		return mAudioTrack!= null && (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING);
	}


	public void stop() {
		if(mAudioTrack != null) {
			mAudioTrack.flush();
			mAudioTrack.stop();
		}
	}
}
