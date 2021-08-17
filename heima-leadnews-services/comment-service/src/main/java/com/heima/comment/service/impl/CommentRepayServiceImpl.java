package com.heima.comment.service.impl;

import com.heima.aliyun.GreenTextScan;
import com.heima.comment.service.CommentRepayService;
import com.heima.common.exception.CustException;
import com.heima.feigns.UserFeign;
import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.comment.pojos.ApComment;
import com.heima.model.comment.pojos.ApCommentRepay;
import com.heima.model.comment.pojos.ApCommentRepayLike;
import com.heima.model.comment.vo.ApCommentRepayVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class CommentRepayServiceImpl implements CommentRepayService {

    @Autowired
    UserFeign userFeign;

    @Autowired
    GreenTextScan greenTextScan;

    @Autowired
    MongoTemplate mongoTemplate;
    /**
     * 保存评论回复信息
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveCommentRepay(CommentRepaySaveDto dto) {
        //检查参数
        ApUser user = AppThreadLocalUtils.getUser();
        if (user==null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //保存评论回复信息 需要 authorId authorName commentId content
        ResponseResult<ApUser> userResult = userFeign.findUserById(user.getId());
        ApUser fullUser = userResult.getData();
        // 使用阿里云进行文本反垃圾检测
//        boolean isTextScan = handleTextScan(dto.getContent());
//        if (!isTextScan) {
//            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW, "评论存在异常信息，无法发表");
//        }
        ApCommentRepay apCommentRepay = new ApCommentRepay();
        apCommentRepay.setAuthorId(fullUser.getId());
        apCommentRepay.setAuthorName(fullUser.getName());
        apCommentRepay.setCommentId(dto.getCommentId());
        apCommentRepay.setContent(dto.getContent());
        apCommentRepay.setLikes(0);
        apCommentRepay.setCreatedTime(new Date());
        apCommentRepay.setUpdatedTime(new Date());
        mongoTemplate.save(apCommentRepay);
        return ResponseResult.okResult();
    }

    /**
     * 保存评论回复点赞信息
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveCommentRepayLike(CommentRepayLikeDto dto) {
        //检查参数
        ApUser user = AppThreadLocalUtils.getUser();
        if (user==null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        if (dto.getOperation()==null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"点赞参数有误");
        }
        //当operation为0点赞时
        Query query = Query.query(Criteria.where("authorId").is(user.getId()).and("commentRepayId").is(dto.getCommentRepayId()));
        Query commentRepayQuery = Query.query(Criteria.where("id").is(dto.getCommentRepayId()));
        ApCommentRepay apCommentRepay = mongoTemplate.findOne(commentRepayQuery, ApCommentRepay.class);
        if (dto.getOperation().intValue()==0) {
            //去mongoDb里查看看之前有没有点过赞 authorId commentRepayId
            ApCommentRepayLike apCommentRepayLike = mongoTemplate.findOne(query, ApCommentRepayLike.class);
            //如果有，抛异常
            if (apCommentRepayLike!=null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"请勿重复点赞~");
            }
            apCommentRepayLike=new ApCommentRepayLike();
            apCommentRepayLike.setAuthorId(user.getId());
            apCommentRepayLike.setCommentRepayId(dto.getCommentRepayId());
            apCommentRepayLike.setOperation((short)0);
            mongoTemplate.save(apCommentRepayLike);
            apCommentRepay.setLikes(apCommentRepay.getLikes()+1);
            mongoTemplate.save(apCommentRepay);
        }
        //当operation为1取消点赞时
        if (dto.getOperation().intValue()==1) {
            mongoTemplate.remove(query,ApCommentRepayLike.class);
            apCommentRepay.setLikes(apCommentRepay.getLikes()<1?0:apCommentRepay.getLikes()-1);
            mongoTemplate.save(apCommentRepay);
        }
        Map map=new HashMap();
        map.put("likes",apCommentRepay.getLikes());
        return ResponseResult.okResult(map);
    }

    /**
     * 加载评论回复信息
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadCommentRepay(CommentRepayDto dto) {
        //校验参数
        ApUser user = AppThreadLocalUtils.getUser();
        if (user==null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        if (dto.getMinDate()==null) {
            dto.setMinDate(new Date());
        }
        if (dto.getSize()==null) {
            dto.setSize(10);
        }
        Query query = Query.query(Criteria.where("commentId").is(dto.getCommentId()).and("createdTime").lt(dto.getMinDate())).with(Sort.by(Sort.Direction.DESC,"createdTime")).limit(dto.getSize());
        List<ApCommentRepay> apCommentRepays = mongoTemplate.find(query, ApCommentRepay.class);
        if (apCommentRepays==null) {
            return ResponseResult.okResult(apCommentRepays);
        }
        ArrayList<ApCommentRepayVo> apCommentRepayVos = new ArrayList<>();
        for (ApCommentRepay apCommentRepay : apCommentRepays) {
            ApCommentRepayVo apCommentRepayVo = new ApCommentRepayVo();
            BeanUtils.copyProperties(apCommentRepay,apCommentRepayVo);
            apCommentRepayVos.add(apCommentRepayVo);
        }
        return ResponseResult.okResult(apCommentRepayVos);
    }

    /**
     * 文本反垃圾
     *
     * @param content
     * @return
     */
    private boolean handleTextScan(String content) {
        boolean flag = true;
        try {
            Map map = greenTextScan.greenTextScan(content);
            String suggestion = (String) map.get("suggestion");
            switch (suggestion) {
                case "block":
                    log.info("评论包含禁止信息，审核失败");
                    flag = false;
                    break;
                case "review":
                    log.info("评论疑似包含禁止信息，待人工审核");
                    flag = false;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("阿里云文本审核出现异常，原因：{}", e.getMessage());
            log.info("AI审核调用失败，转为人工审核");
            flag = false;
        }
        return flag;
    }
}
