package novel.tts.novel_tts.mapper;


import novel.tts.novel_tts.pojo.Api;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GeminiMapper {
    //插入api
    @Insert("INSERT INTO api_token(id,name,token,note,min_interval,max_concurrency,disabled) " +
            "VALUES(#{id}, #{name}, #{token}, #{note}, #{minInterval}, #{maxConcurrency}, #{disabled})")
    void insertapi(Api api);

    //更新api
    @Update("UPDATE api_token SET name=#{name}, token=#{token}, note=#{note}, " +
            "min_interval=#{minInterval}, max_concurrency=#{maxConcurrency}, disabled=#{disabled} " +
            "WHERE id=#{id}")
    void updateApi(Api api);

    //获取api
    @Select("SELECT * FROM api_token")
    List<Api> getApi();

    //更新api状态
    @Select("SELECT disabled FROM api_token WHERE id=#{id}")
    int getStatus(Integer id);

    //更新api状态
    @Update("UPDATE api_token SET disabled=#{i} WHERE id=#{id}")
    void updateStatus(Integer id, int i);

    //删除api
    @Delete("DELETE FROM api_token WHERE id=#{id}")
    void deleteApi(Integer id);
}

