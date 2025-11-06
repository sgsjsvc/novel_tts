package novel.tts.novel_tts.controller;


import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.common.Result;
import novel.tts.novel_tts.pojo.Api;
import novel.tts.novel_tts.pojo.Dashboard;
import novel.tts.novel_tts.service.GeminiService;
import novel.tts.novel_tts.util.GeminiConcurrentProcessor;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("gemini")
@Slf4j
public class Gemini {
    @Autowired
    private GeminiService geminiService;
    @Autowired
    private GeminiConcurrentProcessor geminiProcessor;
    //插入api
    @PostMapping("/accounts")
    public Result<Api> creatapi(@RequestBody Api api){
        log.info("插入api:{}",api);
        geminiService.creatapi(api);
        log.info("插入api成功");
        return Result.success();
    }
    //获取api
    @GetMapping("/accounts")
    public Result<List<Api>> getApi() {
        log.info("获取api");
        List<Api> api = geminiService.getApi();
        log.info("返回api:{}",api);
        return Result.success(api);
    }
    //更新api
    @PutMapping("/accounts/{id}")
    public Result<Api> updateApi(@PathVariable Integer id, @RequestBody Api api) {
        log.info("更新api:{}", api);
        api.setId(id);
        geminiService.updateApi(api);
        log.info("更新api成功");
        return Result.success();
    }
    //删除api
    @DeleteMapping("/accounts/{id}")
    public Result<Api> deleteApi(@PathVariable Integer id) {
        log.info("删除api:{}", id);
        geminiService.deleteApi(id);
        log.info("删除api成功");
        return Result.success();
    }
    //更新api状态
    @PostMapping("/toggle_account/{id}")
    public Result<Api> updateStatus(@PathVariable Integer id) {
        log.info("开始更新api状态:{}", id);
        geminiService.updateStatus(id);
        log.info("更新api状态成功");
        return Result.success();
    }

    @GetMapping
    public Result<String> get() throws IOException, InterruptedException {
        geminiProcessor.process("input1.txt", "output1.txt", "gemini-2.5-flash");
        return Result.success("gemini");
    }
    @GetMapping("/dashboard")
    public Result<Dashboard> getSystemConfig() {
        log.info("获取系统配置");
        return Result.success(geminiService.getSystemConfig());
    }
}
