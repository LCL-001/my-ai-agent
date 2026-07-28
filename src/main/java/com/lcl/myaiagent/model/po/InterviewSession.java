package com.lcl.myaiagent.model.po;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.util.Date;
@Data @TableName("interview_session") public class InterviewSession { @TableId private String id; @TableField("user_id") private String userId; private String status; private String topic; @TableField("question_limit") private Integer questionLimit; @TableField(value="create_time",fill=FieldFill.INSERT) private Date createTime; @TableField(value="update_time",fill=FieldFill.INSERT_UPDATE) private Date updateTime; }
