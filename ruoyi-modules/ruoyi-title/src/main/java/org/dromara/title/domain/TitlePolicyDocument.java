package org.dromara.title.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("title_policy_document")
public class TitlePolicyDocument {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String version;
    private String publisher;
    private LocalDate effectiveAt;
    private LocalDate expiresAt;
    private String informationLevel;
    private String sourceUrl;
    private String status;
}
