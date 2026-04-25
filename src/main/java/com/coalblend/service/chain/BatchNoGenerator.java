package com.coalblend.service.chain;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BatchNoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AtomicInteger seq = new AtomicInteger(0);

    public String mineSourceCode() {
        return next("MS");
    }

    public String rawBatchNo() {
        return next("R");
    }

    public String washBatchNo() {
        return next("W");
    }

    public String productBatchNo(String productType) {
        if ("mixed_product".equalsIgnoreCase(productType)) {
            return next("FP");
        }
        return next("CP");
    }

    public String shipmentNo() {
        return next("SH");
    }

    public String reportNo() {
        return next("QR");
    }

    private String next(String prefix) {
        int n = seq.updateAndGet(v -> v >= 999 ? 1 : v + 1);
        return prefix + LocalDateTime.now().format(FMT) + String.format("%03d", n);
    }
}
