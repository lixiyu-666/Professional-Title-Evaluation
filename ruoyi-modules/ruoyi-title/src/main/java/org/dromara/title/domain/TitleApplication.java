package org.dromara.title.domain;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("title_application")
public class TitleApplication { @TableId(type=IdType.ASSIGN_ID) private Long id; private Long batchId; private Long applicantUserId; private String status; private Integer currentVersion; private String formJson; private LocalDateTime correctionDeadline; private LocalDateTime createdAt; private LocalDateTime updatedAt; }
