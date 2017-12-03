package xwh.sound;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/11/28.
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
					//ret = false;
					Log.d(TAG, "invalidate char:" + ch);
					// break;
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
}
