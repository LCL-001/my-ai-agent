package com.lcl.myaiagent.config;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 面试准备智能体的工具注册配置。
 *
 * <p>默认不向模型开放本地文件、网页抓取、下载或终端能力。后续功能只会注册
 * 已通过当前登录用户完成归属校验的资料检索和计划草稿工具。</p>
 */
@Configuration
public class ToolRegistration {

    @Bean
    public ToolCallback[] allTools() {
        return new ToolCallback[0];
    }
}
