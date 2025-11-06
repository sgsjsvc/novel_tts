package novel.tts.novel_tts.mapper;


import novel.tts.novel_tts.pojo.Api;
import novel.tts.novel_tts.pojo.DashboardApi;
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

    //获取api总数
    @Select("SELECT COUNT(*) FROM api_token")
    int getTotalAccounts();

    //获取活跃api总数
    @Select("SELECT COUNT(*) FROM api_token WHERE disabled=0")
    int getActiveAccounts();

    //获取api请求频率
    @Select("SELECT IFNULL(SUM(request_frequency), 0) FROM api_token")
    int getRequestFrequency();

    //获取系统线程数
    @Select("SELECT IFNULL(SUM(max_concurrency), 0) FROM api_token")
    int getSystemThreads();

    //获取最大线程数
    @Select("SELECT IFNULL(SUM(alive_thread), 0) FROM api_token")
    int getMaxConcurrency();

    //获取api
    @Select("SELECT name,max_concurrency,alive_thread FROM api_token")
    List<DashboardApi> getDashboardApi();
}

