package com.heima.model.behavior.pojos;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * APP阅读行为表
 * @TableName ap_read_behavior
 */
@Document(value ="ap_read_behavior")
@Data
public class ApReadBehavior implements Serializable {
    /**
     *
     */
    @TableId(value = "id")
    private String id;

    /**
     * 用户ID
     */
    @TableField(value = "entry_id")
    private String entryId;

    /**
     * 文章ID
     */
    @TableField(value = "article_id")
    private Long articleId;

    /**
     *
     */
    @TableField(value = "count")
    private Short count;

    /**
     * 阅读时间单位秒
     */
    @TableField(value = "read_duration")
    private Integer readDuration;

    /**
     * 阅读百分比
     */
    @TableField(value = "percentage")
    private Short percentage;

    /**
     * 文章加载时间
     */
    @TableField(value = "load_duration")
    private Short loadDuration;

    /**
     * 登录时间
     */
    @TableField(value = "created_time")
    private Date createdTime;

    /**
     *
     */
    @TableField(value = "updated_time")
    private Date updatedTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
