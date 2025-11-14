package novel.tts.novel_tts.config;

import novel.tts.novel_tts.util.logws.AccessLogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileWebConfig implements WebMvcConfigurer {

    @Autowired
    private AccessLogInterceptor accessLogInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        Path folderPath = currentDir.resolve("temp");

        registry.addResourceHandler("/temp/**")
                .addResourceLocations("file:" + folderPath.toAbsolutePath().toString() + "/")
                .setCachePeriod(3600);

        // Add a new resource handler for audio files to be accessed via /api/audio/**
        Path audioPath = currentDir.resolve("temp/output/audio");
        registry.addResourceHandler("/api/audio/**")
                .addResourceLocations("file:" + audioPath.toAbsolutePath().toString() + "/")
                .setCachePeriod(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 只拦截 /temp/** 静态资源访问
        registry.addInterceptor(accessLogInterceptor).addPathPatterns("/temp/**");
    }
}
