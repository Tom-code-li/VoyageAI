package com.example.travelassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.travelassistant.dto.ChartItemVO;
import com.example.travelassistant.entity.Attraction;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AttractionMapper extends BaseMapper<Attraction> {

    List<Attraction> selectByCity(@Param("city") String city);

    List<ChartItemVO> selectCityDistribution();
}
