package novel.tts.novel_tts;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("novel.tts.novel_tts.mapper")

@SpringBootApplication

public class NovelTtsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovelTtsApplication.class, args);
    }

}
