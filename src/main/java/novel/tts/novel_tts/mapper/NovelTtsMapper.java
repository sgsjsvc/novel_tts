package novel.tts.novel_tts.mapper;

import novel.tts.novel_tts.pojo.Folder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NovelTtsMapper {
    //获取小说列表
    @Select("SELECT DISTINCT parent_directory\n" +
            "FROM url\n" +
            "WHERE ur LIKE 'temp\\\\\\\\output\\\\\\\\txt%';")
    List<String> getNovelList();

    //获取章节列表
    @Select("""
            SELECT *
            FROM url
            WHERE REPLACE(ur, '\\\\', '/') LIKE CONCAT(#{prefix}, '%')
            """)
    List<Folder> getChapterList(String prefix);

}
