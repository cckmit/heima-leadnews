package com.heima.model.behavior.pojos;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * APP点赞行为表
 * @TableName ap_likes_behavior
 */
@Data
@Document("ap_likes_behavior")
public class ApLikesBehavior implements Serializable {
    /**
     *
     */
    @TableId(value = "id")
    private String id;

    /**
     * 行为对象behaviorEntryId
     */
    @TableField(value = "entry_id")
    private String entryId;

    /**
     * 文章ID
     */
    @TableField(value = "article_id")
    private Long articleId;

    /**
     * 点赞内容类型
            0文章
            1动态
     */
    @TableField(value = "type")
    private Short type;

    /**
     * 0 点赞
            1 取消点赞
     */
    @TableField(value = "operation")
    private Short operation;

    /**
     * 登录时间
     */
    @TableField(value = "created_time")
    private Date createdTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
