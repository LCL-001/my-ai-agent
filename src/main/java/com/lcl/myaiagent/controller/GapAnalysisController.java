package com.lcl.myaiagent.controller;

import com.lcl.myaiagent.common.BaseResponse;
import com.lcl.myaiagent.common.ResultUtils;
import com.lcl.myaiagent.model.dto.GapAnalysisRequest;
import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.GapAnalysisVO;
import com.lcl.myaiagent.service.GapAnalysisService;
import com.lcl.myaiagent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/gap-analyses")
@RequiredArgsConstructor
public class GapAnalysisController {

    private final GapAnalysisService gapAnalysisService;
    private final UserService userService;

    @PostMapping
    public BaseResponse<GapAnalysisVO> analyze(@RequestBody GapAnalysisRequest analysisRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(gapAnalysisService.analyze(analysisRequest.getJobDescription(), loginUser));
    }

    @GetMapping
    public BaseResponse<List<GapAnalysisVO>> list(HttpServletRequest request) {
        return ResultUtils.success(gapAnalysisService.list(userService.getLoginUser(request)));
    }
}
