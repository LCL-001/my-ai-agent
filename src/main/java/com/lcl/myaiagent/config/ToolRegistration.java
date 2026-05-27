package com.lcl.myaiagent.config;

import com.lcl.myaiagent.tools.*;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具注册配置类
 * 负责将所有AI工具类注册为Spring Bean，供Spring AI框架统一管理和调用
 */
@Configuration
public class ToolRegistration {

    /**
     * SearchAPI访问密钥
     * 从配置文件中读取，用于WebSearchTool的身份验证
     */
    @Value("${search-api.api-key}")
    private String searchApiKey;

    /**
     * 注册所有AI工具回调
     * 创建各种工具实例并转换为ToolCallback数组，供AI助手调用
     * 包括文件操作、网络搜索、网页抓取、资源下载、终端操作和PDF生成等工具
     *
     * @return ToolCallback[] 包含所有已注册工具的回调数组
     */
    @Bean
    public ToolCallback[] allTools() {
        // 创建各个工具实例
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
//        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        AskHumanTool askHumanTool = new AskHumanTool();
        TerminateTool terminateTool = new TerminateTool();
        // 将所有工具转换为Spring AI的ToolCallback数组
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
//                terminalOperationTool,
                pdfGenerationTool,
                askHumanTool,
                terminateTool
        );
    }
}
