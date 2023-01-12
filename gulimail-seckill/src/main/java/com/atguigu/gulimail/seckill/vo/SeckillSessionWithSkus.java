package com.atguigu.gulimail.seckill.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SeckillSessionWithSkus {
    private Long id;

    private String name;

    private Date startTime;


    private Date endTime;

    private Integer status;

    private Date createTime;

    @TableField(exist = false)
    List<SeckillSkuRelationVo> relationSkus;
}
