package com.atguigu.gulimail.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.atguigu.gulimail.product.entity.AttrEntity;
import com.atguigu.gulimail.product.service.AttrAttrgroupRelationService;
import com.atguigu.gulimail.product.vo.AttrAndAttrGroupVo;
import com.atguigu.gulimail.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimail.product.vo.AttrRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimail.product.entity.AttrGroupEntity;
import com.atguigu.gulimail.product.service.AttrGroupService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;



/**
 * ???Է??
 *
 * @author zhangtianyu
 * @email 937027255@qq.com
 * @date 2022-07-08 19:52:38
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    /**
     * 列表
     */
    @RequestMapping("/list/{catelogId}")
    //@RequiresPermissions("product:attrgroup:list")
    public R list(@RequestParam Map<String, Object> params,@PathVariable Long catelogId){

        PageUtils page = attrGroupService.queryPageByCateId(params,catelogId);
//        PageUtils page = attrGroupService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 根据一个分组id查询出这个分组中的所有属性内容
     * @param attrgroupId
     * @return
     */
    @GetMapping("/{attrgroupId}/attr/relation")
    public R getAttrRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        List<AttrEntity> data = attrGroupService.getAttrRelation(attrgroupId);
        return R.ok().put("data",data);
    }

    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVo[] vos) {
        attrGroupService.deleteAttrGroupRelation(vos);
        return R.ok();
    }


    /**
     * 通过分组id获取到分组的数据
     * @param attrGroupId
     * @return
     */
    @RequestMapping("/info/{attrGroupId}")
    //@RequiresPermissions("product:attrgroup:info")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
        //找到了这个分组的数据
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        //获取这个分组的完整路径
        Long catelogId = attrGroup.getCatelogId();
        Long[] path = attrGroupService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(path);

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 获取属性分组里面还没有关联本分类里面的其他基本属性，方便添加新的关联
     * @return
     */
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R getNoRelationGroup(@RequestParam Map<String, Object> params,@PathVariable Long attrgroupId) {
        PageUtils page = attrGroupService.getNoRelationGroup(params,attrgroupId);
        return R.ok().put("page",page);
    }

    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrRespVo> vos) {
        attrAttrgroupRelationService.addRelation(vos);
        return R.ok();
    }

    /**
     * 根据分类获取所有的分组和分组关联的属性
     * @param catelogId
     * @return
     */
    @GetMapping("/{catelogId}/withattr")
    public R getAttrAndGroupByCatId(@PathVariable Long catelogId) {
        List<AttrAndAttrGroupVo> vos = attrGroupService.getAttrAndGroupByCatId(catelogId);
        return R.ok().put("data",vos);
    }
    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attrgroup:save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attrgroup:update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attrgroup:delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
