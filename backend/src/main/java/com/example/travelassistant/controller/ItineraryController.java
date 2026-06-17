package com.example.travelassistant.controller;

import com.example.travelassistant.common.Result;
import com.example.travelassistant.dto.AiGenerateRequest;
import com.example.travelassistant.dto.AgentSessionResponse;
import com.example.travelassistant.dto.DayRoutePlanResponse;
import com.example.travelassistant.dto.ItineraryDetailResponse;
import com.example.travelassistant.dto.ItineraryListItemVO;
import com.example.travelassistant.dto.ItinerarySaveResponse;
import com.example.travelassistant.dto.RoutePlanRequest;
import com.example.travelassistant.dto.SaveItineraryRequest;
import com.example.travelassistant.service.ItineraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 行程相关控制器。
 * 这里聚合了前端“智能规划”页和“我的行程”页所依赖的主要接口：
 * 1. AI 生成或继续修改一份行程草稿
 * 2. 基于某一天的景点顺序规划路线
 * 3. 把当前草稿正式保存为用户行程
 * 4. 查询、删除行程及其明细
 *
 * 这个类本身只负责路由分发和参数校验，
 * 具体业务逻辑都下沉在 ItineraryServiceImpl 中。
 */
@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    @PostMapping("/ai/generate")
    public Result<AgentSessionResponse> generate(@Valid @RequestBody AiGenerateRequest request) {
        return Result.success(itineraryService.generateDraft(request));
    }

    /**
     * 为草稿中的某一天生成实际路线信息。
     * 返回值中会包含：
     * - 点位列表
     * - 路线步骤
     * - 距离、时长
     * - 自然语言路线摘要
     */
    @PostMapping("/route/day")
    public Result<DayRoutePlanResponse> planDayRoute(@Valid @RequestBody RoutePlanRequest request) {
        return Result.success(itineraryService.planDayRoute(request));
    }

    /**
     * 保存当前草稿。
     * 前端会把最终确认后的 dailySchedules 直接传回后端，
     * 后端据此重写 itinerary_detail 和 itinerary_day_plan。
     */
    @PostMapping("/save")
    public Result<ItinerarySaveResponse> save(@Valid @RequestBody SaveItineraryRequest request) {
        return Result.success(itineraryService.saveItinerary(request));
    }

    /**
     * 查询单条行程详情。
     * 该接口返回的结构是“行程 -> 每天 -> 景点列表”的树状结构，
     * 适合前端详情页用时间轴或按天分组方式展示。
     */
    @GetMapping("/{id}")
    public Result<ItineraryDetailResponse> detail(@PathVariable Long id) {
        return Result.success(itineraryService.getItineraryDetail(id));
    }

    /**
     * 查询某个用户的行程列表。
     * 这里保留了 status 参数，是为了兼容更早期的筛选设计；
     * 当前版本主要按 userId 维度返回该用户的全部行程。
     */
    @GetMapping("/user/{userId}")
    public Result<List<ItineraryListItemVO>> userList(@PathVariable Long userId,
                                                      @RequestParam(required = false) Integer status) {
        return Result.success(itineraryService.listUserItineraries(userId, status));
    }

    /**
     * 删除某条行程中的单个景点安排。
     * 删除后并不会删除 attraction 主表中的景点基础信息，
     * 只是把这条行程明细移除，并同步减少景点引用次数。
     */
    @DeleteMapping("/detail/{detailId}")
    public Result<Void> deleteDetail(@PathVariable Long detailId) {
        itineraryService.deleteDetail(detailId);
        return Result.success("删除成功", null);
    }

    /**
     * 删除整条行程。
     * 会同时清理 itinerary、itinerary_detail、chat_message、itinerary_day_plan 等关联数据。
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteItinerary(@PathVariable Long id) {
        itineraryService.deleteItinerary(id);
        return Result.success("删除成功", null);
    }
}
