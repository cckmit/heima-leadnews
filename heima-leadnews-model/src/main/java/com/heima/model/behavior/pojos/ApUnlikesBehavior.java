package com.heima.model.behavior.pojos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * APP不喜欢行为表
 *
 * @TableName ap_unlikes_behavior
 */
@Document("ap_unlikes_behavior")
@Data
public class ApUnlikesBehavior implements Serializable {
    /**
     *
     */
    @TableId(value = "id")
    private String id;

    /**
     * 实体ID
     */
    @TableField(value = "entry_id")
    private String entryId;

    /**
     * 文章ID
     */
    @TableField(value = "article_id")
    private Long articleId;

    /**
     * 0 不喜欢
     * 1 取消不喜欢
     */
    @TableField(value = "type")
    private Short type;

    /**
     * 登录时间
     */
    @TableField(value = "created_time")
    private Date createdTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    // 定义不喜欢操作的类型
    public enum Type {
        UNLIKE((short) 0), CANCEL((short) 1);
        short code;

        Type(short code) {
            this.code = code;
        }

        public short getCode() {
            return this.code;
        }
    }
}
