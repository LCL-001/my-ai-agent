package com.lcl.myaiagent.model.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("study_plan")
public class StudyPlan {
    @TableId
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("gap_analysis_id")
    private String gapAnalysisId;
    private String title;
    private String status;

    @TableField("confirm_token")
    private String confirmToken;

    @TableField("draft_note")
    private String draftNote;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
