package xwh.sound;

import java.util.List;

/**
 * Created by Administrator on 2017/6/6.
 */
public class Utils {

	/**
	 * 将字符转成字典中的Index
	 * @return
	 */
	public static int[] char2Indexs(char c) {
		int[] indexs = null;
		int cIndex = CodeBook.CONTENT_CODE.indexOf(c);
		int bookSize = CodeBook.CODE_BOOK_LENGTH_CONTENT;
		if (cIndex >= 0) {
			indexs = new int[2];
			indexs[0] = cIndex / bookSize;
			indexs[1] = cIndex % bookSize;
		}
		return indexs;
	}

	public static char indexs2Char(int i, int j) {
		int bookSize = CodeBook.CODE_BOOK_LENGTH_CONTENT;
		int index = i * bookSize + j;
		char c = 0;
		if (index >=0 && index < CodeBook.CONTENT_CODE.length()) {
			c = CodeBook.CONTENT_CODE.charAt(index);
		}
		return c;
	}

	/**
	 * 简单的CRC校验，整体求和取余作为校验位
	 * @param mCodes
	 * @param start
	 * @return
	 */
	public static int[] crc(List<Integer> mCodes, int start, int end) {

		int count = 0;
		for (int j = start; j <= end; j++) {
			count += mCodes.get(j);
		}
		int value = count % CodeBook.CONTENT_CODE.length();
		int[] crc = new int[2];

		int bookSize = CodeBook.CODE_BOOK_LENGTH_CONTENT;

		crc[0] = value / bookSize;
		crc[1] = value % bookSize;

		return crc;

	}


}
