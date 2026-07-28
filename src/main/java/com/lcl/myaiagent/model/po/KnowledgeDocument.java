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

    @TableField("user_id")
    private String userId;
    private String name;

    @TableField("document_type")
    private String documentType;

    @TableField("storage_path")
    private String storagePath;

    @TableField("content_text")
    private String contentText;
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField("is_delete")
    @TableLogic
    private Boolean isDelete;
}
