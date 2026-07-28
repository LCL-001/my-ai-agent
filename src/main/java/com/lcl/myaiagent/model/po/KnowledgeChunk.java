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

    @TableField("document_id")
    private Long documentId;

    @TableField("user_id")
    private String userId;

    @TableField("chunk_index")
    private Integer chunkIndex;
    private String content;

    @TableField("vector_document_id")
    private String vectorDocumentId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
}
