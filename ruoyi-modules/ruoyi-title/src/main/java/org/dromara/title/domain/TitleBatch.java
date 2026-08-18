package org.dromara.title.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("title_batch")
public class TitleBatch {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Integer evaluationYear;
    private String titleSeries;
    private String titleLevel;
    private String applicationType;
    private LocalDateTime openAt;
    private LocalDateTime firstSubmitDeadline;
    private Integer defaultCorrectionHours;
    private String ruleVersion;
    /** Material checklist, policy association and qualification notes for this release. */
    private String configJson;
    private Boolean published;
    private LocalDateTime createdAt;
}
