package com.heima.behavior.service.impl;

import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class ApBehaviorEntryServiceImpl implements ApBehaviorEntryService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public ApBehaviorEntry findByUserIdOrEquipmentId(Integer userId, Integer equipmentId) {

        //1. 判断userId是否为空  不为空 使用userId查询  如果不存在基于userId创建实体数据
        //type user 1 equip 0
        if (userId!=null) {
            ApBehaviorEntry apBehaviorEntry= mongoTemplate.findOne(Query.query(Criteria.where("refId").is(userId).and("type").is(1)), ApBehaviorEntry.class);
            if (apBehaviorEntry==null) {
                 apBehaviorEntry = new ApBehaviorEntry();
                apBehaviorEntry.setType((short)1);
                apBehaviorEntry.setRefId(userId);
                apBehaviorEntry.setCreatedTime(new Date());
                mongoTemplate.save(apBehaviorEntry);
            }
            //查出来apBehaviorEntry，返回
            log.info("正在返回用户行为数据至MongoDB→");
            return apBehaviorEntry;
        }
        //2. 判断设备id是否为空   不为空 使用设备id查询  如果不存在基于设备id创建实体数据
        if (equipmentId==null) {
            ApBehaviorEntry apBehaviorEntry = mongoTemplate.findOne(Query.query(Criteria.where("refId").is(userId).and("type").is(0)), ApBehaviorEntry.class);
            if (apBehaviorEntry==null) {
                apBehaviorEntry = new ApBehaviorEntry();
                apBehaviorEntry.setType((short)0);
                apBehaviorEntry.setRefId(equipmentId);
                apBehaviorEntry.setCreatedTime(new Date());
                mongoTemplate.save(apBehaviorEntry);
            }
            //查出来apBehaviorEntry，返回
            log.info("正在返回游客行为数据至MongoDB→");
            return apBehaviorEntry;
        }
        return null;
    }
}
