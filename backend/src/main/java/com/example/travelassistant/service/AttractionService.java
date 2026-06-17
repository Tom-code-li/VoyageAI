package com.example.travelassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.travelassistant.dto.AttractionCreateRequest;
import com.example.travelassistant.dto.AttractionUpdateRequest;
import com.example.travelassistant.dto.ChartItemVO;
import com.example.travelassistant.entity.Attraction;

import java.util.List;

public interface AttractionService {

    IPage<Attraction> pageAttractions(long current, long size, String keyword, String city);

    Attraction getAttraction(Long id);

    List<Attraction> listByCity(String city);

    List<Attraction> listTopReferenced(int limit);

    List<ChartItemVO> listCityDistribution();

    Attraction createAttraction(AttractionCreateRequest request);

    Attraction updateAttraction(AttractionUpdateRequest request);

    void deleteAttraction(Long id, Long operatorUserId);
}
