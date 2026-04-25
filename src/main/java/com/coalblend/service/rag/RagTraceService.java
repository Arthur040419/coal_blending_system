package com.coalblend.service.rag;

import com.coalblend.vo.AiExplainResultVO;
import com.coalblend.vo.rag.RagRetrieveResultVO;

public interface RagTraceService {

    void saveBlendGenerateTrace(Long planId, RagRetrieveResultVO retrieveResult, AiExplainResultVO explainResult);
}
