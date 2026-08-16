package org.dromara.title.domain;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("title_review_record")
public class TitleReviewRecord { @TableId(type=IdType.ASSIGN_ID) private Long id; private Long applicationId; private Integer versionNo; private Long reviewerUserId; private String reviewNode; private String action; private String reason; private String policyBasis; private LocalDateTime createdAt; }
