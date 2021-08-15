package com.heima.model.behavior.dtos;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;


/**pojo类首先导入lombok*/
@Data
/**
 * @author limingfei
 */
public class LikesBehaviorDto {
    @NotNull(message = "文章id不能为空")
    Long articleId;

    Integer equipmentId;

    /**Integer operation;字段是选择性字段，选择short来优化表*/
    @Range(min = 0,max =1 ,message ="点赞参数错误" )
    Short operation;

    @Range(min = 0,max =2 ,message ="点赞参数错误" )
    Short type;
}
