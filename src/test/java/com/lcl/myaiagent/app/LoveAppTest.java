package com.lcl.myaiagent.app;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 恋爱应用测试类
 * 用于测试LoveApp的各项聊天功能，包括带报告的聊天、普通聊天和RAG增强聊天
 */
@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    /**
     * 测试带报告的聊天功能
     * 验证系统能够根据用户问题生成恋爱报告
     */
    @Test
    void doChatWithReport() {
        String conversationId = UUID.randomUUID().toString();
        // 第一轮对话：用户提出情感问题
        String message = "你好，我是程序员鱼皮，我想让另一半（编程导航）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, conversationId);
        Assertions.assertNotNull(loveReport);
    }

    /**
     * 测试多轮对话的记忆功能
     * 验证系统能够在多轮对话中保持上下文记忆，正确回忆之前提到的信息
     */
    @Test
    void testChat() {
        String conversationId = UUID.randomUUID().toString();
        // 第一轮：用户自我介绍
        String message = "你好，我是程序员鱼皮";
        String answer = loveApp.doChat(message, conversationId);
        Assertions.assertNotNull(answer);
        // 第二轮：用户表达情感需求
        message = "我想让另一半（编程导航）更爱我";
        answer = loveApp.doChat(message, conversationId);
        Assertions.assertNotNull(answer);
        // 第三轮：测试系统是否能记住之前提到的另一半信息
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, conversationId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 测试基于RAG（检索增强生成）的聊天功能
     * 验证系统能够从知识库中检索相关信息来回答用户的情感问题
     */
    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String answer =  loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }



    @Test
    void doChatWithTools() {
//        // 测试联网搜索问题的答案
//        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");
//
//        // 测试网页抓取：恋爱案例分析
//        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");
//
//        // 测试资源下载：图片下载
//        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");
//
//        // 测试终端操作：执行代码
//        testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }


    @Test
    void doChatWithMcp() {
        // 测试图片搜索 MCP
        String chatId = UUID.randomUUID().toString();
        String message = "帮我搜索一些哄另一半开心的图片";
        String answer =  loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }


}











//package com.lcl.myaiagent.app;
//
//import cn.hutool.core.lang.UUID;
//import com.alibaba.cloud.ai.graph.RunnableConfig;
//import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
//import jakarta.annotation.Resource;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class LoveAppTest {
//    @Resource
//    private LoveApp loveApp;
//
//    @Test
//    void doChat() throws GraphRunnerException {
//        String threadId = UUID.randomUUID().toString();
//        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
//        String message = "你好，我是lcl";
//        String content = loveApp.doChat(message, runnableConfig);
//        Assertions.assertNotNull(content);
//        message = "我想让我的另一半（dlx）更爱我";
//        content = loveApp.doChat(message, runnableConfig);
//        Assertions.assertNotNull(content);
//        message = "我的另一半叫什么来着？刚才告诉过你来着，帮我回忆一下";
//        content = loveApp.doChat(message, runnableConfig);
//        Assertions.assertNotNull(content);
//    }
//}
