package novel.tts.novel_tts.util;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Slf4j
@Component
public class GetTableName {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();
    static {
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
    }

    /**
     * 对字符串做预处理：非字母数字和中文字符替换为下划线
     */
    private static String preprocess(String text) {
        return text.replaceAll("[^\\u4E00-\\u9FA5a-zA-Z0-9]", "");
    }

    /**
     * 对最终结果做后处理：只保留字母数字和下划线
     */
    private static String postprocess(String text) {
        return text.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * 中文转安全拼音
     */
    public static String toSafePinyin(String text) {
        String pre = preprocess(text); // 预处理
        StringBuilder pinyin = new StringBuilder();

        try {
            for (char c : pre.toCharArray()) {
                if (Character.toString(c).matches("[\\u4E00-\\u9FA5]")) {
                    String[] arr = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
                    if (arr != null && arr.length > 0) {
                        pinyin.append(arr[0]);
                    }
                } else if (Character.isLetterOrDigit(c)) {
                    pinyin.append(c);
                } else {
                    pinyin.append('_');
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return postprocess(pinyin.toString()); // 后处理
    }

    /**
     * 获取首字母缩写
     */
    public static String getInitials(String text) {
        String pre = preprocess(text);
        StringBuilder initials = new StringBuilder();

        try {
            for (char c : pre.toCharArray()) {
                if (Character.toString(c).matches("[\\u4E00-\\u9FA5]")) {
                    String[] arr = PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
                    if (arr != null && arr.length > 0) {
                        initials.append(arr[0].charAt(0));
                    }
                } else if (Character.isLetterOrDigit(c)) {
                    initials.append(c);
                } else {
                    initials.append('_');
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return postprocess(initials.toString()).toUpperCase();
    }

    public  String TableName(String text) {
        String initials = getInitials(text);
        log.info("开始生成表名");
        return initials + "_" + UUID.randomUUID().toString().replace("-", "");
    }

}
