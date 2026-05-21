package com.eagle.example.integration.scheduler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB Starter 验证执行器。
 */
@Slf4j
@Component
public class SampleXxlJobHandler {

    @XxlJob("sampleJobHandler")
    public void sampleJob() {
        String param = XxlJobHelper.getJobParam();
        log.info("[XXL-JOB] Sample job executed, param={}", param);
        XxlJobHelper.handleSuccess("执行成功");
    }

    @XxlJob("sampleShardingJobHandler")
    public void sampleShardingJob() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("[XXL-JOB] Sharding job executed, shardIndex={}, shardTotal={}", shardIndex, shardTotal);
        XxlJobHelper.handleSuccess("分片执行成功");
    }
}
