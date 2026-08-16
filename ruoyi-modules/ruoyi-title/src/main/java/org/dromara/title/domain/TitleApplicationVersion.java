package org.dromara.title.domain;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("title_application_version")
public class TitleApplicationVersion { @TableId(type=IdType.ASSIGN_ID) private Long id; private Long applicationId; private Integer versionNo; private String formSnapshot; private String materialSnapshot; private String precheckSnapshot; private LocalDateTime submittedAt; }
