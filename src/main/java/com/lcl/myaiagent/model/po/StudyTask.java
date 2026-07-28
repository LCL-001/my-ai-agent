package com.lcl.myaiagent.model.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("study_task")
public class StudyTask {
    @TableId
    private String id;

    @TableField("plan_id")
    private String planId;

    @TableField("user_id")
    private String userId;

    @TableField("day_number")
    private Integer dayNumber;
    private String title;
    private String description;

    @TableField("task_status")
    private String taskStatus;

    @TableField("scheduled_date")
    private LocalDate scheduledDate;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
