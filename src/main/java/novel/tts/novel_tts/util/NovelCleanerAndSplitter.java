package novel.tts.novel_tts.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;

/**
 * 小说清洗 + 按章节分割工具（强化版）
 *
 * 功能：
 *  - 删除空格行、空白行、仅标点行
 *  - 一句话一行（按句号、问号、感叹号切分）
 *  - 引号对话独立成行
 *  - 仅从第一个“第X章”开始保存
 *  - 文件命名格式：001_第X章.txt
 */
@Slf4j
@Component
public class NovelCleanerAndSplitter {

    // 匹配章节标题
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("第[一二三四五六七八九十百千万0-9]+章[^\\n]*");

    // 匹配引号对话
    private static final Pattern DIALOGUE_PATTERN = Pattern.compile("“[^”]+”");

    // 匹配仅标点符号或空格的行（中文标点、英文标点、全角空格）
    private static final Pattern INVALID_LINE_PATTERN =
            Pattern.compile("^[\\p{Punct}·。？！——…、；：‘’“”\\s　]+$");

//    public static void main(String[] args) {
//        String inputFile = "input.txt";   // 输入小说文本路径
//        String outputDir = "output/";     // 输出目录
//        processNovel(inputFile, outputDir);
//    }

    /**
     * 主流程：清洗 + 分章节
     */
    public static void processNovel(String inputPath, String outputDir) {
        try {
            Files.createDirectories(Paths.get(outputDir));
            Path tempCleanFile = Paths.get(outputDir, "cleaned_temp.txt");

            log.info("\uD83E\uDDE9 开始清洗文本...");
            cleanText(Paths.get(inputPath), tempCleanFile);
            log.info("✅ 清洗完成，输出文件：" + tempCleanFile);

            log.info("🔹 开始按章节分割...");
            splitByChapters(tempCleanFile, outputDir);
            log.info("✅ 按章节完成，共" + outputDir + "章");

            Files.deleteIfExists(tempCleanFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Step 1. 清洗文本：
     *  - 删除空白、仅标点、仅空格行
     *  - 对话独立行
     *  - 普通句子按句号问号感叹号分句
     */
    private static void cleanText(Path input, Path output) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 去除首尾空格和全角空格
                line = line.replaceAll("[\\s　]+", " ").trim();
                if (line.isEmpty()) continue;
                if (INVALID_LINE_PATTERN.matcher(line).matches()) continue;

                Matcher matcher = DIALOGUE_PATTERN.matcher(line);
                int lastEnd = 0;

                while (matcher.find()) {
                    // 处理对话前的叙述部分
                    String before = line.substring(lastEnd, matcher.start()).trim();
                    if (!before.isEmpty() && !INVALID_LINE_PATTERN.matcher(before).matches()) {
                        splitAndWriteSentences(before, writer);
                    }

                    // 输出对话句
                    String dialogue = matcher.group().trim();
                    if (!dialogue.isEmpty()) {
                        writer.write(dialogue);
                        writer.newLine();
                    }

                    lastEnd = matcher.end();
                }

                // 处理剩余的叙述部分
                if (lastEnd < line.length()) {
                    String after = line.substring(lastEnd).trim();
                    if (!after.isEmpty() && !INVALID_LINE_PATTERN.matcher(after).matches()) {
                        splitAndWriteSentences(after, writer);
                    }
                }
            }
        }
    }

    /**
     * 按中文句号、问号、感叹号拆句
     */
    private static void splitAndWriteSentences(String text, BufferedWriter writer) throws IOException {
        // 使用正则按句末标点拆分
        String[] sentences = text.split("(?<=[。！？])");
        for (String s : sentences) {
            s = s.trim();
            if (s.isEmpty()) continue;
            if (INVALID_LINE_PATTERN.matcher(s).matches()) continue; // 去除纯标点句
            writer.write(s);
            writer.newLine();
        }
    }

    /**
     * Step 2. 按章节标题分割，仅从第一个章节开始
     */
    private static void splitByChapters(Path input, String outputDir) throws IOException {
        BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8);

        String line;
        StringBuilder currentChapter = new StringBuilder();
        String currentTitle = null;
        int chapterCount = 0;
        boolean started = false; // 是否已遇到第一章

        while ((line = reader.readLine()) != null) {
            Matcher chapterMatcher = CHAPTER_PATTERN.matcher(line);
            if (chapterMatcher.find()) {
                started = true; // 遇到第一个章节标题才开始写
                if (currentTitle != null) {
                    saveChapter(outputDir, ++chapterCount, currentTitle, currentChapter.toString());
                    currentChapter.setLength(0);
                }
                currentTitle = chapterMatcher.group().trim();
                currentChapter.append(currentTitle).append("\n");
            } else if (started) {
                // 仅在章节开始后追加内容
                currentChapter.append(line).append("\n");
            }
        }

        // 保存最后一章
        if (started && currentTitle != null && currentChapter.length() > 0) {
            saveChapter(outputDir, ++chapterCount, currentTitle, currentChapter.toString());
        }

        reader.close();
    }

    /**
     * 写出单个章节文件
     */
    private static void saveChapter(String outputDir, int index, String title, String content) throws IOException {
        // 清理文件名中的非法字符
        String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
        String filename = String.format("%03d_%s.txt", index, safeTitle);
        Path filePath = Paths.get(outputDir, filename);

        Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
        log.info("📘 导出章节：" + filename);
    }
}

