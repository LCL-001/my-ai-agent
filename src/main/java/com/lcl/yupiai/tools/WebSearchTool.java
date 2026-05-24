package com.lcl.yupiai.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网络搜索工具类
 * 提供基于SearchAPI的网络搜索功能，支持通过百度搜索引擎获取信息
 * 供AI助手调用以获取实时网络数据
 */
public class WebSearchTool {

    /**
     * SearchAPI的搜索接口地址
     * 用于发送搜索请求并获取搜索结果
     */
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    /**
     * SearchAPI访问密钥
     * 用于身份验证和授权搜索请求
     */
    private final String apiKey;

    /**
     * 构造网络搜索工具实例
     *
     * @param apiKey SearchAPI的访问密钥，用于认证搜索请求
     */
    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 执行网络搜索操作
     * 通过SearchAPI调用百度搜索引擎，获取与查询关键词相关的搜索结果
     * 返回前5条最相关的搜索结果，格式化为JSON字符串
     *
     * @param query 搜索查询关键词，用于指定要搜索的内容
     * @return String 搜索结果的JSON字符串，包含前5条有机搜索结果；如果搜索失败则返回错误信息
     */
    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        // 构建搜索请求参数，包括查询词、API密钥和搜索引擎类型
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            // 解析响应JSON并提取前5条有机搜索结果
            JSONObject jsonObject = JSONUtil.parseObj(response);
            // 提取 organic_results 部分
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            List<Object> objects = organicResults.subList(0, 5);
            // 将搜索结果对象转换为JSON字符串并用逗号拼接
            String result = objects.stream().map(obj -> {
                JSONObject tmpJSONObject = (JSONObject) obj;
                return tmpJSONObject.toString();
            }).collect(Collectors.joining(","));
            return result;
        } catch (Exception e) {
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}
