package novel.tts.novel_tts.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UtilMapper {

    // 修改 getToken 方法，添加 model 参数
    @Select("SELECT token " +
            "FROM api_token " +
            "WHERE (max_concurrency - alive_thread) > 0 " +
            "AND disabled = 0 " +
            "AND model = #{model} " + // 添加 model 筛选条件
            "ORDER BY request_frequency ASC " +
            "LIMIT 1;")
    String getToken(@Param("model") String model); // 使用 @Param 注解绑定参数

    // 修改 getMaxConcurrency 方法，添加 model 参数
    @Select("SELECT IFNULL(SUM(max_concurrency), 0) " +
            "AS total_alive_thread " +
            "FROM api_token " +
            "WHERE model = #{model}") // 添加 model 筛选条件
    int getMaxConcurrency(@Param("model") String model); // 使用 @Param 注解绑定参数

}
