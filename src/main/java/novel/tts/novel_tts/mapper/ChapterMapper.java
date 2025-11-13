package novel.tts.novel_tts.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChapterMapper {
    /**
     * ✅ 根据 txt 路径更新解析状态（自动兼容 \\ 和 /）
     */
    @Update("""
        UPDATE url
        SET status = #{status}
        WHERE REPLACE(ur, '\\\\', '/') = REPLACE(#{txtPath}, '\\\\', '/')
        """)
    void updateChapterStatus(@Param("txtPath") String txtPath, @Param("status") int status);

    /**
     * ✅ 判断章节是否存在（兼容路径分隔符）
     */
    @Select("""
        SELECT COUNT(*) 
        FROM url 
        WHERE REPLACE(ur, '\\\\', '/') = REPLACE(#{txtPath}, '\\\\', '/')
        """)
    int existsByTxtPath(@Param("txtPath") String txtPath);

}
