package xwh.sound;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import xwh.sound.utils.FFT;

/**
 * Created by xwh on 2017/11/28.
 */

public class Decoder {

	public static final String TAG = "Decoder";

    private final static int UP = 1;
    private final static int DOWN = 2;

    private int waveCount;

    private int countStartCode = 0;
    private int countEndCode = 0;
    private boolean startDecode;
    private ArrayList<Integer> codeIndexs;
    private Deque<Integer> codeQueue;
    private Deque<Integer> debugQueue;
    private int queueTop;
    private int lastIndex;

    private Handler mHandler;

    private long lastStartTime;
    private static final int TIMEOUT = 10000;

    private static final int COUNT_STEP_SIZE = 20;

    private FFT fft = new FFT();

    public Decoder(Handler handler) {
        this.mHandler = handler;
        codeIndexs = new ArrayList<>();
        codeQueue = new LinkedList<Integer>();
        debugQueue = new LinkedList<Integer>();
        queueTop = -1;
        lastIndex = -1;
    }

    public void countFreq(short[] datas, int sampleStep) {
       /* int currentFreq = 0;
        for (int i=0; i<sampleStep; i++) {
            currentFreq = fft.getFrequency(datas, Record.DEFAULT_SAMPLE_RATE, i * datas.length / sampleStep, 256);
            decodeFre(currentFreq);
        }*/

        int currentFreq = fft.getFrequency(datas, Record.DEFAULT_SAMPLE_RATE, 0, 256*8);
        decodeFre(currentFreq);

        // 只显示一次
        Message msg = mHandler.obtainMessage();
        msg.what = MainActivity.MSG_CURRENT_FREQ;
        msg.obj = currentFreq + "";
        mHandler.sendMessage(msg);
    }

    /**
     * 对一个录音Buffer进行频率统计
     */
    public int countFreq1(short[] datas, int sampleStep) throws UnsupportedEncodingException {
        int itemStep = datas.length / COUNT_STEP_SIZE;
        int waveState = -1;
        int stepCount = 0;
        int bufferFreqCount = 0;
        int currentFreq = 0;
        for(short sample : datas) {
            if (waveState == -1) {
                waveState = sample > 0 ? UP : DOWN;
            }
            // 根据波形上下计算频率
            if (waveState == UP) {
                if (sample < -10) {
                    waveState = DOWN;
                    waveCount++;
                }
            } else {
                if (sample > 10) {
                    waveState = UP;
                    waveCount++;
                }
            }
            /**
             * 并不是每个buffer取一次频率，而是在一段buffer中获取小段，每个小段进行解码
             */
            stepCount++;
            if (stepCount >= itemStep) {
                //waveCount = waveCount / 2;  // 一上一下表示一个波形，所以要除以2
                bufferFreqCount += waveCount;
                currentFreq = waveCount * COUNT_STEP_SIZE * sampleStep / 2;	// 这里根据每小段得出频率（一秒内波形次数）
                decodeFre_SeqHamming(currentFreq);
                stepCount = 0;
                waveCount = 0;
            }
        }
        if (mHandler != null) {
            int freq = bufferFreqCount * sampleStep / 2;  // 一上一下表示一个波形，所以要除以2
            Message msg = mHandler.obtainMessage();
            msg.what = MainActivity.MSG_CURRENT_FREQ;
            msg.obj = freq + "";
            mHandler.sendMessage(msg);
            if (freq > CodeBook.START_FREQ_HAMMING) {
                //Log.i("Record", "fre:" + freq);
            }
        }
        return currentFreq;
    }

    /**
     * 将频率解码为码库对应index，当出现结束值时返回结果
     * @param fre
     */
    public void decodeFre(int fre) {
        int codeIndex = CodeBook.decode(fre);
        if (codeIndex != -1) {
            //Log.i("Record", "waveCount:" + waveCount + ", fre:" + fre + ", decode:" + codeIndex);
            if (codeIndex == CodeBook.START_INDEX) {
                countEndCode = 0;
                if (startDecode) {
                    return;
                }
                countStartCode++;
                if (countStartCode >= 2) {
                    countStartCode = 0;
                    startDecode = true;
                    codeIndexs.clear();
                    lastStartTime = System.currentTimeMillis();
                }
            } else if (startDecode) {
                countStartCode = 0;
                if (System.currentTimeMillis() - lastStartTime > TIMEOUT) {
                    startDecode = false;    // 可能上一次结束码丢失，超时
                    return;
                }
                if (codeIndex == CodeBook.END_INDEX) {
                    countEndCode++;
                    if (countEndCode >= 2) {
                        countEndCode = 0;
                        startDecode = false;
                        List<Integer> cleanIndexs = cleanCodeIndexs(codeIndexs);
                        Log.i("Record", "clearCodeIndexs:" + cleanIndexs);
                        if(cleanIndexs.size() > 0) {
                            showResult(cleanIndexs);
                        }
                    }
                } else {
                    countEndCode = 0;
                    codeIndexs.add(codeIndex);
                }
            } else {
                countStartCode = 0;
                countEndCode = 0;
            }
        }
    }

