package com.lcl.myaiagent.model.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long documentId;
    private String userId;
    private Integer chunkIndex;
    private String content;
    private String vectorDocumentId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
}
