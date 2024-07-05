package com.yupi.usercenter.config.common;

import lombok.Data;

import java.io.Serializable;
@Data
public class PageRequest implements Serializable{

    private static final long serialVersionUID = -4543923249131299977L;
    /**
     * 页面大小
     */
    protected int pageSize;
    /**
     继续编写接口
     然后使用knife4j测试接口，没问题
     3.细化接口
     这边我们会运用到队伍的状态,即公开，私有等，所以我们提前写一个队伍状态枚举类
     * 当前是第几页
     */
    protected int pageNum;

}
