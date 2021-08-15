package com.heima.model.behavior.dtos;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReadBehaviorDto {

    @NotNull(message = "文章id不能为空")
    Long articleId;

    Integer equipmentId;

    /**
     * 阅读次数
     */
    Short count;
    /**
     * 阅读时长（S)
     */
    Integer readDuration;
    /**
     * 阅读百分比
     */
    Short percentage;
    /**
     * 加载时间
     */
    Short loadDuration;
}
