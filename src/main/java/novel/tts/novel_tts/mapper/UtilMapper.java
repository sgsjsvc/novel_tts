package novel.tts.novel_tts.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UtilMapper {

    @Select("SELECT token\n" +
            "FROM api_token\n" +
            "WHERE (max_concurrency - alive_thread) > 0\n" +
            "AND disabled = 0\n" +
            "ORDER BY request_frequency ASC\n" +
            "LIMIT 1;\n")
    String getToken();

    @Select("SELECT IFNULL(SUM(max_concurrency), 0) " +
            "AS total_alive_thread\n" +
            "FROM api_token")
    int getMaxConcurrency();
}
