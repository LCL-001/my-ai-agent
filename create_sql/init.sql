drop database if exists `my-ai-agent`;
create database `my-ai-agent`;

drop table if exists `chat_message`;

create table if not exists `chat_message`
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    conversation_id varchar(64)                          not null comment '会话ID',
    message_type    varchar(20)                          not null comment '消息类型',
    content         text                                 not null comment '消息内容',
    metadata        text                                 not null comment '元数据',
    create_time     datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete       tinyint(1) default 0                 not null comment '是否删除 0-未删除 1-已删除'
)
    comment '聊天消息表';

create index idx_conversation_id
    on chat_message (conversation_id);

