package com.heima.comment.service.impl;

import com.heima.comment.service.CommentHotService;
import com.heima.model.comment.pojos.ApComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
/**
 * 计算热点评论
 */
public class CommentHotServiceImpl implements CommentHotService {

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public void hotCommentExecutor(ApComment apComment) {
        log.info("异步计算热点评论方法 执行 start======================= {}",apComment);
        //查询热点评论列表flag=1，并按照点赞数进行降序排序
        Query query = Query.query(Criteria.where("flag").is(1).and("id").is(apComment.getId())).with(Sort.by(Sort.Direction.DESC, "likes"));
        List<ApComment> hotComments = mongoTemplate.find(query, ApComment.class);
        if (hotComments==null||hotComments.size()<5) {
            apComment.setFlag((short)1);
            mongoTemplate.save(apComment);
            return;
        }
        //跟热点评论中最后一条的赞赏进行比较，如果当前消息的赞数大于最后一条，则直接替换掉热点评论
        ApComment lastHotComment = hotComments.get(hotComments.size()-1);
        int lastHotCommentsLikes = lastHotComment.getLikes().intValue();
        if (lastHotCommentsLikes>apComment.getLikes().intValue()) {
            apComment.setFlag((short)1);
            mongoTemplate.save(apComment);
            lastHotComment.setFlag((short)0);
            mongoTemplate.save(lastHotComment);
            return;
        }
    }
}
