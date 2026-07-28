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
    private String planId;
    private String userId;
    private Integer dayNumber;
    private String title;
    private String description;
    private String taskStatus;
    private LocalDate scheduledDate;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
