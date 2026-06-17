package com.example.travelassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.travelassistant.dto.AttractionCreateRequest;
import com.example.travelassistant.dto.AttractionUpdateRequest;
import com.example.travelassistant.dto.ChartItemVO;
import com.example.travelassistant.entity.Attraction;
import com.example.travelassistant.entity.ItineraryDetail;
import com.example.travelassistant.entity.User;
import com.example.travelassistant.exception.BusinessException;
import com.example.travelassistant.mapper.AttractionMapper;
import com.example.travelassistant.mapper.ItineraryDetailMapper;
import com.example.travelassistant.mapper.UserMapper;
import com.example.travelassistant.service.AttractionService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 景点服务实现。
 * 负责公共景点知识库的增删改查，以及按城市统计等后台管理业务。
 */
@Service
@RequiredArgsConstructor
public class AttractionServiceImpl implements AttractionService {

    private final AttractionMapper attractionMapper;
    private final UserMapper userMapper;
    private final ItineraryDetailMapper itineraryDetailMapper;

    @Override
    public IPage<Attraction> pageAttractions(long current, long size, String keyword, String city) {
        // 广场页和管理员管理页都复用这一套分页查询能力。
        LambdaQueryWrapper<Attraction> wrapper = new LambdaQueryWrapper<Attraction>()
                .like(StringUtils.isNotBlank(keyword), Attraction::getName, keyword)
                .eq(StringUtils.isNotBlank(city), Attraction::getCity, city)
                .orderByDesc(Attraction::getReferenceCount)
                .orderByDesc(Attraction::getUpdateTime)
                .orderByDesc(Attraction::getId);
        return attractionMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public Attraction getAttraction(Long id) {
        // 按主键读取单个景点，常用于管理员进入编辑页时回显详情。
        Attraction attraction = attractionMapper.selectById(id);
        if (attraction == null) {
            throw new BusinessException("景点不存在");
        }
        return attraction;
    }

    @Override
    public List<Attraction> listByCity(String city) {
        // 该能力适合下拉筛选、城市维度联动等场景。
        if (StringUtils.isBlank(city)) {
            throw new BusinessException("城市不能为空");
        }
        return attractionMapper.selectByCity(city.trim());
    }

    @Override
    public List<Attraction> listTopReferenced(int limit) {
        int safeLimit = Math.max(1, limit);
        return attractionMapper.selectList(new LambdaQueryWrapper<Attraction>()
                .orderByDesc(Attraction::getReferenceCount)
                .orderByDesc(Attraction::getUpdateTime)
                .orderByDesc(Attraction::getId)
                .last("limit " + safeLimit));
    }

    @Override
    public List<ChartItemVO> listCityDistribution() {
        // 返回城市景点分布统计数据，供 ECharts 图表使用。
        return attractionMapper.selectCityDistribution();
    }

    @Override
    public Attraction createAttraction(AttractionCreateRequest request) {
        // 只有管理员才能维护公共景点知识库。
        assertAdmin(request.getOperatorUserId());

        // 同一城市下不允许出现完全同名的景点，保持知识库整洁。
        Attraction exists = attractionMapper.selectOne(new LambdaQueryWrapper<Attraction>()
                .eq(Attraction::getCity, request.getCity().trim())
                .eq(Attraction::getName, request.getName().trim())
                .last("limit 1"));
        if (exists != null) {
            throw new BusinessException("该城市下已存在同名景点");
        }

        Attraction attraction = new Attraction();
        attraction.setCity(request.getCity().trim());
        attraction.setName(request.getName().trim());
        attraction.setDescription(StringUtils.defaultString(request.getDescription()).trim());
        attraction.setImageUrl(StringUtils.trimToNull(request.getImageUrl()));
        attraction.setPlayTime(request.getSuggestedHours());
        attraction.setLongitude(request.getLongitude());
        attraction.setLatitude(request.getLatitude());
        attraction.setReferenceCount(0);
        attraction.setCreateTime(LocalDateTime.now());
        attraction.setUpdateTime(LocalDateTime.now());
        attractionMapper.insert(attraction);
        return attraction;
    }

    @Override
    public Attraction updateAttraction(AttractionUpdateRequest request) {
        // 管理员编辑已有景点。
        assertAdmin(request.getOperatorUserId());

        Attraction attraction = attractionMapper.selectById(request.getId());
        if (attraction == null) {
            throw new BusinessException("景点不存在");
        }
        if (StringUtils.isNotBlank(request.getCity())) {
            attraction.setCity(request.getCity());
        }
        if (StringUtils.isNotBlank(request.getName())) {
            attraction.setName(request.getName());
        }
        if (StringUtils.isNotBlank(request.getDescription())) {
            attraction.setDescription(request.getDescription());
        }
        if (StringUtils.isNotBlank(request.getImageUrl())) {
            attraction.setImageUrl(request.getImageUrl());
        }
        if (request.getSuggestedHours() != null) {
            attraction.setPlayTime(request.getSuggestedHours());
        }
        if (request.getLongitude() != null) {
            attraction.setLongitude(request.getLongitude());
        }
        if (request.getLatitude() != null) {
            attraction.setLatitude(request.getLatitude());
        }
        attraction.setUpdateTime(LocalDateTime.now());
        attractionMapper.updateById(attraction);
        return attraction;
    }

    @Override
    public void deleteAttraction(Long id, Long operatorUserId) {
        assertAdmin(operatorUserId);
        Attraction attraction = attractionMapper.selectById(id);
        if (attraction == null) {
            throw new BusinessException("景点不存在");
        }
        // 如果景点已经被行程引用，就不允许直接删除，避免破坏历史行程数据。
        Long referenced = itineraryDetailMapper.selectCount(new LambdaQueryWrapper<ItineraryDetail>()
                .eq(ItineraryDetail::getAttractionId, id));
        if (referenced != null && referenced > 0) {
            throw new BusinessException("该景点已被行程引用，无法直接删除");
        }
        attractionMapper.deleteById(id);
    }

    /** 校验操作者是否为管理员。 */
    private void assertAdmin(Long operatorUserId) {
        User operator = userMapper.selectById(operatorUserId);
        if (operator == null || !"ADMIN".equals(operator.getRole())) {
            throw new BusinessException("无权操作公共数据");
        }
    }
}
