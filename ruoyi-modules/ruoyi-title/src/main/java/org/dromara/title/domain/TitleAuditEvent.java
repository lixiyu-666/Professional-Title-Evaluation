package org.dromara.title.domain;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("title_audit_event")
public class TitleAuditEvent { @TableId(type=IdType.ASSIGN_ID) private Long id; private Long applicationId; private Long actorUserId; private String actorRole; private String eventType; private String beforeStatus; private String afterStatus; private Integer versionNo; private String reason; private LocalDateTime occurredAt; }
