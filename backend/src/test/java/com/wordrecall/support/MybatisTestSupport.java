package com.wordrecall.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wordrecall.domain.LearningRecord;
import com.wordrecall.domain.User;
import com.wordrecall.domain.Word;
import com.wordrecall.domain.Wordbook;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * MyBatis-Plus resolves lambda column references through TableInfo metadata that is
 * normally built by mapper scanning. Plain Mockito tests have no Spring context, so
 * they must register the entities themselves before building any wrapper.
 */
public final class MybatisTestSupport {

    private MybatisTestSupport() {
    }

    public static void initAll() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "wordrecall-test");
        TableInfoHelper.initTableInfo(assistant, Wordbook.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, Word.class);
        TableInfoHelper.initTableInfo(assistant, LearningRecord.class);
    }
}
