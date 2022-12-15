package xwh.sound;
import android.util.Log;
/**
 * 编码字典表
 * 用声音频率来表达不同的字符，理想的做法是每个字符对应一段指定频率的声音。但是声音容易被干扰，只能采用划分大的频率段的方式进行编码。
 * 字典：每位表示一段频率的声音。字典长度12（内容码8 + 开始1 + 结束1 + 重复2）
 *
 *  流程：字符串 -> Base64编码  ->  用字典对每个字符进行编码 -> 每位字典对应一段声音
 *
 * 使用Base64对源数据进行转码，可以支持汉字、特殊符号等所有情况下的字符。
 * 为了表达64种码值， 8*8 = 64，刚好需要两个字典码
 */

public class CodeBook {

	public static final int CODE_BOOK_LENGTH_CONTENT = 8;  // 内容编码长度 （0-7为内容）
	public static final int DUPLICATE_INDEX_1 = CODE_BOOK_LENGTH_CONTENT; // 重复标记1
	public static final int DUPLICATE_INDEX_2 = DUPLICATE_INDEX_1 +1; // 重复标记2
	public static final int START_INDEX = DUPLICATE_INDEX_2 + 1;   // 开始标记
	public static final int END_INDEX = START_INDEX + 1;   // 结束标记

	public static final int SEP_INDEX = END_INDEX + 1; // 分割符

	public static int freqDistance = 200;  // 两个频率之间的间距
	public static final int START_INDEX_HAMMING = 4;
	public static final int END_INDEX_HAMMING = 6;
	public static final int DUPLICATE_INDEX_1_HAMMING = 5;
	public static final int DUPLICATE_INDEX_2_HAMMING = 7;
	public static final int BASE_FREQ = 5000;
	public static final int START_FREQ_HAMMING = BASE_FREQ + freqDistance * START_INDEX_HAMMING;
	public static final int END_FREQ_HAMMING = BASE_FREQ + freqDistance * END_INDEX_HAMMING;
	public static final int DUP1_FREQ_HAMMING = BASE_FREQ + freqDistance * DUPLICATE_INDEX_1_HAMMING;
	public static final int DUP2_FREQ_HAMMING = BASE_FREQ + freqDistance * DUPLICATE_INDEX_2_HAMMING;

	/**
	 * 两个book字典码来组成下面每个字符的编码
	 */
	public final static String CONTENT_CODE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";   // Base64编码

	public static int[] freqsWave = new int[13];    // 将声音频率划分成12段，每一段表示一个字典码。

	static {
		for(int i=0; i<freqsWave.length; i++) {
			freqsWave[i] = BASE_FREQ + freqDistance * i;
		}
	}

	public static int encode(int index) {
		int freq = 0;
		if (index >= 0 && index < freqsWave.length) {
			freq = freqsWave[index];
		}
		return freq;
	}

	public static int encode_74hamming(int index) {
		if (index == START_INDEX_HAMMING) {
			return START_FREQ_HAMMING;
		}
		if (index == END_INDEX_HAMMING) {
			return END_FREQ_HAMMING;
		}
		if (index == DUP1_FREQ_HAMMING) {
			return DUP1_FREQ_HAMMING;
		}
		if (index == DUP2_FREQ_HAMMING) {
			return DUP2_FREQ_HAMMING;
		}
		int freq_idx = _inv_gray(index, 2);
		return freqsWave[index];
	}

	private static int _gray(int value, int bitNum) {
		value = value & ((1<<bitNum) - 1);
		int res = 0;
		for (int i = bitNum-1; i >= 0; i--) {
			res += (((value >> i) & 0x1) ^ ((value >> (i + 1)) & 0x1)) << i;
		}
		return res;
	}

	private static int _inv_gray(int value, int bitNum) {
		value = value & ((1<<bitNum) - 1);
		int res = value & (1<<(bitNum-1));
		for (int i = bitNum - 2; i >= 0; i--) {
			res += (((value >> i) & 0x1) ^ ((res >> (i + 1)) & 0x1)) << i;
		}
		//Log.i("_inv_gray", "before: " + value + " after: " + res);
		return res;
	}

	/**
	 * 从码库里面找到一个最相近的
	 * @param fre
	 * @return index
	 */
	public static int decode(int fre) {
		int index = -1;

		if ( fre + freqDistance > freqsWave[0]) {   // 太小的不要
			int min = Integer.MAX_VALUE;
			for(int i=0; i<freqsWave.length; i++) {
				int distance = Math.abs(fre - freqsWave[i]);
				if(distance < min) {
					min = distance;
					index = i;
				}
			}
		}
		return index;
	}

	public static int decode_hamming(int fre) {
		int index = decode(fre);
		//if (index == -1 || index >= START_INDEX_HAMMING) {
		//	return index;
		//}
		//index = _gray(index, 2);
		if (index > DUPLICATE_INDEX_2_HAMMING) index = DUPLICATE_INDEX_2_HAMMING;
		return index;
	}
}
