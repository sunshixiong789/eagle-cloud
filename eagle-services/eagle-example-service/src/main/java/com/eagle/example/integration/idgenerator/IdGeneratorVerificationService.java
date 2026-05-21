package com.eagle.example.integration.idgenerator;

import com.eagle.idgenerator.util.IdGeneratorFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ID 生成器 Starter 验证服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdGeneratorVerificationService {

    private final IdGeneratorFacade idGeneratorFacade;

    public Map<String, String> generateAllTypes() {
        return Map.of(
                "snowflake", String.valueOf(idGeneratorFacade.snowflakeId()),
                "uuid", idGeneratorFacade.uuid(),
                "tsid", idGeneratorFacade.tsidStr(),
                "nanoId", idGeneratorFacade.nanoId(8),
                "orderNo", idGeneratorFacade.orderNo("EX"),
                "payNo", idGeneratorFacade.payNo()
        );
    }
}
