package novel.tts.novel_tts.service;

import java.nio.file.Path;

public interface FolderWatcherService {
    void insert(String relativePath);

    void delete(String child);
}
