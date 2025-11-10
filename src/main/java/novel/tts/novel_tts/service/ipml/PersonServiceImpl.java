package novel.tts.novel_tts.service.ipml;

import lombok.extern.slf4j.Slf4j;
import novel.tts.novel_tts.mapper.PersonMapper;
import novel.tts.novel_tts.service.PersonService;
import novel.tts.novel_tts.util.FileParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class PersonServiceImpl implements PersonService {

    @Autowired
    private PersonMapper personMapper;

    public void processFile(String filePath, String tableName,String  version) {


        // 解析文件
        List<Map<String, String>> persons = FileParser.parse(filePath);

        // 写入数据库
        for (Map<String, String> person : persons) {
            String name = person.get("name");
            String gender = person.get("gender");

            // 如果已存在则跳过
            if (personMapper.getName(tableName, name) != null) continue;

            // 获取未使用角色
            String characterName = personMapper.getUnusedRandomCharacter(tableName, gender, version);
            if (characterName == null) {
                log.warn("⚠️ 无可用角色名，跳过 {} ({})", name, gender);
                continue;
            }

            try {
                personMapper.insertPerson(tableName, name, gender, characterName,version);
                log.info("✅ 插入 {} -> {} ({})", tableName, name, gender);
            } catch (DuplicateKeyException e) {
                log.warn("⚠️ 跳过重复插入：{}", name);
            }
        }

    }
}
