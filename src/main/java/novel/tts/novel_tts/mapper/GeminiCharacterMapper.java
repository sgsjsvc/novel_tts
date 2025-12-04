package novel.tts.novel_tts.mapper;



import novel.tts.novel_tts.pojo.NovelTable;
import novel.tts.novel_tts.pojo.TableCharacter;
import novel.tts.novel_tts.pojo.TtsCharacter;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GeminiCharacterMapper {

    List<TtsCharacter> getCharacters(String search, String gender, String version);

    @Select("select count(*) from characters where gender=#{gender}")
    Integer cont(String gender);

    @Select("select count(*) from characters")
    int contall();

    int importCharacters(@Param("names") List<String> names, @Param("version") String version, @Param("createdAt") LocalDateTime createdAt);

    @Select("select distinct version from characters")
    List<String> getversions();

    @Select("delete from characters where id=#{id}")
    void deleteCharacter(Integer id);

    @Delete({
            "<script>",
            "DELETE FROM characters WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int deleteBatchByIds(@Param("ids") List<Integer> ids);

    @Update("UPDATE characters SET gender = #{gender} WHERE id = #{id}")
     int updateGenderById(@Param("id") Long id, @Param("gender") String gender);


    List<NovelTable> selectModels(String tableName);



    @Select("select * from `${novelname}`")
    List<TableCharacter> selectAllCharacters(@Param("novelname") String novelname);

}
