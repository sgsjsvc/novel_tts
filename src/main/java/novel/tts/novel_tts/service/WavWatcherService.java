package novel.tts.novel_tts.service;

public interface WavWatcherService {
    void startWatching(); // 启动监听
    void initialScan();   // 启动时全量扫描
}
