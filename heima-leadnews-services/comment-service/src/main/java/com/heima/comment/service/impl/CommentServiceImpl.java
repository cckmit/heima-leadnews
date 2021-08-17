package com.heima.comment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.heima.aliyun.GreenTextScan;
import com.heima.comment.service.CommentHotService;
import com.heima.comment.service.CommentService;
import com.heima.common.exception.CustException;
import com.heima.feigns.UserFeign;
import com.heima.model.comment.dtos.CommentDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.comment.pojos.ApComment;
import com.heima.model.comment.pojos.ApCommentLike;
import com.heima.model.comment.vo.ApCommentVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.AppThreadLocalUtils;
import com.heima.model.user.pojos.ApUser;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

    @Autowired
    UserFeign userFeign;

    @Autowired
    GreenTextScan greenTextScan;

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public ResponseResult saveComment(CommentSaveDto dto) {
        //1.检查参数
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //保存comment需要使用feign调用user微服务查出user的全部信息，拿到authorName
        ResponseResult<ApUser> userResult = userFeign.findUserById(user.getId());
        ApUser fullUser = userResult.getData();
        // 使用阿里云进行文本反垃圾检测
//        boolean isTextScan = handleTextScan(dto.getContent());
//        if (!isTextScan) {
//            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_ALLOW, "评论存在异常信息，无法发表");
//        }
        // save ApComment 设置时间、内容、评论的作者、点赞数、回复数
        ApComment apComment = new ApComment();
        apComment.setAuthorId(fullUser.getId());
        apComment.setAuthorName(fullUser.getName());
        apComment.setArticleId(dto.getArticleId());
        apComment.setType((short) 0);
        apComment.setContent(dto.getContent());
        apComment.setImage(fullUser.getImage());
        apComment.setLikes(0);
        apComment.setReply(0);
        apComment.setFlag((short) 0);
        apComment.setCreatedTime(new Date());
        apComment.setUpdatedTime(new Date());
        mongoTemplate.save(apComment);
        return ResponseResult.okResult();
    }

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    CommentHotService commentHotService;

    @Override
    public ResponseResult like(CommentLikeDto dto) {
        //检验参数
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //当点赞参数为0-点赞时
        Query query = Query.query(Criteria.where("authorId").is(user.getId()).and("commentId").is(dto.getCommentId()));
        Query commentQuery = Query.query(Criteria.where("authorId").is(user.getId()).and("id").is(dto.getCommentId()));
        ApComment apComment;
        //开启分布式锁
        RLock lock = redissonClient.getLock("likes-lock");
        lock.lock();
        try {
            apComment = mongoTemplate.findOne(commentQuery, ApComment.class);
            if (apComment == null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "未发现评论信息");
            }
            if (dto.getOperation().intValue() == 0) {
                //先查一下之前是否有点过赞，点过抛异常 通过 authorId commentId
                ApCommentLike apCommentLike;
                //            ApCommentLike apCommentLike = mongoTemplate.findOne(query, ApCommentLike.class);
                //            if (apCommentLike != null) {
                //                CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "请勿重复点赞");
                //            }
                apCommentLike = new ApCommentLike();
                apCommentLike.setAuthorId(user.getId());
                apCommentLike.setCommentId(dto.getCommentId());
                apCommentLike.setOperation((short) 0);
                mongoTemplate.save(apCommentLike);
                apComment.setLikes(apComment.getLikes() + 1);
                mongoTemplate.save(apComment);
                //热点评论判断,点赞数>10并且flag为0，进行修改
                if (apComment.getLikes().intValue() >= 10 && apComment.getFlag().intValue() == 0) {
                    commentHotService.hotCommentExecutor(apComment);
                }
                //这边的操作都需要到最后返回
            }
            //当点赞参数为1-取消点赞时
            if (dto.getOperation().intValue() == 1) {
                mongoTemplate.remove(query, ApCommentLike.class);
                apComment.setLikes(apComment.getLikes() < 1 ? 0 : apComment.getLikes() - 1);
                mongoTemplate.save(apComment);
            }
        } finally {
            lock.unlock();//释放锁
        }
        //返回点赞数量
        Map map = new HashMap<>();
        map.put("likes", apComment.getLikes());
        return ResponseResult.okResult(map);
    }

    @Override
    public ResponseResult findByArticleId(CommentDto dto) {
        //检查参数
        ApUser user = AppThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        // 查询评论列表，根据当前文章id进行检索，按照创建时间倒序，分页查询（默认10条数据）
        //判断时间和页码
        if (dto.getMinDate() == null) {
            dto.setMinDate(new Date());
        }
        if (dto.getSize() == null || dto.getSize() < 1) {
            dto.setSize(10);
        }
        //普通评论的查询条件，根据日期倒序
        //先查热点评论，然后将普通评论加在热点评论中
        Query hotCommentsQuery = Query.query(Criteria.where("articleId").is(dto.getArticleId()).and("flag").is(1)).with(Sort.by(Sort.Direction.DESC, "likes"));
        List<ApComment> apComments = mongoTemplate.find(hotCommentsQuery, ApComment.class);
        int hotCommentsSize = apComments.size();
        //计算正常评论的size
        int normalSize = dto.getSize() - hotCommentsSize;
        Query query = Query.query(Criteria.where("articleId").is(dto.getArticleId()).and("flag").is(0).and("createdTime").lt(dto.getMinDate())).with(Sort.by(Sort.Direction.DESC, "createdTime")).limit(normalSize);
        //查询正常评论集合
        List<ApComment> normalApComments = mongoTemplate.find(query, ApComment.class);
        //将正常的评论集合添加到热点集合中
        apComments.addAll(normalApComments);
        //如果没有登录，直接返回
        if (user == null || CollectionUtils.isEmpty(apComments)) {
            return ResponseResult.okResult(apComments);
        }
        //如果有登录，将评论信息集合进行遍历并转为vo对象
        ArrayList<ApCommentVo> apCommentVos = new ArrayList<>();
        //把评论中的id提出来成为一个集合
        List<String> commentIdList = apComments.stream().map(ApComment::getId).collect(Collectors.toList());
        //查询在这些评论中，我在哪些评论里点过赞
        Query userLikesQuery = Query.query(Criteria.where("commentId").in(commentIdList).and("authorId").is(user.getId()));
        List<ApCommentLike> apCommentLikes = mongoTemplate.find(userLikesQuery, ApCommentLike.class);
        //获取点赞记录中，所有评论的id
        List<String> userLikesCommentIdList = apCommentLikes.stream().map(ApCommentLike::getId).distinct().collect(Collectors.toList());
        //获取点赞记录中所有评论的id
        for (ApComment apComment : apComments) {
            /*  每次遍历给前端的VO对象都需要重新创建*/
            ApCommentVo apCommentVo = new ApCommentVo();
            BeanUtils.copyProperties(apComment, apCommentVo);
            if (userLikesCommentIdList.contains(apComment.getId())) {
                apCommentVo.setOperation((short) 0);
            }
            apCommentVos.add(apCommentVo);
        }
        return ResponseResult.okResult(apCommentVos);
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
