package com.coalblend.service.intelligent;

import com.coalblend.dto.BlendGenerateDTO;
import com.coalblend.service.intelligent.model.BlendGenerationRuntimeConfig;

public interface BlendGenerationConfigResolver {

    BlendGenerationRuntimeConfig resolve(BlendGenerateDTO dto);
}
