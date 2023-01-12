package com.atguigu.gulimail.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimail.product.service.CategoryBrandRelationService;
import com.atguigu.gulimail.product.vo.front.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimail.product.dao.CategoryDao;
import com.atguigu.gulimail.product.entity.CategoryEntity;
import com.atguigu.gulimail.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 将种类表中的数据查询出来并且按照树形结构进行显示
     * @return
     */
    @Override
    public List<CategoryEntity> showCategoryWithTree() {
        //首先将所有的商品都查询出来
        List<CategoryEntity> all = baseMapper.selectList(null);
        //将所有的结果封装成树形结构
        List<CategoryEntity> treeList = all.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map((menu) -> {
                    menu.setChildren(setChildrenMenu(menu, all));
                    return menu;
                }).sorted((m1, m2) -> (m1.getSort() == null ? 0 : m1.getSort()) - (m2.getSort() == null ? 0 : m2.getSort())).collect(Collectors.toList());
        return treeList;
    }

    /**
     * 根据ids删除数据库表中的数据
     * @param catIds
     */
    @Override
    public void removeMenusByIds(List<Long> catIds) {
        //TODO 删除含有引用的数据的时候不能够直接删除
        baseMapper.deleteBatchIds(catIds);
    }

    /**
     * @CacheEvict:更新之后使缓存失效，进行删除
     * @Caching:使用Caching在更新种类之后删除一级分类和所有分类信息的缓存，两个@CacheEvict进行组合操作
     * @param category
     */

    @Caching(evict = {
            @CacheEvict(value = "category",key = "'getLevelOneCatagories'"),
            @CacheEvict(value = "category",key = "'getCatalogJson'")
    })
    //删除某个分区下的所有数据
//    @CacheEvict(value = "category",allEntries = true)
    @Transactional
    @Override
    public void updateDetail(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());

    }

    /**
     * 获取一级种类的所有分类信息
     * @return
     */
    //1、每一个需要缓存的数据我们都指定要放到哪个名字的分区
    //2、代表当前方法的结果需要缓存，如果缓存中有，方法不调用，如果缓存中没有会调用方法，最后将方法的结果放入到缓存中
    /**
     * 3、默认行为：1)如果缓存中有，方法不调用
     *            2)redis-key默认自动生成，缓存的名字::SimpleKey[](自主生成的key值)
     *            3)缓存的value值，默认使用jdk序列化机制，将序列化之后的数据存到redis中
     *            4)ttl默认-1，永远不过期
     *    自定义：1)指定生成的的缓存使用的key  key属性的指定，接收一个SpEl表达式   详细参见spel的详细语法
     *           2)指定缓存的数据的存活时间，配置文件中修改ttl     spring.cache.redis.time-to-live=3600000
     *           3)将数据保存为json格式
     */

    @Cacheable(value = {"category"},key = "#root.methodName")
    @Override
    public List<CategoryEntity> getLevelOneCatagories() {
        System.out.println("...getLevelOneCatagories...");
        List<CategoryEntity> categoryEntities = this.list(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0L));
        return categoryEntities;
    }

    /**
     * 使用spring-cache来操作redis
     * @return
     */
    @Cacheable(value = "category",key = "#root.method.name")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        System.out.println("查询了数据库......");
        //优化：先将所有的数据查询出来，然后再进行一级、二级分类的查询及组装-->只查询一次数据库
        List<CategoryEntity> allCategories = baseMapper.selectList(null);

        //1、查出所有的一级分类
        List<CategoryEntity> levelOneCatagories = getCategoriesByParentCid(allCategories,0L);
        //根据所有的一级分类找到对应的二级分类进行封装
        Map<String, List<Catelog2Vo>> collectLevel1AsMap = levelOneCatagories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //根据一级分类查询二级分类列表
            List<CategoryEntity> levelTwoCatagories = getCategoriesByParentCid(allCategories,v.getCatId());
            List<Catelog2Vo> collectLevel2 = null;
            if (levelTwoCatagories != null) {
                collectLevel2 = levelTwoCatagories.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //查询对应的三级id
                    List<CategoryEntity> levelThreeCatagories = getCategoriesByParentCid(allCategories,l2.getCatId());
                    List<Catelog2Vo.Catelog3Vo> collectLevel3 = null;
                    if (levelThreeCatagories != null) {
                        collectLevel3 = levelThreeCatagories.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                    }
                    catelog2Vo.setCatalog3List(collectLevel3);
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return collectLevel2;
        }));
        return collectLevel1AsMap;
    }

    /**
     * 加入缓存功能获取json数据,使用原生redis操作
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonWithOriginalRedis() {
        /**
         * 1、空结果缓存，解决缓存穿透
         * 2、设置过期时间(加随机值)，解决缓存雪崩
         * 3、加锁，解决缓存击穿
         */
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String catalogJSON = ops.get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            //没有在缓存中查到数据，从数据库获取json数据
            System.out.println("缓存不命中......查询数据库......");
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedisLock();
            //这里会有问题，如果在从数据库返回数据后，还没有将数据放入到redis中，其他线程发请求，就会多次访问DB，锁的范围需要扩大到放入到redis后
            //得到数据后放入到缓存中去
