package com.lcl.myaiagent.controller;

import com.lcl.myaiagent.common.BaseResponse;
import com.lcl.myaiagent.common.ResultUtils;
import com.lcl.myaiagent.model.enums.KnowledgeDocumentType;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.KnowledgeDocumentVO;
import com.lcl.myaiagent.model.vo.KnowledgeSearchResultVO;
import com.lcl.myaiagent.service.KnowledgeDocumentService;
import com.lcl.myaiagent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/knowledge/documents")
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;
    private final UserService userService;

    @PostMapping
    public BaseResponse<KnowledgeDocumentVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(knowledgeDocumentService.upload(file, KnowledgeDocumentType.from(type), loginUser));
    }

    @GetMapping
    public BaseResponse<List<KnowledgeDocumentVO>> list(HttpServletRequest request) {
        return ResultUtils.success(knowledgeDocumentService.list(userService.getLoginUser(request)));
    }

    @DeleteMapping("/{documentId}")
    public BaseResponse<Boolean> delete(@PathVariable long documentId, HttpServletRequest request) {
        knowledgeDocumentService.delete(documentId, userService.getLoginUser(request));
        return ResultUtils.success(true);
    }

    @GetMapping("/search")
    public BaseResponse<List<KnowledgeSearchResultVO>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit,
            HttpServletRequest request) {
        return ResultUtils.success(knowledgeDocumentService.search(query, limit, userService.getLoginUser(request)));
    }
}
