package com.atguigu.gulimail.product.service.impl;

import com.atguigu.common.consts.ProductConst;
import com.atguigu.gulimail.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimail.product.dao.AttrGroupDao;
import com.atguigu.gulimail.product.dao.CategoryDao;
import com.atguigu.gulimail.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimail.product.entity.AttrGroupEntity;
import com.atguigu.gulimail.product.entity.CategoryEntity;
import com.atguigu.gulimail.product.service.AttrGroupService;
import com.atguigu.gulimail.product.vo.AttrRespVo;
import com.atguigu.gulimail.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.product.dao.AttrDao;
import com.atguigu.gulimail.product.entity.AttrEntity;
import com.atguigu.gulimail.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    AttrGroupService attrGroupService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        //保存到attr表中
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr,attrEntity);
        baseMapper.insert(attrEntity);
        //新增数据到关联关系表中
        if (attr.getAttrType() == ProductConst.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());

            attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
        }


    }

    @Override
    public PageUtils getBaseList(Map<String, Object> params, Long catelogId, String type) {
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("attr_type","base".equalsIgnoreCase(type)
                ? ProductConst.ATTR_TYPE_BASE.getCode() : ProductConst.ATTR_TYPE_SALE.getCode());
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(obj -> {
                obj.eq("attr_id",key).or().like("attr_name",key);
            });
        }
        if (catelogId != 0) {
            wrapper.eq("catelog_id",catelogId);
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> collect = records.stream().map(attrEntity -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            //根据cateid查对应的分类名称
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            attrRespVo.setCatelogName(categoryEntity.getName());
            //根据attr_id查询group_id
            if ("base".equalsIgnoreCase(type)) {
                AttrAttrgroupRelationEntity attr = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (attr != null && attr.getAttrGroupId() != null)
                {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attr.getAttrGroupId());
                    if (attrGroupEntity != null)
                        attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }

            return attrRespVo;
        }).collect(Collectors.toList());
        pageUtils.setList(collect);
        return pageUtils;


    }

    @Override
    public AttrRespVo getAttrDetailById(Long attrId) {
        //先获取属性的基本信息
        AttrEntity attrEntity = baseMapper.selectById(attrId);
        AttrRespVo attrRespVo = new AttrRespVo();
        BeanUtils.copyProperties(attrEntity,attrRespVo);
        if (attrEntity.getAttrType() == ProductConst.ATTR_TYPE_BASE.getCode()) {
            //获取groupId,去关联关系表查找
            AttrAttrgroupRelationEntity attr = attrAttrgroupRelationDao
                    .selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            attrRespVo.setAttrGroupId(attr.getAttrGroupId());
        }

        //获取分类的完整路径
        Long[] catelogPath = attrGroupService.findCatelogPath(attrEntity.getCatelogId());
        attrRespVo.setCatelogPath(catelogPath);

        return attrRespVo;
    }
    @Transactional
    @Override
    public void updateAttrDetail(AttrVo attrVo) {
        //先修改attr中的数据
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attrVo,attrEntity);
        baseMapper.updateById(attrEntity);
        if (attrVo.getAttrType() == ProductConst.ATTR_TYPE_BASE.getCode()) {
            //修改关联关系表中的内容
            UpdateWrapper<AttrAttrgroupRelationEntity> wrapper = new UpdateWrapper<>();
            wrapper.eq("attr_id",attrVo.getAttrId());
            AttrAttrgroupRelationEntity attrRelation = new AttrAttrgroupRelationEntity();
            attrRelation.setAttrId(attrVo.getAttrId());
            attrRelation.setAttrGroupId(attrVo.getAttrGroupId());
            attrAttrgroupRelationDao.update(attrRelation,wrapper);

            Integer count = attrAttrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrVo.getAttrId()));
            if (count > 0) {
                //说明是修改操作
                attrAttrgroupRelationDao.update(attrRelation,wrapper);
            } else {
                //说明是新增操作
                attrAttrgroupRelationDao.insert(attrRelation);
            }
        }

    }

    @Override
    public List<AttrEntity> getAttrsByIdsAndSearchCondition(List<Long> attrIds) {
        List<AttrEntity> list = this.list(new QueryWrapper<AttrEntity>().eq("search_type", 1).in("attr_id", attrIds));
        return list;
    }

}