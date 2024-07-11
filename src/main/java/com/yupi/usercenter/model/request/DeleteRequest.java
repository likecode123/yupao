package com.yupi.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;
@Data
public class DeleteRequest implements Serializable
{

    private static final long serialVersionUID = -8090879395084030155L;

    /**
     * id
     */
    private Long id;
}
