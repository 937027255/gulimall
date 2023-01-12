package com.atguigu.gulimail.product.service.impl;

import com.atguigu.common.consts.ProductConst;
import com.atguigu.gulimail.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimail.product.dao.AttrDao;
import com.atguigu.gulimail.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimail.product.entity.AttrEntity;
import com.atguigu.gulimail.product.entity.CategoryEntity;
import com.atguigu.gulimail.product.service.AttrService;
import com.atguigu.gulimail.product.service.CategoryService;
import com.atguigu.gulimail.product.vo.AttrAndAttrGroupVo;
import com.atguigu.gulimail.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimail.product.vo.AttrRespVo;
import com.atguigu.gulimail.product.vo.front.SkuItemVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.product.dao.AttrGroupDao;
import com.atguigu.gulimail.product.entity.AttrGroupEntity;
import com.atguigu.gulimail.product.service.AttrGroupService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    private AttrDao attrDao;

    @Autowired
    private AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                //这里就是获取了一个page对象，经过了一下封装
                new Query<AttrGroupEntity>().getPage(params),
                //传入一个查询对象
                new QueryWrapper<AttrGroupEntity>()
        );
        //最后把信息都封装在了这个PageUtils的工具类里面
        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCateId(Map<String, Object> params, Long catelogId) {
        //这里的key字段就有东西了
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(obj -> {
                obj.eq("attr_group_id",key).or().like("attr_group_name",key);
            });
        }

        //首先判断catelogId是否存在
        if (catelogId == 0) {
            //直接按照分页查询查找所有的数据
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);
        } else {
            wrapper.eq("catelog_id",catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);
        }
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {

        List<Long> path = new ArrayList<>();
        searchPath(catelogId,path);
        Collections.reverse(path);
        return path.toArray(new Long[path.size()]);
    }

    @Override
    public List<AttrEntity> getAttrRelation(Long attrgroupId) {
        //根据group_id获取所有的attr信息并返回
        List<AttrAttrgroupRelationEntity> attrList = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .eq("attr_group_id", attrgroupId));
        //收集所有的attr_id
        List<Long> collect = attrList.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());
        //查找出所有的attr进行返回
//        List<AttrEntity> attrEntities = attrDao.selectBatchIds(collect);
        if (collect.size() != 0){
            Collection<AttrEntity> attrEntities = attrService.listByIds(collect);
            return (List<AttrEntity>) attrEntities;
        }
        return null;

    }

    @Override
    public void deleteAttrGroupRelation(AttrGroupRelationVo[] vos) {
        //将vo中的东西放入到实体类中
        List<AttrAttrgroupRelationEntity> relationCollects = Arrays.asList(vos).stream().map(item -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(item.getAttrGroupId());
            relationEntity.setAttrId(item.getAttrId());
            return relationEntity;
        }).collect(Collectors.toList());
        //调用baseMapper中自定义方法
        attrAttrgroupRelationDao.deleteBatchRelations(relationCollects);
    }

    @Override
    public PageUtils getNoRelationGroup(Map<String, Object> params, Long attrgroupId) {
        //获取属性分组里面还没有关联的本分类里面的其他基本属性，方便添加新的关联
        //通过groupid找到此类商品对应的id
        AttrGroupEntity attrGroupEntity = baseMapper.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        //在同一个商品分类中查找所有组，然后去关系表中找到这些组用过的属性
        List<AttrGroupEntity> groupIds = baseMapper.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<Long> collectGroupIds = groupIds.stream().map(g -> {
            return g.getAttrGroupId();
        }).collect(Collectors.toList());
        List<AttrAttrgroupRelationEntity> relations = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", collectGroupIds));
        //获得已经使用的属性id
        List<Long> usedAttrIds = relations.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());
        //通过catelogId和usedAttrIds找到还能使用的属性
        //添加分页查询条件
        //1、判断模糊查询
        String key = (String) params.get("key");
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(w->{
                w.eq("attr_id",key).or().like("attr_name",key);
            });
        }
        wrapper.eq("catelog_id", catelogId).eq("attr_type", ProductConst.ATTR_TYPE_BASE.getCode());
        if (usedAttrIds != null && usedAttrIds.size() > 0) {
            wrapper.notIn("attr_id", usedAttrIds);
        }
        IPage<AttrEntity> iPage = attrDao.selectPage(new Query<AttrEntity>().getPage(params), wrapper);
        PageUtils page = new PageUtils(iPage);
        return page;
    }

    /**
     * 根据分类id获取所有分组和分组相关联的所有属性
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrAndAttrGroupVo> getAttrAndGroupByCatId(Long catelogId) {
        //根据分类id获取到所有的分组
        List<AttrGroupEntity> groups = baseMapper.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<AttrAndAttrGroupVo> collect = groups.stream().map(group -> {
            AttrAndAttrGroupVo attrAndAttrGroupVo = new AttrAndAttrGroupVo();
            BeanUtils.copyProperties(group, attrAndAttrGroupVo);
            //根据关联表查询分组关联的属性
            List<AttrEntity> attrs = this.getAttrRelation(group.getAttrGroupId());
            if (!CollectionUtils.isEmpty(attrs))
                attrAndAttrGroupVo.setAttrs(attrs);
            else
                attrAndAttrGroupVo.setAttrs(new ArrayList<AttrEntity>());
            return attrAndAttrGroupVo;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 通过spuId定位其所属的组和组的的所有规格属性信息
     * @param spuId
     * @param catalogId
     * @return
     */
    @Override
    public List<SkuItemVo.SpuItemAttrGroup> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        List<SkuItemVo.SpuItemAttrGroup> attrGroupVos = baseMapper.getAttrGroupWithAttrsBySpuId(spuId,catalogId);
        return attrGroupVos;
    }


    private void searchPath(Long catelogId, List<Long> path) {
        CategoryEntity category = categoryService.getById(catelogId);
        path.add(catelogId);
        if(category.getParentCid() != 0) {
            searchPath(category.getParentCid(),path);
        }
        return;
    }

}