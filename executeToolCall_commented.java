/**
 * 执行工具调用
 * 
 * <p>该方法负责处理助手消息中的所有工具调用请求，依次执行每个工具并收集结果。
 * 主要流程包括：获取工具回调列表、解析工具参数、查找对应工具、执行工具调用、
 * 记录观测数据以及处理异常情况。</p>
 *
 * <p>对于每个工具调用，方法会：</p>
 * <ul>
 *   <li>验证并处理工具参数，空参数会使用默认空JSON对象</li>
 *   <li>从提示词选项或解析器中查找对应的工具回调</li>
 *   <li>根据工具元数据确定是否直接返回结果</li>
 *   <li>在观测框架下执行工具调用并捕获异常</li>
 *   <li>收集所有工具响应并封装返回</li>
 * </ul>
 *
 * @param prompt          提示词对象，包含聊天选项和工具回调配置
 * @param assistantMessage 助手消息，包含需要执行的工具调用列表
 * @param toolContext     工具上下文，提供工具执行所需的运行时环境信息
 * @return InternalToolExecutionResult 内部工具执行结果，包含工具响应消息和直接返回标志
 * @throws IllegalStateException 当找不到指定名称的工具回调时抛出
 */
private InternalToolExecutionResult executeToolCall(Prompt prompt, AssistantMessage assistantMessage, ToolContext toolContext) {
    // 从提示词选项中获取工具回调列表
    List<ToolCallback> toolCallbacks = List.of();
    ChatOptions var6 = prompt.getOptions();
    if (var6 instanceof ToolCallingChatOptions toolCallingChatOptions) {
        toolCallbacks = toolCallingChatOptions.getToolCallbacks();
    }

    // 初始化工具响应列表和直接返回标志
    List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList();
    Boolean returnDirect = null;

    // 遍历助手消息中的所有工具调用，逐个执行
    for(AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
        logger.debug("Executing tool call: {}", toolCall.name());
        String toolName = toolCall.name();
        String toolInputArguments = toolCall.arguments();
        String finalToolInputArguments;
        
        // 处理空参数情况，使用默认空JSON对象
        if (!StringUtils.hasText(toolInputArguments)) {
            logger.warn("Tool call arguments are null or empty for tool: {}. Using empty JSON object as default.", toolName);
            finalToolInputArguments = "{}";
        } else {
            finalToolInputArguments = toolInputArguments;
        }

        // 从提示词选项或解析器中查找对应的工具回调
        ToolCallback toolCallback = (ToolCallback)toolCallbacks.stream().filter((tool) -> toolName.equals(tool.getToolDefinition().name())).findFirst().orElseGet(() -> this.toolCallbackResolver.resolve(toolName));
        if (toolCallback == null) {
            logger.warn("LLM may have adapted the tool name '{}', especially if the name was truncated due to length limits. If this is the case, you can customize the prefixing and processing logic using McpToolNamePrefixGenerator", toolName);
            throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
        }

        // 计算直接返回标志，所有工具的returnDirect都为true时才返回true
        if (returnDirect == null) {
            returnDirect = toolCallback.getToolMetadata().returnDirect();
        } else {
            returnDirect = returnDirect && toolCallback.getToolMetadata().returnDirect();
        }

        // 构建观测上下文并在观测框架下执行工具调用
        ToolCallingObservationContext observationContext = ToolCallingObservationContext.builder().toolDefinition(toolCallback.getToolDefinition()).toolMetadata(toolCallback.getToolMetadata()).toolCallArguments(finalToolInputArguments).build();
        String toolCallResult = (String)ToolCallingObservationDocumentation.TOOL_CALL.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry).observe(() -> {
            String toolResult;
            try {
                toolResult = toolCallback.call(finalToolInputArguments, toolContext);
            } catch (ToolExecutionException ex) {
                toolResult = this.toolExecutionExceptionProcessor.process(ex);
            }

            observationContext.setToolCallResult(toolResult);
            return toolResult;
        });
        
        // 收集工具响应结果
        toolResponses.add(new ToolResponseMessage.ToolResponse(toolCall.id(), toolName, toolCallResult != null ? toolCallResult : ""));
    }

    // 封装并返回工具执行结果
    return new InternalToolExecutionResult(ToolResponseMessage.builder().responses(toolResponses).build(), returnDirect);
}
