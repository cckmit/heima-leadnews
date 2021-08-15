package com.heima.model.behavior.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * APP行为实体表,一个行为实体可能是用户或者设备，或者其它
 * @TableName ap_behavior_entry
 */
@Data
@Document(value ="ap_behavior_entry")
public class ApBehaviorEntry implements Serializable {
    /**
     * 主键
     */
    private String id;

    /**
     * 实体类型
            0终端设备
            1用户
     */
    @TableField(value = "type")
    private Short type;

    /**
     * 实体ID
     */
    @TableField(value = "ref_id")
    private Integer refId;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private Date createdTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public enum  Type{
        USER((short)1),EQUIPMENT((short)0);
        @Getter
        short code;
        Type(short code){
            this.code = code;
        }
    }
    public boolean isUser(){
        if(this.getType()!=null&&this.getType()== Type.USER.getCode()){
            return true;
        }
        return false;
    }
}
