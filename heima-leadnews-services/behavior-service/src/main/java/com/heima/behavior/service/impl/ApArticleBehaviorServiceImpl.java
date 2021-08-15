package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApArticleBehaviorService;
import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.common.constants.user.UserRelationConstants;
import com.heima.common.exception.CustException;
import com.heima.model.behavior.dtos.ArticleBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApCollection;
import com.heima.model.behavior.pojos.ApLikesBehavior;
import com.heima.model.behavior.pojos.ApUnlikesBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @作者 itcast
 * @创建日期 2021/8/15 14:31
 **/
@Service
public class ApArticleBehaviorServiceImpl implements ApArticleBehaviorService {

    @Autowired
    ApBehaviorEntryService apBehaviorEntryService;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    RedisTemplate<String,String> redisTemplate;

    @Override
    public ResponseResult loadArticleBehavior(ArticleBehaviorDto dto) {
        // 1. 检查参数  (articleId  authorApUserId)
        // 2. 定义返回值   4个boolean变量
        boolean islikes=false,isunlikes=false,iscollection=false,isfollow=false;
        // 3. 判断当前用户是否登录  ，如果未登录  直接返回结果
        // 4. 如果登录  查询对应行为数据
        ApUser user = AppThreadLocalUtils.getUser();
        if (user!=null) {
            // 4.1  查询登录用户对应的行为实体数据
            ApBehaviorEntry behaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(user.getId(), dto.getEquipmentId());
            if (behaviorEntry==null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"未查询到行为实体信息");
            }
            // 4.2  查询点赞行为
            Query query = Query.query(Criteria.where("entryId").is(behaviorEntry.getId()).and("articleId").is(dto.getArticleId()));
            ApLikesBehavior likesBehavior = mongoTemplate.findOne(query, ApLikesBehavior.class);
            if(likesBehavior!=null){
                islikes=true;
            }
            // 4.3  查询不喜欢行为
            ApUnlikesBehavior unlikesBehavior = mongoTemplate.findOne(query, ApUnlikesBehavior.class);
            if(unlikesBehavior!=null){
                isunlikes=true;
            }
            // 4.4  查询收藏行为
            ApCollection collectionBehavior = mongoTemplate.findOne(query, ApCollection.class);
            if(collectionBehavior!=null){
                iscollection=true;
            }
            // 4.5  查询关注行为     作者: authorApUserId   登录:user.getId
            Double score = redisTemplate.opsForZSet().score(UserRelationConstants.FOLLOW_LIST + user.getId(), String.valueOf(dto.getAuthorApUserId()));
            if(score!=null){
                isfollow=true;
            }
        }
        // 5. 封装map结果数据返回
        Map map = new HashMap<>();
        map.put("islike",islikes);
        map.put("isfollow",isfollow);
        map.put("isunlike",isunlikes);
        map.put("iscollection",iscollection);
        return ResponseResult.okResult(map);
    }
}