    public void decodeFre_SeqHamming(int fre) {
        int codeIndex = CodeBook.decode(fre);
        if (codeIndex != -1) {
            //Log.i("Record", "waveCount:" + waveCount + ", fre:" + fre + ", decode:" + codeIndex);
            if (codeIndex == CodeBook.START_INDEX) {
                countEndCode = 0;
                if (startDecode) {
                    return;
                }
                countStartCode++;
                if (countStartCode >= 2) {
                    countStartCode = 0;
                    startDecode = true;
                    codeIndexs.clear();
                    lastStartTime = System.currentTimeMillis();
                }
            } else if (startDecode) {
                countStartCode = 0;
                if (System.currentTimeMillis() - lastStartTime > TIMEOUT) {
                    startDecode = false;    // 可能上一次结束码丢失，超时
                    return;
                }
                if (codeIndex == CodeBook.END_INDEX) {
                    countEndCode++;
                    if (countEndCode >= 2) {
                        countEndCode = 0;
                        startDecode = false;
                        List<Integer> cleanIndexs = cleanCodeIndexs(codeIndexs);
                        Log.i("Record", "clearCodeIndexs:" + cleanIndexs);
                        if(cleanIndexs.size() > 0) {
                            showResult_SeqHamming(cleanIndexs);
                        }
                    }
                } else {
                    countEndCode = 0;
                    codeIndexs.add(codeIndex);
                }
            } else {
                countStartCode = 0;
                countEndCode = 0;
            }
        }
    }

    public void decodeFre_74hamming(int fre) throws UnsupportedEncodingException {

        int codeIndex = CodeBook.decode_hamming(fre);
        if (codeIndex == -1) {
            return;
        } else if (codeIndex == CodeBook.START_INDEX_HAMMING) {
            if (startDecode) {
                return;
            }
            countStartCode++;
            if (countStartCode >= 2) {
                countEndCode = 0;
                debugQueue.clear();
                codeQueue.clear();
                startDecode = true;
                codeIndexs.clear();
                lastStartTime = System.currentTimeMillis();
            }
        } else if (startDecode) {
            countStartCode = 0;
            if (System.currentTimeMillis() - lastStartTime > TIMEOUT) {
                startDecode = false;
                return;
            }
            if (codeIndex == CodeBook.END_INDEX_HAMMING) {
                countEndCode++;
                if (countEndCode >= 2) {
                    countEndCode = 0;
                    startDecode = false;
                    List<Integer> cleanIndexs = dummyCodeIndexs(codeIndexs);

                    Log.i("Record", "clearCodeIndexs:" + cleanIndexs);
                    Log.i("Record", "fracs: " + debugQueue.toString());

                    if (cleanIndexs.size() > 0) {
                        showResult_74hamming(cleanIndexs);
                    }
                }
            } else {
                countEndCode = 0;
                if (lastIndex != -1 && codeIndex == lastIndex) {
                    return;
                } else {
                    lastIndex = codeIndex;
                    if (queueTop != -1 && (codeIndex == CodeBook.DUPLICATE_INDEX_2_HAMMING || codeIndex == CodeBook.DUPLICATE_INDEX_1_HAMMING)) {
                        codeQueue.add(queueTop);
                        debugQueue.add(queueTop);
                    } else {
                        codeQueue.add(codeIndex);
                        debugQueue.add(codeIndex);
                        queueTop = codeIndex;
                    }
                }
                if (codeQueue.size() >= 7) {
                    int c = 0;
                    int s = 0;
                    for (int i = 0; i < 7 && !codeQueue.isEmpty(); ++i) {
                        s += (codeQueue.pollFirst() << (2 * i));
                    }
                    int [] code = new int [7];
                    for (int i = 0; i < 2; ++i) {
                        int curr7bit = (s >> (i * 7)) & 0x7f;
                        for (int j = 0; j < 7; ++j) {
                            code[j] = (curr7bit >> j) & 0x1;
                        }
                        int ch1 = code[0] ^ code[2] ^ code[4] ^ code[6];
                        int ch2 = code[1] ^ code[2] ^ code[5] ^ code[6];
                        int ch3 = code[3] ^ code[4] ^ code[5] ^ code[6];
                        int errorBit = ch1 + (ch2 << 1) + (ch3 << 2);
                        if (errorBit != 0) {
                            code[errorBit-1] = 1 - code[errorBit-1];
                        }
                        int curr4bit = code[2] + (code[4] << 1) + (code[5] << 2) + (code[6] << 3);
                        c += (curr4bit << (4 * i));
                    }
                    codeIndexs.add(c);
                }
            }
        } else {
            countEndCode = 0;
            countStartCode = 0;
        }
    }

