package novel.tts.novel_tts.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.regex.*;
@Slf4j
@Component
public class FileParser {

    /**
     * 解析文本文件中的人名和性别
     */
    public static List<Map<String, String>> parse(String filePath) {
        List<Map<String, String>> list = new ArrayList<>();
        // 仅匹配行首的「名字(性别)」形式，冒号前
        Pattern pattern = Pattern.compile("^\\s*([\\u4e00-\\u9fa5]{2,4})\\s*\\((男|女|未知)\\)\\s*[:：]");

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    String name = matcher.group(1);
                    String gender = matcher.group(2) == null ? "未知" : matcher.group(2);
                    Map<String, String> item = new HashMap<>();
                    item.put("name", name);
                    item.put("gender", gender);
                    list.add(item);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
        }

        return list;
    }
}
