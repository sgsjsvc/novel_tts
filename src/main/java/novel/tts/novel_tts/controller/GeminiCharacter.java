package novel.tts.novel_tts.controller;

import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.common.Result;
import novel.tts.novel_tts.mapper.GeminiCharacterMapper;
import novel.tts.novel_tts.pojo.CharacterResponse;
import novel.tts.novel_tts.pojo.ModelRequest;
import novel.tts.novel_tts.pojo.TtsCharacter;
import novel.tts.novel_tts.pojo.Statistics;
import novel.tts.novel_tts.service.GeminiCharacterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("gemini/characters")
@Slf4j

public class GeminiCharacter {

    @Autowired
    private GeminiCharacterService geminiCharacterService;
    @Autowired
    private GeminiCharacterMapper geminiCharacterMapper;

    @GetMapping("")
    public Result<CharacterResponse> getCharacters(@RequestParam(required = false) String search, @RequestParam(required = false) String gender, @RequestParam(required = false) String version) {
        log.info("前端传入参数：search= {}, gender= {}, version= {}", search, gender, version);
        log.info("获取所有角色");
        List<TtsCharacter> characters = geminiCharacterService.getCharacters(search, gender, version);
        log.info("返回角色:{}", characters);
        // 获取统计信息（由数据库统计）
        Statistics stats = geminiCharacterService.getCharacterStatistics();
        List<String> versions = geminiCharacterMapper.getversions();
        CharacterResponse response = new CharacterResponse(characters, stats, versions);
        return Result.success(response);
    }

    @PostMapping("/import")
    public Result<Integer> forwardToFastAPI(@RequestBody ModelRequest modelRequest) {

        int importcount = geminiCharacterService.importCharacters(modelRequest);
        log.info("导入角色数量:{}", importcount);
        return Result.success(importcount);
    }

    @DeleteMapping("/{id}")
    public Result<TtsCharacter> deleteCharacter(@PathVariable Integer id) {
        log.info("删除角色:{}", id);
        geminiCharacterMapper.deleteCharacter(id);
        log.info("删除角色成功");
        return Result.success();
    }

    @PostMapping("/batch_delete")
    public Result<?> batchDelete(@RequestBody Map<String, List<Integer>> body) {
        log.info("批量删除角色:{}", body);
        List<Integer> ids = body.get("ids");
        // 调用 Service 删除
        int deletedCount = geminiCharacterMapper.deleteBatchByIds(ids);
        log.info("成功删除 {} 条记录", deletedCount);
        return Result.success("成功删除 " + deletedCount + " 条记录");
    }

    @PutMapping("/{id}")
    public Result<?> updateCharacterGender(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        log.info("更新角色性别:{}", body);
        String gender = (String) body.get("gender");
        int updated = geminiCharacterMapper.updateGenderById(id, gender);
        if (updated > 0) {
            log.info("更新成功");
            return Result.success("更新成功");
        } else {
            log.info("更新失败");
            return Result.error("更新失败，未找到该角色");
        }
    }


}
