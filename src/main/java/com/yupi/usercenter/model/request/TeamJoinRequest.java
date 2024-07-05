package com.yupi.usercenter.model.request;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;
@Data
public class TeamJoinRequest {

    /**
     * teamId
     */
    private Long teamId;



    /**
     * 密码
     */
    private String password;



}
