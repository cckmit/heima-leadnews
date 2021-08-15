package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApLikeBehaviorService;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApLikesBehavior;
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
public class ApLikeBehaviorServiceImpl implements ApLikeBehaviorService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;

    @Override
    public ResponseResult likeOrUnlike(LikesBehaviorDto likesBehaviorDto) {

        //1.校验参数 articleId equipmentId operation(0点赞/1取消点赞) type(0文章/2，点赞的数据类型，文章还是其他的)
        //首先校验当前用户有没有登录
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //2.业务实现
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), likesBehaviorDto.getEquipmentId());
        //通过userId及articleId查询apLikeBehavior看是否存在，存在抛异常
        Query query = Query.query(Criteria.where("entryId").is(apBehaviorEntry.getId()).and("articleId").is(likesBehaviorDto.getArticleId()));
        if (likesBehaviorDto.getOperation().intValue() == 0) {
            ApLikesBehavior apLikesBehavior = mongoTemplate.findOne(query, ApLikesBehavior.class);
            if (apLikesBehavior != null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "已经点过赞咯~");
            } else {
                apLikesBehavior = new ApLikesBehavior();
                apLikesBehavior.setEntryId(apBehaviorEntry.getId());
                apLikesBehavior.setArticleId(likesBehaviorDto.getArticleId());
                apLikesBehavior.setType((short) 0);
                apLikesBehavior.setOperation((short) 0);
                apLikesBehavior.setCreatedTime(new Date());
                //保存apLikesBehavior到mongoDB
                mongoTemplate.save(apLikesBehavior);
            }

        } else {
            mongoTemplate.remove(query, ApLikesBehavior.class);
        }
        //3.返回结果
        return ResponseResult.okResult();
    }
}
