package com.lcl.myaiagent.model.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String userId;
    private String name;
    private String documentType;
    private String storagePath;
    private String contentText;
    private String status;
    private String errorMessage;
    private Integer chunkCount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Boolean isDelete;
}
