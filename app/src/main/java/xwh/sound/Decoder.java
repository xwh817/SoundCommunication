package xwh.sound;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2017/11/28.
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

    private Handler mHandler;

    private long lastStartTime;
    private static final int TIMEOUT = 10000;

    private static final int COUNT_STEP_SIZE = 10;

    private LinkedList<Integer> listBufferFreq = new LinkedList<>();

    public Decoder(Handler handler) {
        this.mHandler = handler;
        codeIndexs = new ArrayList<>();
    }


    /**
     * 对一个录音Buffer进行频率统计
     */
    public int countFreq(short[] datas, int sampleStep) {
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

                //Log.i("Record", "waveCount:" + waveCount + ", fre:" + fre + ", decode:" + CodeBook.decode(fre));

                decodeFre(currentFreq);

                stepCount = 0;
                waveCount = 0;
            }

        }


        if (mHandler != null) {

            /*if (listBufferFreq.size() >= sampleStep) {
                listBufferFreq.remove(0);
            }
            listBufferFreq.add(bufferFreqCount);

            int allCount = 0;
            for(int bCount : listBufferFreq) {  // 累计最近1s采样得到频率
                allCount += bCount;
            }

            int freq = allCount / 2;  // 一上一下表示一个波形，所以要除以2*/

            //int freq = currentFreq;
            int freq = bufferFreqCount * sampleStep / 2;  // 一上一下表示一个波形，所以要除以2

            Message msg = mHandler.obtainMessage();
            msg.what = MainActivity.MSG_CURRENT_FREQ;
            msg.obj = freq + "";
            mHandler.sendMessage(msg);
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

            Log.i("Record", "waveCount:" + waveCount + ", fre:" + fre + ", decode:" + codeIndex);

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

}
