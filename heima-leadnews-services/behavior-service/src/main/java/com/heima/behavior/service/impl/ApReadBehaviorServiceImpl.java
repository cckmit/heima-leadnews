package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApReadBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.ReadBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApReadBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class ApReadBehaviorServiceImpl implements ApReadBehaviorService {
    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;

    @Override
    public ResponseResult apReadBehavior(ReadBehaviorDto readBehaviorDto) {
        //1.校验参数
        ApUser user = AppThreadLocalUtils.getUser();
        if (user==null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //2.业务实现
        //查询行为实体类，没有则创建
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), readBehaviorDto.getEquipmentId());
        if (apBehaviorEntry==null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        Query query = Query.query(Criteria.where("entryId").is(apBehaviorEntry.getId()).and("articleId").is(readBehaviorDto.getArticleId()));
        ApReadBehavior apReadBehavior = mongoTemplate.findOne(query, ApReadBehavior.class);
        if (apReadBehavior==null) {
            apReadBehavior = new ApReadBehavior();
            apReadBehavior.setEntryId(apBehaviorEntry.getId());
            apReadBehavior.setArticleId(readBehaviorDto.getArticleId());
            apReadBehavior.setCount((short)1);
            apReadBehavior.setCreatedTime(new Date());
            apReadBehavior.setUpdatedTime(new Date());
            mongoTemplate.save(apReadBehavior);
        }else {
           apReadBehavior.setCount((short)(apReadBehavior.getCount()+1));
           apReadBehavior.setUpdatedTime(new Date());
           mongoTemplate.save(apReadBehavior);
        }

        //3.返回结果
        return ResponseResult.okResult();
    }
}
