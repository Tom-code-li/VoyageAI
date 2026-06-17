package com.example.travelassistant.service;

import com.example.travelassistant.dto.AiGenerateRequest;
import com.example.travelassistant.dto.AiTravelPlanDTO;
import com.example.travelassistant.dto.AgentSessionResponse;
import com.example.travelassistant.dto.DayRoutePlanResponse;
import com.example.travelassistant.dto.ItineraryDetailResponse;
import com.example.travelassistant.dto.ItineraryListItemVO;
import com.example.travelassistant.dto.RoutePlanRequest;
import com.example.travelassistant.dto.ItinerarySaveResponse;
import com.example.travelassistant.dto.SaveItineraryRequest;

import java.util.List;

public interface ItineraryService {

    AgentSessionResponse generateDraft(AiGenerateRequest request);

    ItinerarySaveResponse saveItinerary(SaveItineraryRequest request);

    DayRoutePlanResponse planDayRoute(RoutePlanRequest request);

    ItineraryDetailResponse getItineraryDetail(Long id);

    List<ItineraryListItemVO> listUserItineraries(Long userId, Integer status);

    void deleteDetail(Long detailId);

    void deleteItinerary(Long itineraryId);

    void refreshStatus();
}
