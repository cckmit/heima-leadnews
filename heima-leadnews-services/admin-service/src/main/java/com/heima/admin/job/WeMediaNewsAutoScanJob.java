package com.heima.admin.job;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.heima.admin.service.WemediaNewsAutoScanService;
import com.heima.common.exception.CustException;
import com.heima.feigns.WemediaFeign;
import com.heima.model.common.dtos.ResponseResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
/**
 * 自动审核任务类
 */
public class WeMediaNewsAutoScanJob {

    @Autowired
    WemediaFeign wemediaFeign;

    @Autowired
    WemediaNewsAutoScanService wemediaNewsAutoScanService;

    @XxlJob("weMediaNewsAutoScanJob")
    public ReturnT<String> autoScanJob(String param){
        log.info("xxljob：文章定时审核任务正在执行→");
        //先远程调用feign查出需要审核的文章id集合
        ResponseResult<List<Integer>> release = wemediaFeign.findRelease();
        if (release.getCode().intValue()!=0) {
            log.error("xxljob：文章定时审核任务执行失败→ {}",release.getErrorMessage());
            return ReturnT.FAIL;
        }
        List<Integer> idList = release.getData();
        if (idList!=null&& CollectionUtils.isNotEmpty(idList)) {
            for (Integer id : idList) {
                //遍历集合调用feign进行自动审核
                wemediaNewsAutoScanService.autoScanByMediaNewsId(id);
            }
        }
        log.info("xxljob：文章定时审核任务执行结束√");
        return ReturnT.SUCCESS;
    }
}