//            String s = JSON.toJSONString(catalogJsonFromDb);
//            //设置缓存过期时间
//            ops.set("catalogJSON",s,1, TimeUnit.DAYS);
            return catalogJsonFromDb;

        }
        System.out.println("缓存命中......直接返回......");
        //执行到这里说明缓存已经命中，反序列化为Java对象进行返回
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {});

        return result;
    }

    /**
     * 使用本地锁的方式查询数据库获取分类的json对象
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {
        //TODO 使用本地锁synchronized，JUC(lock)，在分布式情况下想锁住所有的线程必须使用分布式锁
        synchronized (this) {
            //上锁，在获取锁之后不能直接去DB仍然需要先在redis中查询是否存在，如果存在就不用去DB中查询了，不然当时并发排队的线程都要查询DB
            String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
            if (!StringUtils.isEmpty(catalogJSON)) {
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
                return result;
            }
            System.out.println("查询了数据库......");
            //优化：先将所有的数据查询出来，然后再进行一级、二级分类的查询及组装-->只查询一次数据库
            List<CategoryEntity> allCategories = baseMapper.selectList(null);

            //1、查出所有的一级分类
            List<CategoryEntity> levelOneCatagories = getCategoriesByParentCid(allCategories,0L);
            //根据所有的一级分类找到对应的二级分类进行封装
            Map<String, List<Catelog2Vo>> collectLevel1AsMap = levelOneCatagories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //根据一级分类查询二级分类列表
                List<CategoryEntity> levelTwoCatagories = getCategoriesByParentCid(allCategories,v.getCatId());
                List<Catelog2Vo> collectLevel2 = null;
                if (levelTwoCatagories != null) {
                    collectLevel2 = levelTwoCatagories.stream().map(l2 -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //查询对应的三级id
                        List<CategoryEntity> levelThreeCatagories = getCategoriesByParentCid(allCategories,l2.getCatId());
                        List<Catelog2Vo.Catelog3Vo> collectLevel3 = null;
                        if (levelThreeCatagories != null) {
                            collectLevel3 = levelThreeCatagories.stream().map(l3 -> {
                                Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return catelog3Vo;
                            }).collect(Collectors.toList());
                        }
                        catelog2Vo.setCatalog3List(collectLevel3);
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }
                return collectLevel2;
            }));
            //查库和放入缓存成为原子操作
            String s = JSON.toJSONString(collectLevel1AsMap);
            //设置缓存过期时间
            redisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
            return collectLevel1AsMap;
        }
    }

    /**
     * 使用redis分布式锁查询数据库获取分类的json对象
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        //1、占用分布式锁，去redis占坑
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,300,TimeUnit.SECONDS);
        if (lock) {
            //加锁成功，查询数据库逻辑
            //如果在代码执行的过程中断电了或者出现异常了，程序无法正常的释放锁，就会产生死锁的问题，所以需要设置锁的过期时间
            //2、设置过期时间，必须和加锁是同步的，原子操作,如果不是原子操作，当设置好锁然后程序还没设置过期时间这段时间宕机了，那程序又死锁了
//            redisTemplate.expire("lock",30,TimeUnit.SECONDS);
            Map<String, List<Catelog2Vo>> dataFromDb = null;
            try {
                dataFromDb = getCatalogJsonFromDb();
            } finally {
                //删除锁的原子操作
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<Integer>(script,Integer.class), Arrays.asList("lock"),uuid);
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if (uuid.equals(lockValue)) {
//                //如果获取到的是自己的锁就删除锁
//                redisTemplate.delete("lock");
//            }
                return dataFromDb;
            }

        } else {
            //加锁失败，自旋获取锁
            while (true) {
                lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid);
                if (lock) {
                    break;
                }
            }
            Map<String, List<Catelog2Vo>> dataFromDb = null;
            try {
                dataFromDb = getCatalogJsonFromDb();
            } finally {
                //删除锁的原子操作
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<Integer>(script,Integer.class), Arrays.asList("lock"),uuid);
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if (uuid.equals(lockValue)) {
//                //如果获取到的是自己的锁就删除锁
//                redisTemplate.delete("lock");
//            }
                return dataFromDb;
            }
        }
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDb(){
            //上锁，在获取锁之后不能直接去DB仍然需要先在redis中查询是否存在，如果存在就不用去DB中查询了，不然当时并发排队的线程都要查询DB
            String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
            if (!StringUtils.isEmpty(catalogJSON)) {
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
                return result;
            }
            System.out.println("查询了数据库......");
            //优化：先将所有的数据查询出来，然后再进行一级、二级分类的查询及组装-->只查询一次数据库
            List<CategoryEntity> allCategories = baseMapper.selectList(null);

            //1、查出所有的一级分类
            List<CategoryEntity> levelOneCatagories = getCategoriesByParentCid(allCategories,0L);
            //根据所有的一级分类找到对应的二级分类进行封装
            Map<String, List<Catelog2Vo>> collectLevel1AsMap = levelOneCatagories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //根据一级分类查询二级分类列表
                List<CategoryEntity> levelTwoCatagories = getCategoriesByParentCid(allCategories,v.getCatId());
                List<Catelog2Vo> collectLevel2 = null;
                if (levelTwoCatagories != null) {
                    collectLevel2 = levelTwoCatagories.stream().map(l2 -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //查询对应的三级id
                        List<CategoryEntity> levelThreeCatagories = getCategoriesByParentCid(allCategories,l2.getCatId());
                        List<Catelog2Vo.Catelog3Vo> collectLevel3 = null;
                        if (levelThreeCatagories != null) {
                            collectLevel3 = levelThreeCatagories.stream().map(l3 -> {
                                Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return catelog3Vo;
                            }).collect(Collectors.toList());
                        }
                        catelog2Vo.setCatalog3List(collectLevel3);
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }
                return collectLevel2;
            }));
            //查库和放入缓存成为原子操作
            String s = JSON.toJSONString(collectLevel1AsMap);
            //设置缓存过期时间
            redisTemplate.opsForValue().set("catalogJSON",s,1, TimeUnit.DAYS);
            return collectLevel1AsMap;
        }




    /**
     * 通过parentCid找到对应的分类实体列表
     * @param allCategories
     * @param parentCid
     * @return
     */
    private List<CategoryEntity> getCategoriesByParentCid(List<CategoryEntity> allCategories,Long parentCid) {
        List<CategoryEntity> collect = allCategories.stream().filter(item -> item.getParentCid() == parentCid).collect(Collectors.toList());
        return collect;
    }

    private List<CategoryEntity> setChildrenMenu(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> collect = all.stream().filter(categoryEntity -> categoryEntity.getParentCid() == root.getCatId())
                .map(m -> {
                    m.setChildren(setChildrenMenu(m, all));
                    return m;
                }).sorted((m1, m2) -> (m1.getSort() == null ? 0 : m1.getSort()) - (m2.getSort() == null ? 0 : m2.getSort())).collect(Collectors.toList());
        return collect;
    }


}