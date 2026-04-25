package com.coalblend.service.rag;

import com.coalblend.entity.Orders;
import com.coalblend.vo.rag.RagRetrieveResultVO;

public interface RagRetrieveService {

    RagRetrieveResultVO retrieveByOrder(Orders order, int topK);

    String buildKnowledgeText(RagRetrieveResultVO result);
}
