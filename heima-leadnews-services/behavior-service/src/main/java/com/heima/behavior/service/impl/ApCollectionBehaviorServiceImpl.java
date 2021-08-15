package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApCollectionBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApCollection;
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
public class ApCollectionBehaviorServiceImpl implements ApCollectionBehaviorService {

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public ResponseResult collectArticle(CollectionBehaviorDto dto) {
        //1.校验参数
        ApUser user = AppThreadLocalUtils.getUser();
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), dto.getEquipmentId());
        Query query = Query.query(Criteria.where("entryId").is(apBehaviorEntry.getId()).and("articleId").is(dto.getArticleId()));
        ApCollection apCollection = mongoTemplate.findOne(query, ApCollection.class);
        //2.业务实现
        // 3. 如果是收藏操作，查询是否已经收藏
        if (dto.getOperation().intValue() == 0) {
            if (apCollection != null) {
                CustException.cust(AppHttpCodeEnum.DATA_EXIST, "您已收藏该文章");
            }
            apCollection = new ApCollection();
            apCollection.setEntryId(apBehaviorEntry.getId());
            apCollection.setArticleId(dto.getArticleId());
            apCollection.setType((short) 0);
            apCollection.setCollectionTime(new Date());
            mongoTemplate.save(apCollection);
        } else {
            mongoTemplate.remove(query, ApCollection.class);
        }
        return ResponseResult.okResult();
        //3.返回结果

    }
}
