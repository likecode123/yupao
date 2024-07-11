package com.yupi.usercenter.model.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.yupi.usercenter.config.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

//生成equals和hashcode方法
@EqualsAndHashCode
@Data
public class TeamQuery extends PageRequest {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * id列表
     */
    private List<Long> idList;
    /**
     * 搜索文本
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;


    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;


}
