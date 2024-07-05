package com.yupi.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;
@Data
public class TeamQuitRequest implements Serializable {
    /**
     * 队伍id
     */
    private Long teamId;


}
