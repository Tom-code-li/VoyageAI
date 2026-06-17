package com.example.travelassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.travelassistant.dto.ItineraryDetailResponse;
import com.example.travelassistant.entity.Itinerary;
import org.apache.ibatis.annotations.Param;

public interface ItineraryMapper extends BaseMapper<Itinerary> {

    ItineraryDetailResponse selectItineraryDetail(@Param("id") Long id);
}
