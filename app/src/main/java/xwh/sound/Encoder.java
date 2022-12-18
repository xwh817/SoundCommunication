package xwh.sound;

import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by xwh on 2017/11/28.
 */

public class Encoder {
	public static final String TAG = "Encoder";

	public static List<Integer> convertTextToCodes(String text) {
		List<Integer> mCodes = new ArrayList<>();
		if (!TextUtils.isEmpty(text)) {
			mCodes.add(CodeBook.START_INDEX);    //开始
			mCodes.add(CodeBook.START_INDEX);    //开始(防止丢失，重复添加两组)
			int len = text.length();
			for (int i = 0; i < len; ++i) {     // 内容
				char ch = text.charAt(i);
				int[] indexs = Utils.char2Indexs(ch);
				if (indexs != null) {
					mCodes.add(indexs[0]);
					mCodes.add(indexs[1]);
				} else {
					Log.d(TAG, "invalidate char:" + ch);
				}
			}
			// 对内容进行crc校验
			int[] crc = Utils.crc(mCodes, 2, mCodes.size()-1);
			mCodes.add(crc[0]);
			mCodes.add(crc[1]);
			mCodes.add(CodeBook.END_INDEX);   //结束
			mCodes.add(CodeBook.END_INDEX);   //结束(防止丢失，重复添加两组)
			// 替换内容相邻相同字符
			for (int i=mCodes.size()-3; i>2; i--) {
				if (mCodes.get(i) == mCodes.get(i-1)) {
					mCodes.set(i, i%2 == 0 ? CodeBook.DUPLICATE_INDEX_1 : CodeBook.DUPLICATE_INDEX_2);
				}
			}
		}
		return mCodes;
	}

	public static List<Integer> convertTextToCode_SeqHamming(String text) {
		List<Integer> mCodes = new ArrayList<>();
		Log.d(TAG, text);
		if (!TextUtils.isEmpty(text)) {
			mCodes.add(CodeBook.START_INDEX);    //开始
			mCodes.add(CodeBook.START_INDEX);    //开始(防止丢失，重复添加两组)
			int len = text.length();
			if (len % 2 == 1) {
				if (text.endsWith("0")) {
					text = text.concat("1");
				} else {
					text = text.concat("0");
				}
			} else {
				if (text.endsWith("0")) {
					text = text.concat("11");
				} else {
					text = text.concat("00");
				}
			}
			Log.d(TAG, text);
			len = text.length();
			for (int i = 0; i < len; i+=2) {     // 内容
				char ch1 = text.charAt(i);
				char ch2 = text.charAt(i+1);
				int[] indexs1 = Utils.char2Indexs(ch1);
				int[] indexs2 = Utils.char2Indexs(ch2);
				int chk1;
				int chk2;
				int chk3;
				if (indexs1 != null && indexs2 != null) {
					chk1 = (CodeBook.CODE_BOOK_LENGTH_CONTENT - ((indexs1[0] + indexs1[1] + indexs2[1]) % CodeBook.CODE_BOOK_LENGTH_CONTENT)) % CodeBook.CODE_BOOK_LENGTH_CONTENT;
					chk2 = (CodeBook.CODE_BOOK_LENGTH_CONTENT - ((indexs1[0] + indexs2[0] + indexs2[1]) % CodeBook.CODE_BOOK_LENGTH_CONTENT)) % CodeBook.CODE_BOOK_LENGTH_CONTENT;
					chk3 = (CodeBook.CODE_BOOK_LENGTH_CONTENT - ((indexs1[1] + indexs2[0] + indexs2[1]) % CodeBook.CODE_BOOK_LENGTH_CONTENT)) % CodeBook.CODE_BOOK_LENGTH_CONTENT;
					mCodes.add(chk1);
					mCodes.add(chk2);
					mCodes.add(indexs1[0]);
					mCodes.add(chk3);
					mCodes.add(indexs1[1]);
					mCodes.add(indexs2[0]);
					mCodes.add(indexs2[1]);
					mCodes.add(CodeBook.SEP_INDEX);
					mCodes.add(CodeBook.SEP_INDEX);
				} else {
					Log.d(TAG, "invalidate char:" + ch1 + " or " + ch2);
				}
			}
			mCodes.add(CodeBook.END_INDEX);   //结束
			mCodes.add(CodeBook.END_INDEX);   //结束(防止丢失，重复添加两组)
			Log.d(TAG, mCodes.toString());
			// 替换内容相邻相同字符
			for (int i=mCodes.size()-3; i>2; i--) {
				if (mCodes.get(i) == mCodes.get(i-1) && mCodes.get(i) != CodeBook.SEP_INDEX) {
					mCodes.set(i, i%2 == 0 ? CodeBook.DUPLICATE_INDEX_1 : CodeBook.DUPLICATE_INDEX_2);
				}
			}
		}
		return mCodes;
	}

	public static List<Integer> convertTextToCode_74hamming(String text) throws UnsupportedEncodingException {
		byte[] bytes = text.getBytes("UTF-8");
		List<Integer> mCodes = new ArrayList<>();
		mCodes.add(CodeBook.START_INDEX_HAMMING);
		mCodes.add(CodeBook.START_INDEX_HAMMING);
		int[] temp = new int[14];
		StringBuilder binaryString = new StringBuilder();
		// little endian
		for (byte b: bytes) {
			int cb = b;
			Log.i("byte", String.valueOf(cb));
			for (int i = 0; i < 2; ++i) {
				int curr = (b >> (i * 4)) & 0xf;
				Log.i("curr", String.valueOf(curr));
				int d1 = curr & 1;
				int d2 = (curr >> 1) & 1;
				int d3 = (curr >> 2) & 1;
				int d4 = (curr >> 3) & 1;
				temp[2 + i * 7] = d1;
				temp[4 + i * 7] = d2;
				temp[5 + i * 7] = d3;
				temp[6 + i * 7] = d4;
				//chk bits
				temp[0 + i * 7] =  (d1 ^ d2 ^ d4);
				temp[1 + i * 7] =  (d1 ^ d3 ^ d4);
				temp[3 + i * 7] =  (d2 ^ d3 ^ d4);
			}
			Log.i("bitarray", Arrays.toString(temp));
			int last_code = -1;
			boolean odd = true;
			for (int i = 0; i < 7; ++i) {
				int code = temp[i*2] + (temp[i*2+1] << 1);
				if (code != last_code) {
					odd = true;
					mCodes.add(code);
					last_code = code;
				} else if (odd) {
					mCodes.add(CodeBook.DUPLICATE_INDEX_1_HAMMING);
					odd = false;
				} else {
					mCodes.add(CodeBook.DUPLICATE_INDEX_2_HAMMING);
					odd = true;
				}
			}
		}
		mCodes.add(CodeBook.END_INDEX_HAMMING);
		mCodes.add(CodeBook.END_INDEX_HAMMING);
		Log.i("mCodes", "before: " + text + " after: " + mCodes);
		return mCodes;
	}
}
