package com.lcl.myaiagent.service;

import com.lcl.myaiagent.model.po.User;
import com.lcl.myaiagent.model.vo.GapAnalysisVO;

import java.util.List;

public interface GapAnalysisService {

    GapAnalysisVO analyze(String jobDescription, User loginUser);

    List<GapAnalysisVO> list(User loginUser);
}