    /**
     * 去重
     * 将频率串转为编码串，出现多个连续的频率转为一个码值
     * @param codeIndexs
     * @return
     */
    private List<Integer> cleanCodeIndexs(ArrayList<Integer> codeIndexs) {
        List<Integer> list = new ArrayList<>();

        int current;
        int last = -1;
        for (int i = 0; i < codeIndexs.size(); i++) {
            current = i;
            int currentCode = codeIndexs.get(i);

            do {
                i++;
            } while (i < codeIndexs.size() && currentCode == codeIndexs.get(i));

            i--;

            if (i - current >= 2) {     // 至少要有3个连续相同的频率
                currentCode = codeIndexs.get(i);
                if (currentCode != last) {
                    list.add(currentCode);
                    last = currentCode;
                }
            }
        }

        return list;
    }

    private  List<Integer> dummyCodeIndexs(ArrayList<Integer> codeIndexs) {
        List<Integer> list = new ArrayList<>(codeIndexs);
        return list;
    }



    private String showResult(List<Integer> indexs) {
		if (indexs.size() < 2) {
			return null;
		}

		// 还原相邻相同字符
		for (int i=indexs.size()-1; i>0; i--) {
			if (indexs.get(i) == CodeBook.DUPLICATE_INDEX_1 || indexs.get(i) == CodeBook.DUPLICATE_INDEX_2) {
				int temp = i -1;
				while(temp>0 && (indexs.get(temp) == CodeBook.DUPLICATE_INDEX_1 || indexs.get(temp) == CodeBook.DUPLICATE_INDEX_2)) {
					temp --;
					continue;
				}

				indexs.set(i, indexs.get(temp));
			}
		}

		int crcContent0 = indexs.get(indexs.size() -2);
        int crcContent1 = indexs.get(indexs.size() -1);
		int[] crc = Utils.crc(indexs, 0, indexs.size() -3);
        boolean crcResult = (crc[0] == crcContent0 && crc[1] == crcContent1);

		StringBuilder mTextBuilder = new StringBuilder();
		for(int i=0; i<indexs.size() -2; i+=2) {
			char c = Utils.indexs2Char(indexs.get(i), indexs.get(i+1));
			mTextBuilder.append(c);
		}

		String re = mTextBuilder.toString();

		String text = null;
		try {
			text = new String(Base64.decode(re, Base64.NO_WRAP | Base64.NO_PADDING));
		} catch (Exception e) {
			e.printStackTrace();
		}

        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            msg.what = MainActivity.MSG_RESULT;
            msg.obj = indexs.toString() +", crc:"+crcResult + "\n" + text;
            mHandler.sendMessage(msg);
        }

        Log.d(TAG, "showResult:"+"  crc:" + crcResult + "____" + re +"____"+ text);

