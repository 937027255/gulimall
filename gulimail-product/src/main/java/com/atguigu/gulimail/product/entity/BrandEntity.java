package com.atguigu.gulimail.product.entity;

import com.atguigu.common.valid.AddGroup;
import com.atguigu.common.valid.ListValue;
import com.atguigu.common.valid.UpdateGroup;
import com.atguigu.common.valid.UpdateStatusGroup;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;

/**
 * Ʒ?
 * 
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 14:31:00
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Null(message = "添加品牌时不能指定品牌id",groups = {AddGroup.class})
    @NotNull(message = "修改时必须指定品牌id",groups = {UpdateGroup.class})
	@TableId
	private Long brandId;

    @NotBlank(message = "品牌名必须非空",groups = {AddGroup.class,UpdateGroup.class})
	private String name;

    @NotBlank(message = "新增logo不能为空值",groups = {AddGroup.class})
    @URL(message = "logo必须是一个合法的url地址",groups = {AddGroup.class,UpdateGroup.class})
	private String logo;

	private String descript;

    @TableLogic
    @ListValue(vals = {0,1}, message = "只能传入指定的数值0、1",groups = {AddGroup.class,UpdateStatusGroup.class})
    @NotNull(groups = {AddGroup.class, UpdateStatusGroup.class})
	private Integer showStatus;

    @NotEmpty(groups = {AddGroup.class})
    @Pattern(regexp = "^[a-zA-Z]$",message = "首字母必须是一个字母",groups = {AddGroup.class,UpdateGroup.class})
	private String firstLetter;

    @NotNull(groups = {AddGroup.class})
    @Min(value = 0,message = "排序必须大于等于0",groups = {AddGroup.class,UpdateGroup.class})
	private Integer sort;

}
