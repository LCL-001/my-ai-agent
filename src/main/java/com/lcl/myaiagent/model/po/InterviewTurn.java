package com.lcl.myaiagent.model.po;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.util.Date;
@Data @TableName("interview_turn") public class InterviewTurn { @TableId private String id; private String sessionId; private Integer turnNumber; private String question; private String answer; private String feedback; private Integer score; private String turnStatus; @TableField(value="create_time",fill=FieldFill.INSERT) private Date createTime; @TableField(value="update_time",fill=FieldFill.INSERT_UPDATE) private Date updateTime; }
