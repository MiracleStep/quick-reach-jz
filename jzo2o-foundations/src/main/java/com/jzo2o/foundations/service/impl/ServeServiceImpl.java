package com.jzo2o.foundations.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.ChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jzo2o.common.expcetions.CommonException;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.utils.BeanUtils;
import com.jzo2o.common.utils.ObjectUtils;
import com.jzo2o.foundations.enums.FoundationStatusEnum;
import com.jzo2o.foundations.mapper.RegionMapper;
import com.jzo2o.foundations.mapper.ServeItemMapper;
import com.jzo2o.foundations.mapper.ServeMapper;
import com.jzo2o.foundations.model.domain.Region;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.domain.ServeItem;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServeUpsertReqDTO;
import com.jzo2o.foundations.model.dto.response.ServeResDTO;
import com.jzo2o.foundations.service.IServeService;
import com.jzo2o.mysql.utils.PageHelperUtils;
import jodd.introspector.Mapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 服务表 服务实现类
 * </p>
 *
 * @author
 * @since 2024-06-17
 */
@Service
public class ServeServiceImpl extends ServiceImpl<ServeMapper, Serve> implements IServeService {
    @Resource
    private ServeItemMapper serveItemMapper;

    @Resource
    private RegionMapper regionMapper;
    /**
     * 区域服务分页查询服务
     *
     * @param servePageQueryReqDTO
     * @returnList<ServeResDTO>
     */
    @Override
    public PageResult<ServeResDTO> page(ServePageQueryReqDTO servePageQueryReqDTO) {
        PageResult<ServeResDTO> serveResDTOPageResult = PageHelperUtils.selectPage(servePageQueryReqDTO,
                () -> baseMapper.queryServeListByRegionId(servePageQueryReqDTO.getRegionId()));
        return serveResDTOPageResult;
    }

    @Override
    public void batchAdd(List<ServeUpsertReqDTO> serveUpsertReqDTOList) {
        //遍历serveUpsertReqDTOList
        for (ServeUpsertReqDTO serveUpsertReqDTO : serveUpsertReqDTOList) {
            //合法性校验
            //serve_item合法校验，如果未启用不能添加
            ServeItem serveItem = serveItemMapper.selectById(serveUpsertReqDTO.getServeItemId());
            if (ObjectUtils.isNull(serveItem) || serveItem.getActiveStatus() != FoundationStatusEnum.ENABLE.getStatus()) {
                //抛出异常
                throw new ForbiddenOperationException("服务项不存在或服务项未启动不允许添加");
            }
            //同一个区域下不能添加相同的服务
            //sql: select count(*) from serve where serve_item_id = ? and region_id = ?
            Integer count = lambdaQuery()//相当于new LambdaQueryChainWrapper<>()
                    .eq(Serve::getServeItemId, serveUpsertReqDTO.getServeItemId())
                    .eq(Serve::getRegionId, serveUpsertReqDTO.getRegionId())
                    .count();
            if (count > 0) {
                throw new ForbiddenOperationException(serveItem.getName()+"服务已经存在");
            }
            //像serve插入数据
            Serve serve = BeanUtils.toBean(serveUpsertReqDTO, Serve.class);
            Long regionId = serve.getRegionId();//区域Id
            Region region = regionMapper.selectById(regionId);
            String cityCode = region.getCityCode();
            serve.setCityCode(cityCode);
            //向serve表插入数据
            baseMapper.insert(serve);
        }
    }

    @Override
    public Serve update(Long id, BigDecimal price) {
        boolean update = lambdaUpdate()
                .eq(Serve::getId, id)
                .set(Serve::getPrice, price)
                .update();
        if (!update) {
            throw new CommonException("修改价格服务失败");
        }
        //查询serve数据
        Serve serve = baseMapper.selectById(id);
        return serve;
    }

    @Override
    public Serve onSale(Long id) {
        //根据id查询serve信息
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtils.isNull(serve)) {
            throw new ForbiddenOperationException("区域服务信息不存在");
        }
        //如果serve的sale_status为0或1可以上架
        Integer saleStatus = serve.getSaleStatus(); //售卖状态
        if (!(saleStatus == FoundationStatusEnum.INIT.getStatus() || saleStatus == FoundationStatusEnum.DISABLE.getStatus())) {
            throw new ForbiddenOperationException("区域服务的状态是草稿或下架时方可上架");
        }

        //如果服务项没有启用不能上架
        Long serveItemId = serve.getServeItemId();
        ServeItem serveItem = serveItemMapper.selectById(serveItemId);
        Integer activeStatus = serveItem.getActiveStatus();//状态
        if (activeStatus != FoundationStatusEnum.ENABLE.getStatus()) {
            throw new ForbiddenOperationException("服务项的状态未启用不能上架");
        }
        //更新sale_status
        boolean update = lambdaUpdate()
                .eq(Serve::getId, id)
                .set(Serve::getSaleStatus, FoundationStatusEnum.ENABLE.getStatus())
                .update();
        if (!update) {
            throw new CommonException("服务上架失败");
        }

        return baseMapper.selectById(id);
    }
}
