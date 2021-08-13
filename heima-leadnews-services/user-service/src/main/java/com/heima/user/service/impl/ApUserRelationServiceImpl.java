package com.heima.user.service.impl;

import com.heima.common.constants.user.UserRelationConstants;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.UserRelationDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.service.ApUserRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApUserRelationServiceImpl implements ApUserRelationService {

    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public ResponseResult follow(UserRelationDto dto) {
        //检验参数  authorApUserId  必须登录  operation 0 1
        if (dto.getAuthorApUserId() == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        Short operation = dto.getOperation();
        if (operation == null || (operation != 0 && operation != 1)) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "关注类型错误");
        }
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        Integer loginId = user.getId();
        Integer followId = dto.getAuthorApUserId();
        //判断自己不可以关注自己
        if (loginId.equals(followId)) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "自己不能关注自己哦！");
        }
        //检验之前有没有关注过
        //参数1 key 参数2 要查询集合元素
        Double score = redisTemplate.opsForZSet()
                .score(UserRelationConstants.FOLLOW_LIST + loginId, String.valueOf(followId));
        if (operation.intValue() == 0 && score != null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "您已关注此作者，请勿重复关注");
        }
        try {
            redisTemplate.multi();
            if (operation.intValue() == 0) {
                //没有关注过  zadd follow：我的id 作者id
                //参数1：key 参数2：集合元素  参数3：score
                redisTemplate.opsForZSet().add(UserRelationConstants.FOLLOW_LIST + loginId, String.valueOf(followId), System.currentTimeMillis());
                redisTemplate.opsForZSet().add(UserRelationConstants.FANS_LIST + followId, String.valueOf(loginId), System.currentTimeMillis());
            } else {
                redisTemplate.opsForZSet().remove(UserRelationConstants.FOLLOW_LIST + loginId, String.valueOf(followId));
                redisTemplate.opsForZSet().remove(UserRelationConstants.FANS_LIST + followId, String.valueOf(loginId));
            }
            redisTemplate.exec();
            return ResponseResult.okResult();
        } catch (Exception e) {
            e.printStackTrace();
            redisTemplate.discard();
            throw e;
        }
    }
}
