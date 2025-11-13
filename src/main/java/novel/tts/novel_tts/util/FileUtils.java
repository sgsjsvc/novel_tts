package novel.tts.novel_tts.util;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class FileUtils {

    /**
     * 统计 txt 文件行数
     */
    public static long countTxtLines(Path txtPath) {
        try (Stream<String> lines = Files.lines(txtPath)) {
            return lines.count();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * 统计某文件夹下 .wav 文件数量
     */
    public static long countWavFiles(Path folderPath) {
        if (!Files.exists(folderPath)) return 0;
        try (Stream<Path> paths = Files.walk(folderPath, 1)) {
            return paths.filter(p -> p.toString().endsWith(".wav")).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