        return text;

	}

    private String showResult_SeqHamming(List<Integer> raw_indexs) {
        List<Integer> indexs = new ArrayList<Integer>();
        if (raw_indexs.size() < 2) {
            return null;
        }
        // 还原相邻相同字符
        while (raw_indexs.get(0) == CodeBook.DUPLICATE_INDEX_2 || raw_indexs.get(0) == CodeBook.DUPLICATE_INDEX_1) {
            raw_indexs.remove(0);
        }
        int temp = raw_indexs.size();
        for (int i=raw_indexs.size()-1; i>=0; i--) {
            if (raw_indexs.get(i) == CodeBook.DUPLICATE_INDEX_1 || raw_indexs.get(i) == CodeBook.DUPLICATE_INDEX_2) {
                if (temp >= i) {
                    temp = i -1;
                }
                while(temp>0 && (raw_indexs.get(temp) == CodeBook.DUPLICATE_INDEX_1 || raw_indexs.get(temp) == CodeBook.DUPLICATE_INDEX_2)) {
                    temp --;
                }
                if (temp < 0) temp = 0;
                raw_indexs.set(i, raw_indexs.get(temp));
            }
        }
        // 截取字符段进行译码
        for (int i = 0; i < raw_indexs.size(); ++i) {
            while (i < raw_indexs.size() && raw_indexs.get(i) == CodeBook.SEP_INDEX) {
                i++;
            }
            int j = i;
            while (j < raw_indexs.size() && raw_indexs.get(j) != CodeBook.SEP_INDEX) {
                j++;
            }
            if (j > i) {
                List<Integer> sublist = new ArrayList<>(raw_indexs.subList(i, j));
                int[] corrected_units = correct(sublist);
                for (int k = 0; k < 4; ++k) {
                    indexs.add(corrected_units[k]);
                }
            }
            i = j;
        }
        StringBuilder mTextBuilder = new StringBuilder();
        for(int i=0; i<indexs.size(); i+=2) {
            char c = Utils.indexs2Char(indexs.get(i), indexs.get(i+1));
            mTextBuilder.append(c);
        }
        String re = mTextBuilder.toString();
        if(re.endsWith("0")) {
            re = re.replaceAll("0+$", "");
        } else if (re.endsWith("1")) {
            re = re.replaceAll("1+$", "");
        }
        String text = null;
        try {
            text = new String(Base64.decode(re, Base64.NO_WRAP | Base64.NO_PADDING));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            msg.what = MainActivity.MSG_RESULT;
            msg.obj = indexs.toString() + "\n" + text;
            mHandler.sendMessage(msg);
        }
        Log.d(TAG, "showResult:" + re +" --> "+ text);
        return text;
    }

    // 译码及纠错
    private int[] correct(List<Integer> frag) {
        Log.d(TAG, "correct: " + frag);
        int[] indexs = new int[4];
        if (frag.size() < 7) {
            Log.d(TAG, "Loss of units");
            if (frag.size() < 6) {
                while (frag.size() < 7) {
                    frag.add(0);
                }
                process_complete_pack(frag, indexs);
            } else {
                for (int i = 0; i <= 6; ++i) {
                    frag.add(i, 0);
                    int error_pos = process_complete_pack(frag, indexs);
                    if (error_pos == -1 || error_pos == i) {
                        Log.d(TAG, "correct success: " + error_pos + " = " + i);
                        return indexs;
                    }
                    frag.remove(i);
                }
            }
        } else if (frag.size() > 7) {
            Log.d(TAG, "Redundant of units");
            if (frag.size() > 8) {
                frag = frag.subList(0, 7);
                process_complete_pack(frag, indexs);
            } else {
                for (int i = 0; i < 8; ++i) {
                    int tmp = frag.get(i);
                    frag.remove(i);
                    int error_pos = process_complete_pack(frag, indexs);
                    if (error_pos == -1) {
                        Log.d(TAG, "correct success: " + error_pos + " now i = " + i);
                        return indexs;
                    }
                    frag.add(i, tmp);
                }
            }
        }
        else {
            process_complete_pack(frag, indexs);
        }
        return indexs;
    }

    private int process_complete_pack(List<Integer> _frag, int[] indexs) {
        Log.d(TAG, "process: " + _frag);
        List<Integer> frag = new ArrayList<Integer>(_frag);
        int pos = 0;
        int chk1 = (frag.get(0) + frag.get(2) + frag.get(4) + frag.get(6)) % CodeBook.CODE_BOOK_LENGTH_CONTENT;
        int chk2 = (frag.get(1) + frag.get(2) + frag.get(5) + frag.get(6)) % CodeBook.CODE_BOOK_LENGTH_CONTENT;
        int chk3 = (frag.get(3) + frag.get(4) + frag.get(5) + frag.get(6)) % CodeBook.CODE_BOOK_LENGTH_CONTENT;
        int offset = 0;
        if (chk1 != 0) {
            pos += 1;
            offset = chk1;
        }
        if (chk2 != 0) {
            pos += 2;
            offset = chk2;
        }
        if (chk3 != 0) {
            pos += 4;
            offset = chk3;
        }
        pos -= 1;
        if (pos != -1) {
            int error_value = frag.get(pos);
            frag.set(pos, (error_value + CodeBook.CODE_BOOK_LENGTH_CONTENT - offset) % CodeBook.CODE_BOOK_LENGTH_CONTENT);
        }
        Log.d(TAG, "processed: " + frag + "corrected: " + pos);
        indexs[0] = frag.get(2);
        indexs[1] = frag.get(4);
        indexs[2] = frag.get(5);
        indexs[3] = frag.get(6);
        return pos;
    }

    private String showResult_74hamming(List<Integer> codeIndexs) throws UnsupportedEncodingException {
        byte [] bytes = new byte[codeIndexs.size()];
        for (int i = 0; i < codeIndexs.size(); ++i) {
            bytes[i] = codeIndexs.get(i).byteValue();
        }
        String re  = new String(bytes, "UTF-8");
        String text = null;
        try {
            text = new String(Base64.decode(re, Base64.NO_WRAP | Base64.NO_PADDING));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            msg.what = MainActivity.MSG_RESULT;
            msg.obj = debugQueue.toString() + "\n" + text;
            mHandler.sendMessage(msg);
        }

        Log.d(TAG, "showResult:" + "____" + codeIndexs +"____"+ text);

        return text;

    }

}
