package novel.tts.novel_tts.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FolderWatcherMapper {

    @Delete("TRUNCATE TABLE url")
    void deleteAll();

    @Insert("INSERT INTO url(file_name,parent_directory,ur) VALUES (#{fileName}, #{parentDirectory},#{ur})")
    void insertUrl(@Param("fileName") String fileName, @Param("parentDirectory") String parentDirectory,@Param("ur") String ur);

    @Delete("DELETE FROM url WHERE ur = #{url}")
    void delete(String url);
    @Delete("DELETE FROM url WHERE ur LIKE CONCAT(#{prefix}, '%')")
    void deleteByPrefix(@Param("prefix") String prefix);
}
