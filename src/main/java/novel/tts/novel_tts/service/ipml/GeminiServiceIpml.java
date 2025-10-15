package novel.tts.novel_tts.service.ipml;


import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.GeminiMapper;
import novel.tts.novel_tts.pojo.Api;
import novel.tts.novel_tts.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GeminiServiceIpml implements GeminiService {
    @Autowired
    private  GeminiMapper geminiMapper;

    @Override
    public void creatapi(Api api) {
        log.info("开始插入数据");
        geminiMapper.insertapi(api);
    }

    @Override
    public void updateApi(Api api) {
        log.info("开始更新数据");
        geminiMapper.updateApi(api);
    }

    @Override
    public List<Api> getApi() {
        log.info("开始获取数据");
        return geminiMapper.getApi();
    }

    @Override
    public void updateStatus(Integer id) {
        log.info("获取api旧状态");
        int status = geminiMapper.getStatus(id);
        if (status == 1) {
            log.info("api状态为1，更新为0");
            geminiMapper.updateStatus(id, 0);
        } else {
            log.info("api状态为0，更新为1");
            geminiMapper.updateStatus(id, 1);
        }
    }

    @Override
    public void deleteApi(Integer id) {
        log.info("开始删除数据");
        geminiMapper.deleteApi(id);
    }


}
