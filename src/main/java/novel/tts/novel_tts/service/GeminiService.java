package novel.tts.novel_tts.service;

import novel.tts.novel_tts.pojo.Api;
import novel.tts.novel_tts.pojo.Dashboard;

import java.util.List;

public interface GeminiService {
    void creatapi(Api api);
    void updateApi(Api api);

    List<Api> getApi();

    void updateStatus(Integer id);

    void deleteApi(Integer id);

    Dashboard getSystemConfig();
}
