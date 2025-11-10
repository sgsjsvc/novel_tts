package novel.tts.novel_tts.mapper;

import org.apache.ibatis.annotations.*;

public interface PersonMapper {

    // 自动建表（目标表）
    @Update("""
                CREATE TABLE IF NOT EXISTS ${tableName} (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50) NOT NULL UNIQUE,
                    gender VARCHAR(10) DEFAULT '未知',
                    character_name VARCHAR(50) NOT NULL UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    version VARCHAR(10) DEFAULT 'v2'
                )
            """)
    void createTableIfNotExists(@Param("tableName") String tableName);


    // 插入数据（保证 character_name 唯一）
    @Insert("""
                INSERT INTO ${tableName}(name, gender, character_name,version)
                VALUES(#{name}, #{gender}, #{characterName},#{version})
            """)
    void insertPerson(@Param("tableName") String tableName,
                      @Param("name") String name,
                      @Param("gender") String gender,
                      @Param("characterName") String characterName,
                      @Param("version") String version);

    @Select("""
                SELECT modelname
                FROM characters
                WHERE gender = #{gender} and version=#{version}
                  AND modelname NOT IN (SELECT character_name FROM ${tableName})
                ORDER BY RAND()
                LIMIT 1
            """)
    String getUnusedRandomCharacter(@Param("tableName") String tableName,
                                    @Param("gender") String gender,
                                    @Param("version") String version);

    @Insert("insert into novel_table(novel_name, tableName) values (#{novelName},#{tableName})")
    void insertNovelTable(@Param("novelName") String novelName,
                          @Param("tableName") String tableName);

    @Select("select tableName from novel_table where novel_name=#{novelName}")
    String getTableName(@Param("novelName") String novelName);


    @Select("select name from ${tableName} where name=#{name}")
    String getName(@Param("tableName") String tableName,
                   @Param("name") String name);

    @Select("select character_name from ${tableName} where name= #{Name}")
    String getCharacterName(@Param("tableName") String tableName,
                            @Param("Name") String Name);
}
