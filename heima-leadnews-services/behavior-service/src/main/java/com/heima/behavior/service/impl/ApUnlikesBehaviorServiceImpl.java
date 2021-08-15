package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApUnlikesBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApUnlikesBehavior;
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
public class ApUnlikesBehaviorServiceImpl implements ApUnlikesBehaviorService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;


    @Override
    public ResponseResult apUnlikesBehavior(UnLikesBehaviorDto unLikesBehaviorDto) {
        //1.校验参数
        ApUser user = AppThreadLocalUtils.getUser();
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(),unLikesBehaviorDto.getEquipmentId());
        if (apBehaviorEntry==null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //2.业务实现
        Query query = Query.query(Criteria.where("entryId").is(apBehaviorEntry.getId()).and("articleId").is(unLikesBehaviorDto.getArticleId()));
        //查出不喜欢行为表
        ApUnlikesBehavior apUnlikesBehavior = mongoTemplate.findOne(query, ApUnlikesBehavior.class);
        //3.返回结果
        if (unLikesBehaviorDto.getType().intValue() == 0) {
            if (apUnlikesBehavior != null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "已经点过赞咯~");
            } else {
                apUnlikesBehavior = new ApUnlikesBehavior();
                apUnlikesBehavior.setEntryId(apBehaviorEntry.getId());
                apUnlikesBehavior.setArticleId(unLikesBehaviorDto.getArticleId());
                apUnlikesBehavior.setType((short) 0);
                apUnlikesBehavior.setCreatedTime(new Date());
                //保存apLikesBehavior到mongoDB
                mongoTemplate.save(apUnlikesBehavior);
            }

        } else {
            mongoTemplate.remove(query, ApUnlikesBehavior.class);
        }
        //3.返回结果
        return ResponseResult.okResult();
    }
}
