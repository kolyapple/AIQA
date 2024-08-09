package com.wxy.aiqa.common;

import com.wxy.aiqa.constant.CommonConstant;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求
 *
 * @author <a href="https://github.com/yupi">程序员鱼皮</a>
 * @from <a href="https://wxy.icu">编程导航知识星球</a>
 */
/**
 * 审核请求
 */
@Data
public class ReviewRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 状态：0-待审核, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;


    private static final long serialVersionUID = 1L;
}

