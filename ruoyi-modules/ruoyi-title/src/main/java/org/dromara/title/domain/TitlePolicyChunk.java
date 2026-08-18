package org.dromara.title.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("title_policy_chunk")
public class TitlePolicyChunk {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long documentId;
    private String locator;
    private String content;
    private Integer applicableYear;
    private String titleSeries;
    private String titleLevel;
}
