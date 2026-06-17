package com.example.travelassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.travelassistant.config.AiProperties;
import com.example.travelassistant.config.AmapProperties;
import com.example.travelassistant.dto.AiGenerateRequest;
import com.example.travelassistant.dto.AiTravelPlanDTO;
import com.example.travelassistant.dto.AgentSessionResponse;
import com.example.travelassistant.dto.ChatMessageDTO;
import com.example.travelassistant.dto.DayRoutePlanResponse;
import com.example.travelassistant.dto.ItineraryDetailResponse;
import com.example.travelassistant.dto.ItineraryListItemVO;
import com.example.travelassistant.dto.ItinerarySaveResponse;
import com.example.travelassistant.dto.RoutePlanRequest;
import com.example.travelassistant.dto.SaveItineraryRequest;
import com.example.travelassistant.entity.Attraction;
import com.example.travelassistant.entity.ChatMessage;
import com.example.travelassistant.entity.Itinerary;
import com.example.travelassistant.entity.ItineraryDayPlan;
import com.example.travelassistant.entity.ItineraryDetail;
import com.example.travelassistant.entity.User;
import com.example.travelassistant.exception.BusinessException;
import com.example.travelassistant.mapper.AttractionMapper;
import com.example.travelassistant.mapper.ChatMessageMapper;
import com.example.travelassistant.mapper.ItineraryDayPlanMapper;
import com.example.travelassistant.mapper.ItineraryDetailMapper;
import com.example.travelassistant.mapper.ItineraryMapper;
import com.example.travelassistant.mapper.UserMapper;
import com.example.travelassistant.service.ItineraryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 行程服务实现。
 *
 * 这是整个项目最核心的后端文件，负责把“自然语言需求 -> AI 草稿 -> 地图路线 -> 本地知识沉淀 -> 保存行程”
 * 这条完整业务链串起来。
 *
 * 你在阅读这个类时，可以重点关注下面几条主线：
 * 1. generateDraft：AI 生成或续写一份草稿
 * 2. normalizePlan / ensureReasonableDailyAttractions：规范化 AI 输出
 * 3. hydrateAttractions / upsertAttraction：把草稿景点和本地知识库对齐
 * 4. buildDayRoutePlan：给某一天规划路线
 * 5. saveItinerary / rewriteItineraryDetails / rewriteDayPlans：最终保存
 */
@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private static final int DEFAULT_ATTRACTIONS_PER_DAY = 2;

    private static final String SYSTEM_PROMPT = """
            你是一个资深AI旅游规划师。用户会用自然语言输入旅游需求，也可能基于现有草稿要求你局部修改。
            你的任务是解析需求，并严格输出纯JSON，绝不要Markdown、解释性文字或额外问候。
            你必须尽可能从用户输入里提取城市、天数与可选日期；如果用户没有提供日期，可以让 startDate 和 endDate 为空。
            你必须输出完整草稿，不要只输出差异。
            每一天的景点数量必须根据行程强度灵活安排，而不是固定数量。
            如果景点之间相距较近、单个景点建议游玩时长较短，可以适当安排 3 个或更多景点；
            如果景点之间相距较远，或者单个景点建议游玩时长较长，则每天只安排 1 到 2 个景点更合理。
            你必须优先保证路线顺路、节奏自然，严禁为了凑数量而塞入明显不顺路或会导致行程过满的景点。
            输出景点 name 时，必须使用官方标准名称（如：峨眉山、故宫博物院）。严禁附加任何省份前缀、游玩时长或括号备注，否则视为任务失败！
            严格使用如下JSON结构：
            {
              "title": "行程标题",
              "destination": "提取出的城市名",
              "startDate": "YYYY-MM-DD 或 null",
              "endDate": "YYYY-MM-DD 或 null",
              "totalDays": 4,
              "dailySchedules": [
                {
                  "dayNumber": 1,
                  "attractions": [
                    {
                      "name": "景点标准名称",
                      "description": "约40到70字的简介，兼顾体验与美食建议",
                      "suggestedHours": 2.5
                    }
                  ]
                }
              ]
            }
            如果用户是在现有草稿基础上修改，请保留未被修改的其余安排，并让每天的顺序自然合理。
            """;

    private static final String ROUTE_SUMMARY_PROMPT = """
            你是一位旅游路线解读助手。请根据提供的单日景点顺序、交通路线和导航步骤，
            用自然中文生成一段 80 到 140 字的总结，说明这一天的路线节奏、交通特征和适合用户注意的点。
            严禁输出 Markdown、标题、编号或额外客套话，只返回一段纯文本总结。
            """;

    private static final Pattern DAY_COUNT_PATTERN = Pattern.compile("([0-9一二两三四五六七八九十]+)天");
    private static final Pattern DAY_NUMBER_PATTERN = Pattern.compile("第([0-9一二两三四五六七八九十]+)天");
    private static final Pattern DELETE_PATTERN = Pattern.compile("(删掉|删除|去掉)(.+)");
    private static final Pattern CITY_PATTERN = Pattern.compile("去([\\p{IsHan}A-Za-z]{2,12})(?:玩|旅游|旅行|出发|逛)");

    private final ItineraryMapper itineraryMapper;
    private final ItineraryDetailMapper itineraryDetailMapper;
    private final ItineraryDayPlanMapper itineraryDayPlanMapper;
    private final AttractionMapper attractionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final AmapProperties amapProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSessionResponse generateDraft(AiGenerateRequest request) {
        // 1. 先尝试让大模型生成完整草稿；失败时自动回退到本地规则规划。
        AiTravelPlanDTO plan = fetchAiPlan(request);
        // 2. 统一解析目的地、日期、总天数等上下文。
        PlanningContext context = resolvePlanningContext(request, plan);
        // 3. 清洗 AI 输出，修正天数字段、景点顺序、默认描述等。
        normalizePlan(context, request, plan);
        // 4. 对景点数量做柔性补足，避免机械地把每天都补到固定数量。
        ensureReasonableDailyAttractions(context, request, plan);
        // 5. 用本地知识库和高德数据补全经纬度、图片、标准名称。
        hydrateAttractions(context.destination(), plan);
        // 6. 把草稿和对话写入数据库，形成一份可继续追问修改的会话。
        Itinerary itinerary = persistAgentSession(request, context, plan);
        return buildAgentSessionResponse(itinerary.getId(), plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItinerarySaveResponse saveItinerary(SaveItineraryRequest request) {
        // 保存并不要求重新走 AI，只需要把前端已经确认的草稿结构正式落库。
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在，无法保存行程");
        }
        Itinerary itinerary = request.getItineraryId() == null ? null : itineraryMapper.selectById(request.getItineraryId());
        if (itinerary == null) {
            itinerary = new Itinerary();
            itinerary.setUserId(request.getUserId());
            itinerary.setTitle(request.getTitle());
            itinerary.setCity(request.getCity());
            itinerary.setCreateTime(LocalDateTime.now());
            itineraryMapper.insert(itinerary);
        } else {
            itinerary.setTitle(request.getTitle());
            itinerary.setCity(request.getCity());
            itineraryMapper.updateById(itinerary);
        }

        AiTravelPlanDTO plan = new AiTravelPlanDTO();
        plan.setTitle(request.getTitle());
        plan.setDestination(request.getCity());
        plan.setDailySchedules(Optional.ofNullable(request.getDailySchedules()).orElse(Collections.emptyList()));
        plan.setTotalDays(Optional.ofNullable(request.getDailySchedules()).orElse(Collections.emptyList()).size());

        // 重写景点明细表和每日计划表，确保数据库状态与前端确认结果完全一致。
        rewriteItineraryDetails(itinerary.getId(), request.getCity(), plan);
        rewriteDayPlans(itinerary.getId(), plan, Boolean.TRUE.equals(request.getModified()));
        return new ItinerarySaveResponse(itinerary.getId());
    }

    @Override
    public DayRoutePlanResponse planDayRoute(RoutePlanRequest request) {
        // 这个接口专门服务于前端地图联动场景。
        return buildDayRoutePlan(
                Optional.ofNullable(request.getDraft()).orElseThrow(() -> new BusinessException("草稿不能为空")),
                Optional.ofNullable(request.getDayNumber()).orElseThrow(() -> new BusinessException("天数不能为空")),
                StringUtils.defaultIfBlank(request.getMode(), "driving"),
                true);
    }

    @Override
    public ItineraryDetailResponse getItineraryDetail(Long id) {
        // 详情页需要的是已经按“天 -> 景点顺序”整理好的树状结构。
        ItineraryDetailResponse response = itineraryMapper.selectItineraryDetail(id);
        if (response == null) {
            throw new BusinessException("行程不存在");
        }
        response.setDailySchedules(Optional.ofNullable(response.getDailySchedules()).orElseGet(ArrayList::new));
        response.getDailySchedules().removeIf(day -> day.getDayNumber() == null);
        response.getDailySchedules().sort(Comparator.comparing(
                ItineraryDetailResponse.DailyScheduleVO::getDayNumber, Comparator.nullsLast(Integer::compareTo)));
        response.getDailySchedules().forEach(day -> {
            day.setAttractions(Optional.ofNullable(day.getAttractions()).orElseGet(ArrayList::new));
            day.getAttractions().removeIf(item -> item.getDetailId() == null);
            day.getAttractions().sort(Comparator.comparing(
                    ItineraryDetailResponse.AttractionVO::getSortOrder, Comparator.nullsLast(Integer::compareTo)));
        });
        response.setTotalDays(response.getDailySchedules().size());
        return response;
    }

    @Override
    public List<ItineraryListItemVO> listUserItineraries(Long userId, Integer status) {
        // 列表页会展示标题、城市、总天数、景点数、封面、首日摘要等信息。
        List<Itinerary> itineraries = itineraryMapper.selectList(new LambdaQueryWrapper<Itinerary>()
                .eq(Itinerary::getUserId, userId)
                .orderByDesc(Itinerary::getCreateTime)
                .orderByDesc(Itinerary::getId));

        if (itineraries.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> itineraryIds = itineraries.stream().map(Itinerary::getId).toList();
        Map<Long, Long> countMap = itineraryDetailMapper.selectMaps(new QueryWrapper<ItineraryDetail>()
                        .select("itinerary_id", "count(*) as total")
                        .in("itinerary_id", itineraryIds)
                        .groupBy("itinerary_id"))
                .stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("itinerary_id")).longValue(),
                        map -> ((Number) map.get("total")).longValue()));

        Map<Long, List<ItineraryDayPlan>> dayPlanMap = itineraryDayPlanMapper.selectList(new LambdaQueryWrapper<ItineraryDayPlan>()
                        .in(ItineraryDayPlan::getItineraryId, itineraryIds)
                        .orderByAsc(ItineraryDayPlan::getDayNumber))
                .stream()
                .collect(Collectors.groupingBy(ItineraryDayPlan::getItineraryId));

        Map<Long, String> coverImageMap = buildItineraryCoverImageMap(itineraryIds);

        return itineraries.stream().map(itinerary -> {
            ItineraryListItemVO item = new ItineraryListItemVO();
            item.setId(itinerary.getId());
            item.setTitle(itinerary.getTitle());
            item.setCity(itinerary.getCity());
            item.setCreateTime(itinerary.getCreateTime());
            item.setTotalDays(dayPlanMap.getOrDefault(itinerary.getId(), Collections.emptyList()).size());
            item.setAttractionCount(countMap.getOrDefault(itinerary.getId(), 0L).intValue());
            item.setCoverImageUrl(coverImageMap.get(itinerary.getId()));
            item.setRouteSummary(dayPlanMap.getOrDefault(itinerary.getId(), Collections.emptyList()).stream()
                    .findFirst()
                    .map(ItineraryDayPlan::getRouteSummary)
                    .orElse(null));
            return item;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDetail(Long detailId) {
        // 删除单个景点安排后，要同步减少景点被引用次数。
        ItineraryDetail detail = itineraryDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new BusinessException("行程明细不存在");
        }
        itineraryDetailMapper.deleteById(detailId);
        decrementAttractionReference(detail.getAttractionId(), 1L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItinerary(Long itineraryId) {
        // 删除整条行程前，先统计它引用了哪些景点，后面要批量回收 reference_count。
        Itinerary itinerary = itineraryMapper.selectById(itineraryId);
        if (itinerary == null) {
            throw new BusinessException("行程不存在");
        }
        List<ItineraryDetail> details = itineraryDetailMapper.selectList(new LambdaQueryWrapper<ItineraryDetail>()
                .eq(ItineraryDetail::getItineraryId, itineraryId));
        Map<Long, Long> grouped = details.stream()
                .filter(detail -> detail.getAttractionId() != null)
                .collect(Collectors.groupingBy(ItineraryDetail::getAttractionId, Collectors.counting()));
        itineraryDetailMapper.delete(new LambdaQueryWrapper<ItineraryDetail>()
                .eq(ItineraryDetail::getItineraryId, itineraryId));
        itineraryMapper.deleteById(itineraryId);
        grouped.forEach(this::decrementAttractionReference);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshStatus() {
        // no-op: itinerary no longer uses status transitions
    }

    private Itinerary persistAgentSession(AiGenerateRequest request, PlanningContext context, AiTravelPlanDTO plan) {
        if (request.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Itinerary itinerary = resolveOrCreateItinerary(request, context, plan);
        // 会话阶段也会把草稿结构同步落成明细，便于刷新后继续编辑。
        rewriteItineraryDetails(itinerary.getId(), context.destination(), plan);
        // 记录用户消息和 AI 回复，形成连续对话。
        appendChatMessages(itinerary.getId(), request.getMessage(), buildAssistantSummary(plan));
        return itinerary;
    }

    private Itinerary resolveOrCreateItinerary(AiGenerateRequest request, PlanningContext context, AiTravelPlanDTO plan) {
        Itinerary itinerary = request.getItineraryId() == null ? null : itineraryMapper.selectById(request.getItineraryId());
        LocalDateTime now = LocalDateTime.now();
        if (itinerary == null) {
            itinerary = new Itinerary();
            itinerary.setUserId(request.getUserId());
            itinerary.setTitle(plan.getTitle());
            itinerary.setCity(context.destination());
            itinerary.setCreateTime(now);
            itineraryMapper.insert(itinerary);
        } else {
            itinerary.setTitle(plan.getTitle());
            itinerary.setCity(context.destination());
            itineraryMapper.updateById(itinerary);
        }
        return itinerary;
    }

    private void rewriteItineraryDetails(Long itineraryId, String city, AiTravelPlanDTO plan) {
        // 这里使用“先删后重建”的方式，保证数据库里明细顺序与当前草稿完全一致。
        List<ItineraryDetail> existingDetails = itineraryDetailMapper.selectList(new LambdaQueryWrapper<ItineraryDetail>()
                .eq(ItineraryDetail::getItineraryId, itineraryId));
        Map<Long, Long> grouped = existingDetails.stream()
                .filter(detail -> detail.getAttractionId() != null)
                .collect(Collectors.groupingBy(ItineraryDetail::getAttractionId, Collectors.counting()));
        itineraryDetailMapper.delete(new LambdaQueryWrapper<ItineraryDetail>()
                .eq(ItineraryDetail::getItineraryId, itineraryId));
        grouped.forEach(this::decrementAttractionReference);

        for (AiTravelPlanDTO.DailyScheduleDTO schedule : Optional.ofNullable(plan.getDailySchedules()).orElse(Collections.emptyList())) {
            List<AiTravelPlanDTO.AttractionPlanItemDTO> attractions = Optional.ofNullable(schedule.getAttractions())
                    .orElse(Collections.emptyList());
            for (int index = 0; index < attractions.size(); index++) {
                AiTravelPlanDTO.AttractionPlanItemDTO item = attractions.get(index);
                if (StringUtils.isBlank(item.getName())) {
                    continue;
                }
                Attraction attraction = upsertAttraction(city, item, true);
                ItineraryDetail detail = new ItineraryDetail();
                detail.setItineraryId(itineraryId);
                detail.setAttractionId(attraction.getId());
                detail.setDayNumber(schedule.getDayNumber());
                detail.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : index + 1);
                itineraryDetailMapper.insert(detail);
            }
        }
    }

    private void rewriteDayPlans(Long itineraryId, AiTravelPlanDTO plan, boolean shouldRecalculateRoutes) {
        // 每日计划表保存的是“每天的路线级信息”，与景点明细表分开管理。
        itineraryDayPlanMapper.delete(new LambdaQueryWrapper<ItineraryDayPlan>()
                .eq(ItineraryDayPlan::getItineraryId, itineraryId));

        for (AiTravelPlanDTO.DailyScheduleDTO schedule : Optional.ofNullable(plan.getDailySchedules()).orElse(Collections.emptyList())) {
            if (schedule.getDayNumber() == null) {
                continue;
            }
            if (shouldRecalculateRoutes
                    && Optional.ofNullable(schedule.getAttractions()).orElse(Collections.emptyList()).size() > 1) {
                // 只有当前端明确说明草稿被改动过时，才重新调高德和 AI 生成路线摘要。
                DayRoutePlanResponse routePlan = buildDayRoutePlan(plan, schedule.getDayNumber(), "driving", true);
                schedule.setRouteSummary(routePlan.getSummary());
                if (!routePlan.getRoutes().isEmpty()) {
                    schedule.setRouteDistance(routePlan.getRoutes().get(0).getDistance());
                    schedule.setRouteDuration(routePlan.getRoutes().get(0).getDuration());
                }
            }

            ItineraryDayPlan dayPlan = new ItineraryDayPlan();
            dayPlan.setItineraryId(itineraryId);
            dayPlan.setDayNumber(schedule.getDayNumber());
            dayPlan.setRouteSummary(schedule.getRouteSummary());
            dayPlan.setRouteDistance(schedule.getRouteDistance());
            dayPlan.setRouteDuration(schedule.getRouteDuration());
            dayPlan.setCreateTime(LocalDateTime.now());
            dayPlan.setUpdateTime(LocalDateTime.now());
            itineraryDayPlanMapper.insert(dayPlan);
        }
    }

    private void appendChatMessages(Long itineraryId, String userMessage, String assistantMessage) {
        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.isNotBlank(userMessage)) {
            ChatMessage userChat = new ChatMessage();
            userChat.setItineraryId(itineraryId);
            userChat.setSender("USER");
            userChat.setContent(userMessage.trim());
            userChat.setCreateTime(now);
            chatMessageMapper.insert(userChat);
        }
        ChatMessage botChat = new ChatMessage();
        botChat.setItineraryId(itineraryId);
        botChat.setSender("BOT");
        botChat.setContent(assistantMessage);
        botChat.setCreateTime(now);
        chatMessageMapper.insert(botChat);
    }

    private AgentSessionResponse buildAgentSessionResponse(Long itineraryId, AiTravelPlanDTO plan) {
        AgentSessionResponse response = new AgentSessionResponse();
        response.setItineraryId(itineraryId);
        applyStoredDayPlanMetadata(itineraryId, plan);
        response.setCurrentDraft(plan);
        List<ChatMessageDTO> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getItineraryId, itineraryId)
                        .orderByAsc(ChatMessage::getCreateTime)
                        .orderByAsc(ChatMessage::getId))
                .stream()
                .map(this::toChatMessageDTO)
                .toList();
        response.setMessages(messages);
        return response;
    }

    private ChatMessageDTO toChatMessageDTO(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setId(message.getId());
        dto.setSender(message.getSender());
        dto.setContent(message.getContent());
        dto.setCreateTime(message.getCreateTime());
        return dto;
    }

    private String buildAssistantSummary(AiTravelPlanDTO plan) {
        // 这条文案会写进聊天记录里，用于提示前端“草稿已更新到什么状态”。
        int count = Optional.ofNullable(plan.getDailySchedules()).orElse(Collections.emptyList())
                .stream()
                .mapToInt(day -> Optional.ofNullable(day.getAttractions()).orElse(Collections.emptyList()).size())
                .sum();
        return "已更新" + StringUtils.defaultIfBlank(plan.getDestination(), "目标城市")
                + "行程，共 " + Optional.ofNullable(plan.getTotalDays()).orElse(0)
                + " 天，当前包含 " + count + " 个景点节点。";
    }

    private DayRoutePlanResponse buildDayRoutePlan(AiTravelPlanDTO draft, Integer dayNumber, String mode, boolean includeSummary) {
        // 单日路线规划核心方法：
        // 1. 找到某一天
        // 2. 提取带坐标的景点
        // 3. 调高德路线 API
        // 4. 解析路线和步骤
        // 5. 生成路线摘要
        AiTravelPlanDTO.DailyScheduleDTO daySchedule = Optional.ofNullable(draft.getDailySchedules())
                .orElse(Collections.emptyList())
                .stream()
                .filter(day -> Objects.equals(day.getDayNumber(), dayNumber))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到对应天数的行程"));

        List<AiTravelPlanDTO.AttractionPlanItemDTO> attractions = Optional.ofNullable(daySchedule.getAttractions())
                .orElse(Collections.emptyList())
                .stream()
                .filter(item -> item.getLongitude() != null && item.getLatitude() != null)
                .sorted(Comparator.comparing(AiTravelPlanDTO.AttractionPlanItemDTO::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();

        DayRoutePlanResponse response = new DayRoutePlanResponse();
        response.setDayNumber(dayNumber);
        response.setMode(mode);
        response.setDestination(draft.getDestination());
        response.setPoints(buildRoutePoints(attractions));

        if (attractions.size() <= 1) {
            // 一个点没必要规划完整路线，直接回退为点位展示。
            response.setFallback(true);
            response.setMessage("当前天数景点少于 2 个，无需规划完整路线");
            if (includeSummary) {
                response.setSummary(buildFallbackRouteSummary(dayNumber, attractions, null));
            }
            return response;
        }

        try {
            JsonNode routeData = fetchDrivingRoute(attractions);
            List<DayRoutePlanResponse.RoutePathDTO> routes = parseRoutePaths(routeData);
            response.setPaths(routes);
            response.setRoutes(routes);
            response.setFallback(routes.isEmpty());
            if (response.isFallback()) {
                response.setMessage("高德未返回有效路线，已回退为基础点位展示");
                if (includeSummary) {
                    response.setSummary(buildFallbackRouteSummary(dayNumber, attractions, null));
                }
            } else if (includeSummary) {
                response.setSummary(summarizeRoute(draft, dayNumber, attractions, routes.get(0), routeData));
            }
        } catch (Exception exception) {
            // 只要高德路线获取失败，前端仍然可以用顺序连线展示，不让页面完全不可用。
            response.setFallback(true);
            response.setMessage("高德实际路线获取失败，前端可回退为顺序连线展示");
            if (includeSummary) {
                response.setSummary(buildFallbackRouteSummary(dayNumber, attractions, null));
            }
        }
        return response;
    }

    private void applyStoredDayPlanMetadata(Long itineraryId, AiTravelPlanDTO plan) {
        Map<Integer, ItineraryDayPlan> dayPlanMap = itineraryDayPlanMapper.selectList(new LambdaQueryWrapper<ItineraryDayPlan>()
                        .eq(ItineraryDayPlan::getItineraryId, itineraryId))
                .stream()
                .collect(Collectors.toMap(ItineraryDayPlan::getDayNumber, item -> item, (left, right) -> left));

        for (AiTravelPlanDTO.DailyScheduleDTO schedule : Optional.ofNullable(plan.getDailySchedules()).orElse(Collections.emptyList())) {
            ItineraryDayPlan stored = dayPlanMap.get(schedule.getDayNumber());
            if (stored == null) {
                continue;
            }
            schedule.setRouteSummary(stored.getRouteSummary());
            schedule.setRouteDistance(stored.getRouteDistance());
            schedule.setRouteDuration(stored.getRouteDuration());
        }
    }

    private Map<Long, String> buildItineraryCoverImageMap(List<Long> itineraryIds) {
        if (itineraryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ItineraryDetail> details = itineraryDetailMapper.selectList(new LambdaQueryWrapper<ItineraryDetail>()
                .in(ItineraryDetail::getItineraryId, itineraryIds)
                .orderByAsc(ItineraryDetail::getDayNumber)
                .orderByAsc(ItineraryDetail::getSortOrder));
        Map<Long, Long> firstAttractionMap = new LinkedHashMap<>();
        for (ItineraryDetail detail : details) {
            firstAttractionMap.putIfAbsent(detail.getItineraryId(), detail.getAttractionId());
        }
        if (firstAttractionMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Attraction> attractionMap = attractionMapper.selectBatchIds(firstAttractionMap.values()).stream()
                .collect(Collectors.toMap(Attraction::getId, item -> item));
        Map<Long, String> coverMap = new LinkedHashMap<>();
        firstAttractionMap.forEach((itineraryId, attractionId) -> {
            Attraction attraction = attractionMap.get(attractionId);
            if (attraction != null) {
                coverMap.put(itineraryId, attraction.getImageUrl());
            }
        });
        return coverMap;
    }

    private AiTravelPlanDTO fetchAiPlan(AiGenerateRequest request) {
        // 这里封装了对大模型的调用。
        // 如果 AI 未启用、无密钥、或请求失败，会自动切到本地兜底方案。
        if (aiProperties.isEnabled() && StringUtils.isNotBlank(aiProperties.getApiKey())) {
            try {
                String url = aiProperties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("model", aiProperties.getModel());
                payload.put("temperature", 0.6);
                payload.put("stream", false);
                payload.put("messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", buildUserPrompt(request))));

                JsonNode response = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + aiProperties.getApiKey())
                        .body(payload)
                        .retrieve()
                        .body(JsonNode.class);

                String content = Optional.ofNullable(response)
                        .map(node -> node.path("choices"))
                        .filter(JsonNode::isArray)
                        .filter(array -> !array.isEmpty())
                        .map(array -> array.get(0))
                        .map(choice -> choice.path("message"))
                        .map(message -> message.path("content"))
                        .map(JsonNode::asText)
                        .orElseThrow(() -> new BusinessException("AI 未返回有效内容"));

                return objectMapper.readValue(extractJson(content), AiTravelPlanDTO.class);
            } catch (Exception ignored) {
                return buildFallbackPlan(request);
            }
        }
        return buildFallbackPlan(request);
    }

    private String buildUserPrompt(AiGenerateRequest request) throws JsonProcessingException {
        StringBuilder prompt = new StringBuilder();
        if (StringUtils.isNotBlank(request.getMessage())) {
            prompt.append("用户最新需求：").append(request.getMessage().trim()).append("\n");
        }
        if (StringUtils.isNotBlank(request.getDestination())) {
            prompt.append("已知目的地：").append(request.getDestination().trim()).append("\n");
        }
        if (request.getStartDate() != null) {
            prompt.append("已知开始日期：").append(request.getStartDate()).append("\n");
        }
        if (request.getEndDate() != null) {
            prompt.append("已知结束日期：").append(request.getEndDate()).append("\n");
        }
        if (StringUtils.isNotBlank(request.getPreference())) {
            prompt.append("偏好：").append(request.getPreference().trim()).append("\n");
        }
        if (request.getMustVisit() != null && !request.getMustVisit().isEmpty()) {
            prompt.append("必去景点：").append(String.join("、", request.getMustVisit())).append("\n");
        }
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            prompt.append("最近对话上下文：").append(request.getConversationHistory()).append("\n");
        }
        if (request.getCurrentDraft() != null) {
            prompt.append("当前草稿JSON：")
                    .append(objectMapper.writeValueAsString(request.getCurrentDraft()))
                    .append("\n");
        }
        prompt.append("请输出最终完整JSON。");
        return prompt.toString();
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        throw new BusinessException("AI 返回内容不是合法 JSON");
    }

    private AiTravelPlanDTO buildFallbackPlan(AiGenerateRequest request) {
        // 本地兜底规划：
        // 1. 优先使用 mustVisit
        // 2. 再使用本地热门景点
        // 3. 不足时拼接一组通用默认景点名
        if (request.getCurrentDraft() != null) {
            return buildFallbackDraftFromCurrent(request);
        }

        PlanningContext context = resolvePlanningContext(request, null);
        LinkedHashSet<String> attractionNames = new LinkedHashSet<>();
        Optional.ofNullable(request.getMustVisit()).orElse(Collections.emptyList()).stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .forEach(attractionNames::add);

        List<Attraction> localAttractions = attractionMapper.selectList(new LambdaQueryWrapper<Attraction>()
                .eq(Attraction::getCity, context.destination())
                .orderByDesc(Attraction::getReferenceCount)
                .orderByDesc(Attraction::getId)
                .last("limit " + Math.max(context.totalDays() * 3, 6)));
        Map<String, Attraction> localAttractionMap = localAttractions.stream()
                .collect(Collectors.toMap(Attraction::getName, item -> item, (left, right) -> left, LinkedHashMap::new));
        localAttractions.stream().map(Attraction::getName).forEach(attractionNames::add);

        List<String> defaults = Arrays.asList(
                context.destination() + "博物馆",
                context.destination() + "历史街区",
                context.destination() + "城市公园",
                context.destination() + "地标夜景",
                context.destination() + "美食街",
                context.destination() + "文创市集",
                context.destination() + "滨水步道",
                context.destination() + "艺术中心");
        defaults.forEach(attractionNames::add);

        List<String> pool = new ArrayList<>(attractionNames);
        AiTravelPlanDTO plan = new AiTravelPlanDTO();
        plan.setTitle(context.destination() + context.totalDays() + "日实时漫游计划");
        plan.setDestination(context.destination());
        plan.setStartDate(context.startDate() == null ? null : context.startDate().toString());
        plan.setEndDate(context.endDate() == null ? null : context.endDate().toString());
        plan.setTotalDays(context.totalDays());

        BigDecimal[] hourPool = {
                new BigDecimal("1.5"),
                new BigDecimal("2.0"),
                new BigDecimal("2.5"),
                new BigDecimal("3.0")
        };

        int index = 0;
        for (int day = 1; day <= context.totalDays(); day++) {
            AiTravelPlanDTO.DailyScheduleDTO schedule = new AiTravelPlanDTO.DailyScheduleDTO();
            schedule.setDayNumber(day);
            int remainingDays = context.totalDays() - day + 1;
            int remainingItems = pool.size() - index;
            int count = Math.min(3, Math.max(1, Math.min(DEFAULT_ATTRACTIONS_PER_DAY,
                    (int) Math.ceil((double) remainingItems / remainingDays))));
            for (int order = 1; order <= count && index < pool.size(); order++) {
                String name = pool.get(index++);
                AiTravelPlanDTO.AttractionPlanItemDTO item = new AiTravelPlanDTO.AttractionPlanItemDTO();
                item.setName(name);
                item.setSortOrder(order);
                item.setSuggestedHours(hourPool[(day + order) % hourPool.length]);
                Attraction matched = localAttractionMap.get(name);
                item.setDescription(matched != null && StringUtils.isNotBlank(matched.getDescription())
                        ? matched.getDescription()
                        : buildDefaultDescription(context.destination(), name));
                item.setImageUrl(matched == null ? null : matched.getImageUrl());
                schedule.getAttractions().add(item);
            }
            plan.getDailySchedules().add(schedule);
        }
        return plan;
    }

    private AiTravelPlanDTO buildFallbackDraftFromCurrent(AiGenerateRequest request) {
        AiTravelPlanDTO plan = objectMapper.convertValue(request.getCurrentDraft(), AiTravelPlanDTO.class);
        String message = StringUtils.defaultString(request.getMessage()).trim();
        if (message.isEmpty()) {
            return plan;
        }

        Matcher deleteMatcher = DELETE_PATTERN.matcher(message);
        if (deleteMatcher.find()) {
            String attractionName = deleteMatcher.group(2).replace("今天的", "").replace("明天的", "").trim();
            Integer targetDay = extractDayNumber(message);
            for (AiTravelPlanDTO.DailyScheduleDTO schedule : Optional.ofNullable(plan.getDailySchedules()).orElse(List.of())) {
                if (targetDay != null && !Objects.equals(schedule.getDayNumber(), targetDay)) {
                    continue;
                }
                schedule.setAttractions(schedule.getAttractions().stream()
                        .filter(item -> !StringUtils.contains(item.getName(), attractionName))
                        .collect(Collectors.toCollection(ArrayList::new)));
            }
        }
        return plan;
    }

    private PlanningContext resolvePlanningContext(AiGenerateRequest request, AiTravelPlanDTO plan) {
        String destination = firstNonBlank(
                request.getDestination(),
                plan == null ? null : plan.getDestination(),
                request.getCurrentDraft() == null ? null : request.getCurrentDraft().getDestination(),
                extractDestinationFromText(request.getMessage()));

        LocalDate startDate = firstNonNull(
                request.getStartDate(),
                parseDate(plan == null ? null : plan.getStartDate()),
                parseDate(request.getCurrentDraft() == null ? null : request.getCurrentDraft().getStartDate()));

        LocalDate endDate = firstNonNull(
                request.getEndDate(),
                parseDate(plan == null ? null : plan.getEndDate()),
                parseDate(request.getCurrentDraft() == null ? null : request.getCurrentDraft().getEndDate()));

        Integer totalDays = firstNonNull(
                plan == null ? null : plan.getTotalDays(),
                request.getCurrentDraft() == null ? null : request.getCurrentDraft().getTotalDays(),
                extractTotalDays(request.getMessage()));

        if (totalDays == null || totalDays < 1) {
            if (startDate != null && endDate != null) {
                totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
            } else {
                totalDays = Optional.ofNullable(plan)
                        .map(AiTravelPlanDTO::getDailySchedules)
                        .map(List::size)
                        .filter(size -> size > 0)
                        .orElse(3);
            }
        }

        if (StringUtils.isBlank(destination)) {
            throw new BusinessException("未能识别目的地，请在对话中明确城市");
        }
        return new PlanningContext(destination.trim(), startDate, endDate, totalDays);
    }

    private void normalizePlan(PlanningContext context, AiGenerateRequest request, AiTravelPlanDTO plan) {
        // 对 AI 输出做统一清洗，保证后续流程拿到的是结构稳定的数据。
        if (StringUtils.isBlank(plan.getTitle())) {
            plan.setTitle(context.destination() + context.totalDays() + "日专属行程");
        }
        plan.setDestination(context.destination());
        plan.setStartDate(context.startDate() == null ? null : context.startDate().toString());
        plan.setEndDate(context.endDate() == null ? null : context.endDate().toString());
        plan.setTotalDays(context.totalDays());

        Map<Integer, AiTravelPlanDTO.DailyScheduleDTO> scheduleMap = new LinkedHashMap<>();
        Optional.ofNullable(plan.getDailySchedules()).orElse(Collections.emptyList()).forEach(schedule -> {
            Integer dayNumber = schedule.getDayNumber();
            if (dayNumber != null && dayNumber >= 1 && dayNumber <= context.totalDays() && !scheduleMap.containsKey(dayNumber)) {
                scheduleMap.put(dayNumber, schedule);
            }
        });

        List<AiTravelPlanDTO.DailyScheduleDTO> normalizedSchedules = new ArrayList<>();
        for (int day = 1; day <= context.totalDays(); day++) {
            AiTravelPlanDTO.DailyScheduleDTO schedule = scheduleMap.getOrDefault(day, new AiTravelPlanDTO.DailyScheduleDTO());
            schedule.setDayNumber(day);
            List<AiTravelPlanDTO.AttractionPlanItemDTO> normalizedAttractions = new ArrayList<>();
            List<AiTravelPlanDTO.AttractionPlanItemDTO> sourceAttractions = Optional.ofNullable(schedule.getAttractions())
                    .orElse(Collections.emptyList());
            int order = 1;
            for (AiTravelPlanDTO.AttractionPlanItemDTO item : sourceAttractions) {
                if (item == null || StringUtils.isBlank(item.getName())) {
                    continue;
                }
                item.setName(item.getName().trim());
                item.setSortOrder(order++);
                if (item.getSuggestedHours() == null) {
                    item.setSuggestedHours(new BigDecimal("2.0"));
                } else {
                    item.setSuggestedHours(item.getSuggestedHours().setScale(1, RoundingMode.HALF_UP));
                }
                if (StringUtils.isBlank(item.getDescription())) {
                    item.setDescription(buildDefaultDescription(context.destination(), item.getName()));
                }
                normalizedAttractions.add(item);
            }
            schedule.setAttractions(normalizedAttractions);
            normalizedSchedules.add(schedule);
        }
        plan.setDailySchedules(normalizedSchedules);
    }

    private void ensureReasonableDailyAttractions(PlanningContext context, AiGenerateRequest request, AiTravelPlanDTO plan) {
        // 目标：柔性补足景点，而不是硬性要求每天至少 3 个。
        // 基本原则：
        // 1. 原本已有 2 个及以上景点时，不再机械补点
        // 2. 原本只有 1 个景点且时长较长时，允许保持 1 个
        // 3. 原本只有 0 个或 1 个短时景点时，优先补到 2 个
        List<AiTravelPlanDTO.DailyScheduleDTO> schedules = Optional.ofNullable(plan.getDailySchedules())
                .orElse(Collections.emptyList());
        if (schedules.isEmpty()) {
            return;
        }

        Map<String, Attraction> localAttractionMap = attractionMapper.selectList(new LambdaQueryWrapper<Attraction>()
                        .eq(Attraction::getCity, context.destination())
                        .orderByDesc(Attraction::getReferenceCount)
                        .orderByDesc(Attraction::getId)
                        .last("limit " + Math.max(schedules.size() * 8, 18)))
                .stream()
                .filter(item -> StringUtils.isNotBlank(item.getName()))
                .collect(Collectors.toMap(Attraction::getName, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<String> fallbackCandidates = buildSupplementFallbackNames(context.destination(), request);
        LinkedHashSet<String> globalUsedNames = schedules.stream()
                .flatMap(schedule -> Optional.ofNullable(schedule.getAttractions()).orElse(Collections.emptyList()).stream())
                .map(AiTravelPlanDTO.AttractionPlanItemDTO::getName)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (AiTravelPlanDTO.DailyScheduleDTO schedule : schedules) {
            List<AiTravelPlanDTO.AttractionPlanItemDTO> attractions = Optional.ofNullable(schedule.getAttractions())
                    .orElseGet(ArrayList::new);
            schedule.setAttractions(attractions);

            LinkedHashSet<String> dayUsedNames = attractions.stream()
                    .map(AiTravelPlanDTO.AttractionPlanItemDTO::getName)
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            int targetCount = resolveTargetAttractionCount(attractions);
            while (attractions.size() < targetCount) {
                String candidateName = pickSupplementCandidate(dayUsedNames, globalUsedNames, localAttractionMap, fallbackCandidates);
                if (StringUtils.isBlank(candidateName)) {
                    break;
                }

                Attraction matched = localAttractionMap.get(candidateName);
                AiTravelPlanDTO.AttractionPlanItemDTO supplement = new AiTravelPlanDTO.AttractionPlanItemDTO();
                supplement.setName(candidateName);
                supplement.setSortOrder(attractions.size() + 1);
                supplement.setSuggestedHours(Optional.ofNullable(matched)
                        .map(Attraction::getPlayTime)
                        .orElse(new BigDecimal("2.0")));
                supplement.setDescription(matched != null && StringUtils.isNotBlank(matched.getDescription())
                        ? matched.getDescription()
                        : buildDefaultDescription(context.destination(), candidateName));
                supplement.setImageUrl(matched == null ? null : matched.getImageUrl());
                supplement.setLongitude(matched == null ? null : matched.getLongitude());
                supplement.setLatitude(matched == null ? null : matched.getLatitude());
                attractions.add(supplement);
                dayUsedNames.add(candidateName);
                globalUsedNames.add(candidateName);
            }
        }
    }

    private int resolveTargetAttractionCount(List<AiTravelPlanDTO.AttractionPlanItemDTO> attractions) {
        if (attractions.isEmpty()) {
            return DEFAULT_ATTRACTIONS_PER_DAY;
        }
        if (attractions.size() >= 2) {
            return attractions.size();
        }

        BigDecimal totalSuggestedHours = attractions.stream()
                .map(AiTravelPlanDTO.AttractionPlanItemDTO::getSuggestedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 单个大景点已经足够占满大半天时，允许当天只保留 1 个。
        if (totalSuggestedHours.compareTo(new BigDecimal("4.5")) >= 0) {
            return 1;
        }
        return DEFAULT_ATTRACTIONS_PER_DAY;
    }

    private List<String> buildSupplementFallbackNames(String destination, AiGenerateRequest request) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Optional.ofNullable(request.getMustVisit()).orElse(Collections.emptyList()).stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .forEach(names::add);
        names.addAll(Arrays.asList(
                destination + "博物馆",
                destination + "历史街区",
                destination + "城市公园",
                destination + "地标夜景",
                destination + "美食街",
                destination + "文创市集",
                destination + "滨水步道",
                destination + "艺术中心",
                destination + "老城巷子",
                destination + "非遗体验馆",
                destination + "山水观景台",
                destination + "夜市街区"));
        return new ArrayList<>(names);
    }

    private String pickSupplementCandidate(LinkedHashSet<String> dayUsedNames,
                                           LinkedHashSet<String> globalUsedNames,
                                           Map<String, Attraction> localAttractionMap,
                                           List<String> fallbackCandidates) {
        Optional<String> localUnused = localAttractionMap.keySet().stream()
                .filter(name -> !dayUsedNames.contains(name))
                .filter(name -> !globalUsedNames.contains(name))
                .findFirst();
        if (localUnused.isPresent()) {
            return localUnused.get();
        }

        Optional<String> fallbackUnused = fallbackCandidates.stream()
                .filter(name -> !dayUsedNames.contains(name))
                .filter(name -> !globalUsedNames.contains(name))
                .findFirst();
        if (fallbackUnused.isPresent()) {
            return fallbackUnused.get();
        }

        Optional<String> localReusable = localAttractionMap.keySet().stream()
                .filter(name -> !dayUsedNames.contains(name))
                .findFirst();
        if (localReusable.isPresent()) {
            return localReusable.get();
        }

        return fallbackCandidates.stream()
                .filter(name -> !dayUsedNames.contains(name))
                .findFirst()
                .orElse(null);
    }

    private void hydrateAttractions(String city, AiTravelPlanDTO plan) {
        // 把草稿里的每个景点尽量映射到本地 attraction 主表：
        // - 如果已有景点，则补齐经纬度、图片、标准名
        // - 如果没有，则自动创建
        plan.getDailySchedules().forEach(schedule -> schedule.getAttractions().forEach(item -> {
            Attraction attraction = upsertAttraction(city, item, false);
                item.setName(attraction.getName());
                item.setAttractionId(attraction.getId());
                item.setLongitude(attraction.getLongitude());
                item.setLatitude(attraction.getLatitude());
                item.setImageUrl(attraction.getImageUrl());
                if (StringUtils.isBlank(item.getDescription())) {
                    item.setDescription(attraction.getDescription());
                }
            if (item.getSuggestedHours() == null) {
                item.setSuggestedHours(attraction.getPlayTime());
            }
        }));
    }

    private Attraction upsertAttraction(String city, AiTravelPlanDTO.AttractionPlanItemDTO item, boolean increaseReference) {
        // 这个方法是“景点知识沉淀”的核心。
        // 它负责判断景点是否已经存在、是否需要校正名称、是否需要补 POI 信息，以及是否需要新增入库。
        item.setName(StringUtils.trimToEmpty(item.getName()));
        Attraction attraction = null;
        if (item.getAttractionId() != null) {
            attraction = attractionMapper.selectById(item.getAttractionId());
        }
        if (attraction == null) {
            attraction = attractionMapper.selectOne(new LambdaQueryWrapper<Attraction>()
                    .eq(Attraction::getName, item.getName())
                    .eq(Attraction::getCity, city)
                    .last("limit 1"));
        }

        PoiMetadata poiMetadata = resolvePoiMetadata(city, item, attraction);
        String canonicalName = firstNonBlank(
                poiMetadata.standardName(),
                attraction == null ? null : attraction.getName(),
                item.getName());
        item.setName(canonicalName);

        Attraction canonicalAttraction = StringUtils.isBlank(canonicalName) ? null : attractionMapper.selectOne(new LambdaQueryWrapper<Attraction>()
                .eq(Attraction::getName, canonicalName)
                .eq(Attraction::getCity, city)
                .last("limit 1"));
        if (canonicalAttraction != null) {
            attraction = canonicalAttraction;
        }

        if (attraction != null) {
            // 已存在景点：补充缺失字段并按需增加引用次数。
            attraction.setName(canonicalName);
            attraction.setDescription(StringUtils.defaultIfBlank(attraction.getDescription(), item.getDescription()));
            attraction.setImageUrl(StringUtils.defaultIfBlank(attraction.getImageUrl(), poiMetadata.imageUrl()));
            attraction.setPlayTime(Objects.requireNonNullElse(attraction.getPlayTime(), item.getSuggestedHours()));
            if (poiMetadata.longitude() != null) {
                attraction.setLongitude(poiMetadata.longitude());
            }
            if (poiMetadata.latitude() != null) {
                attraction.setLatitude(poiMetadata.latitude());
            }
            if (increaseReference) {
                attraction.setReferenceCount(Optional.ofNullable(attraction.getReferenceCount()).orElse(0) + 1);
            }
            attraction.setUpdateTime(LocalDateTime.now());
            attractionMapper.updateById(attraction);
            return attraction;
        }

        Attraction newAttraction = new Attraction();
        // 不存在景点：直接创建为本地知识库的一部分。
        newAttraction.setCity(city);
        newAttraction.setName(canonicalName);
        newAttraction.setDescription(StringUtils.defaultIfBlank(item.getDescription(), buildDefaultDescription(city, canonicalName)));
        newAttraction.setImageUrl(poiMetadata.imageUrl());
        newAttraction.setPlayTime(Objects.requireNonNullElse(item.getSuggestedHours(), new BigDecimal("2.0")));
        newAttraction.setLongitude(poiMetadata.longitude());
        newAttraction.setLatitude(poiMetadata.latitude());
        newAttraction.setReferenceCount(increaseReference ? 1 : 0);
        newAttraction.setCreateTime(LocalDateTime.now());
        newAttraction.setUpdateTime(LocalDateTime.now());
        attractionMapper.insert(newAttraction);
        return newAttraction;
    }

    private PoiMetadata resolvePoiMetadata(String city, AiTravelPlanDTO.AttractionPlanItemDTO item, Attraction attraction) {
        // 优先复用已有经纬度 / 图片；
        // 如果缺失，再调用高德 POI 搜索补齐。
        BigDecimal longitude = firstNonNull(item.getLongitude(), attraction == null ? null : attraction.getLongitude());
        BigDecimal latitude = firstNonNull(item.getLatitude(), attraction == null ? null : attraction.getLatitude());
        String standardName = firstNonBlank(item.getName(), attraction == null ? null : attraction.getName());
        String imageUrl = firstNonBlank(
                item.getImageUrl(),
                attraction == null ? null : attraction.getImageUrl());
        if (longitude != null && latitude != null && StringUtils.isNotBlank(imageUrl)) {
            return new PoiMetadata(standardName, scaleCoordinate(longitude), scaleCoordinate(latitude), imageUrl);
        }
        PoiMetadata poiMetadata = searchPoiMetadata(city, item.getName());
        return new PoiMetadata(
                firstNonBlank(poiMetadata.standardName(), standardName),
                firstNonNull(poiMetadata.longitude(), longitude == null ? null : scaleCoordinate(longitude)),
                firstNonNull(poiMetadata.latitude(), latitude == null ? null : scaleCoordinate(latitude)),
                firstNonBlank(poiMetadata.imageUrl(), imageUrl)
        );
    }

    private PoiMetadata searchPoiMetadata(String city, String attractionName) {
        // 调高德 POI 搜索接口，拿标准名称、坐标和图片。
        if (StringUtils.isBlank(amapProperties.getWebServiceKey()) || StringUtils.isBlank(attractionName)) {
            return new PoiMetadata(null, null, null, null);
        }
        try {
            String keywords = StringUtils.defaultString(city) + attractionName;
            JsonNode response = restClient.get()
                    .uri("https://restapi.amap.com/v5/place/text?keywords={keywords}&region={region}&key={key}&show_fields={fields}",
                            keywords, StringUtils.defaultString(city), amapProperties.getWebServiceKey(), "photos")
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode poi = Optional.ofNullable(response)
                    .map(node -> node.path("pois"))
                    .filter(JsonNode::isArray)
                    .filter(array -> !array.isEmpty())
                    .map(array -> array.get(0))
                    .orElse(null);
            if (poi == null) {
                return new PoiMetadata(null, null, null, null);
            }
            String standardName = StringUtils.trimToNull(poi.path("name").asText(""));
            String location = poi.path("location").asText("");
            String[] parts = location.split(",");
            if (parts.length != 2) {
                return new PoiMetadata(standardName, null, null, extractPoiImageUrl(poi));
            }
            return new PoiMetadata(
                    standardName,
                    scaleCoordinate(new BigDecimal(parts[0])),
                    scaleCoordinate(new BigDecimal(parts[1])),
                    extractPoiImageUrl(poi)
            );
        } catch (Exception ignored) {
            return new PoiMetadata(null, null, null, null);
        }
    }

    private String extractPoiImageUrl(JsonNode poi) {
        JsonNode photos = poi.path("photos");
        if (photos.isArray() && !photos.isEmpty()) {
            JsonNode first = photos.get(0);
            String url = first.path("url").asText("");
            return StringUtils.isBlank(url) ? null : url;
        }
        return null;
    }

    private JsonNode fetchDrivingRoute(List<AiTravelPlanDTO.AttractionPlanItemDTO> attractions) {
        // 调高德驾车路线 API。
        // 首点作为 origin，末点作为 destination，中间点作为 waypoints。
        if (StringUtils.isBlank(amapProperties.getWebServiceKey())) {
            throw new BusinessException("未配置高德 Web 服务 Key");
        }
        String origin = joinCoordinate(attractions.get(0));
        String destination = joinCoordinate(attractions.get(attractions.size() - 1));
        String waypoints = attractions.size() > 2
                ? attractions.subList(1, attractions.size() - 1).stream()
                .map(this::joinCoordinate)
                .collect(Collectors.joining("|"))
                : null;

        StringBuilder uri = new StringBuilder("https://restapi.amap.com/v5/direction/driving?key={key}&origin={origin}&destination={destination}&show_fields={showFields}");
        String showFields = "cost,navi,polyline";
        if (StringUtils.isNotBlank(waypoints)) {
            uri.append("&waypoints={waypoints}");
        }

        if (StringUtils.isNotBlank(waypoints)) {
            return restClient.get()
                    .uri(uri.toString(), amapProperties.getWebServiceKey(), origin, destination, showFields, waypoints)
                    .retrieve()
                    .body(JsonNode.class);
        }
        return restClient.get()
                .uri(uri.toString(), amapProperties.getWebServiceKey(), origin, destination, showFields)
                .retrieve()
                .body(JsonNode.class);
    }

    private List<DayRoutePlanResponse.RoutePointDTO> buildRoutePoints(List<AiTravelPlanDTO.AttractionPlanItemDTO> attractions) {
        return attractions.stream().map(item -> {
            DayRoutePlanResponse.RoutePointDTO point = new DayRoutePlanResponse.RoutePointDTO();
            point.setName(item.getName());
            point.setSortOrder(item.getSortOrder());
            point.setLongitude(scaleCoordinate(item.getLongitude()));
            point.setLatitude(scaleCoordinate(item.getLatitude()));
            return point;
        }).toList();
    }

    private List<DayRoutePlanResponse.RoutePathDTO> parseRoutePaths(JsonNode routeData) {
        JsonNode pathsNode = Optional.ofNullable(routeData)
                .map(node -> node.path("route"))
                .map(route -> route.path("paths"))
                .orElse(null);
        if (pathsNode == null || !pathsNode.isArray()) {
            return Collections.emptyList();
        }

        List<DayRoutePlanResponse.RoutePathDTO> paths = new ArrayList<>();
        for (JsonNode pathNode : pathsNode) {
            DayRoutePlanResponse.RoutePathDTO path = new DayRoutePlanResponse.RoutePathDTO();
            path.setDistance(pathNode.path("distance").asText(""));
            path.setDuration(pathNode.path("cost").path("duration").asText(pathNode.path("duration").asText("")));
            path.setStrategy(pathNode.path("strategy").asText(""));
            path.setPolyline(parsePolyline(pathNode.path("polyline").asText("")));

            List<DayRoutePlanResponse.RouteStepDTO> steps = new ArrayList<>();
            for (JsonNode stepNode : pathNode.path("steps")) {
                DayRoutePlanResponse.RouteStepDTO step = new DayRoutePlanResponse.RouteStepDTO();
                step.setInstruction(stepNode.path("instruction").asText(""));
                step.setRoad(stepNode.path("road_name").asText(stepNode.path("road").asText("")));
                step.setOrientation(stepNode.path("orientation").asText(""));
                step.setDistance(stepNode.path("step_distance").asText(stepNode.path("distance").asText("")));
                step.setDuration(stepNode.path("cost").path("duration").asText(stepNode.path("duration").asText("")));
                step.setPolyline(parsePolyline(stepNode.path("polyline").asText("")));
                steps.add(step);
            }
            path.setSteps(steps);
            if (path.getPolyline().isEmpty()) {
                path.setPolyline(steps.stream()
                        .flatMap(step -> step.getPolyline().stream())
                        .collect(Collectors.toCollection(ArrayList::new)));
            }
            paths.add(path);
        }
        return paths;
    }

    private String summarizeRoute(AiTravelPlanDTO draft,
                                  Integer dayNumber,
                                  List<AiTravelPlanDTO.AttractionPlanItemDTO> attractions,
                                  DayRoutePlanResponse.RoutePathDTO route,
                                  JsonNode rawRouteData) {
        // 路线摘要优先由 AI 生成自然语言文案；
        // 如果失败，则回退到本地模板摘要。
        if (aiProperties.isEnabled() && StringUtils.isNotBlank(aiProperties.getApiKey())) {
            try {
                String url = aiProperties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("model", aiProperties.getModel());
                payload.put("temperature", 0.4);
                payload.put("stream", false);
                payload.put("messages", List.of(
                        Map.of("role", "system", "content", ROUTE_SUMMARY_PROMPT),
                        Map.of("role", "user", "content", buildRouteSummaryPrompt(draft, dayNumber, attractions, route, rawRouteData))));

                JsonNode response = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + aiProperties.getApiKey())
                        .body(payload)
                        .retrieve()
                        .body(JsonNode.class);

                return Optional.ofNullable(response)
                        .map(node -> node.path("choices"))
                        .filter(JsonNode::isArray)
                        .filter(array -> !array.isEmpty())
                        .map(array -> array.get(0))
                        .map(choice -> choice.path("message"))
                        .map(message -> message.path("content"))
                        .map(JsonNode::asText)
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .orElseGet(() -> buildFallbackRouteSummary(dayNumber, attractions, route));
            } catch (Exception ignored) {
                return buildFallbackRouteSummary(dayNumber, attractions, route);
            }
        }
        return buildFallbackRouteSummary(dayNumber, attractions, route);
    }

    private String buildRouteSummaryPrompt(AiTravelPlanDTO draft,
                                           Integer dayNumber,
                                           List<AiTravelPlanDTO.AttractionPlanItemDTO> attractions,
                                           DayRoutePlanResponse.RoutePathDTO route,
                                           JsonNode rawRouteData) throws JsonProcessingException {
        List<Map<String, Object>> attractionSummary = new ArrayList<>();
        for (AiTravelPlanDTO.AttractionPlanItemDTO item : attractions) {
            Map<String, Object> attractionItem = new LinkedHashMap<>();
            attractionItem.put("name", item.getName());
            attractionItem.put("sortOrder", Optional.ofNullable(item.getSortOrder()).orElse(0));
            attractionItem.put("suggestedHours", Optional.ofNullable(item.getSuggestedHours()).map(BigDecimal::toPlainString).orElse(""));
            attractionItem.put("description", StringUtils.defaultString(item.getDescription()));
            attractionSummary.add(attractionItem);
        }

        Map<String, Object> routeDigest = new LinkedHashMap<>();
        routeDigest.put("distance", route.getDistance());
        routeDigest.put("duration", route.getDuration());
        routeDigest.put("stepCount", route.getSteps().size());
        List<Map<String, Object>> stepDigest = new ArrayList<>();
        for (DayRoutePlanResponse.RouteStepDTO step : route.getSteps().stream().limit(8).toList()) {
            Map<String, Object> stepItem = new LinkedHashMap<>();
            stepItem.put("instruction", StringUtils.defaultString(step.getInstruction()));
            stepItem.put("road", StringUtils.defaultString(step.getRoad()));
            stepItem.put("distance", StringUtils.defaultString(step.getDistance()));
            stepItem.put("duration", StringUtils.defaultString(step.getDuration()));
            stepDigest.add(stepItem);
        }
        routeDigest.put("steps", stepDigest);

        return """
                城市：%s
                标题：%s
                天数：第 %d 天
                景点顺序：%s
                路线摘要：%s
                原始路线片段：%s
                """.formatted(
                StringUtils.defaultString(draft.getDestination()),
                StringUtils.defaultString(draft.getTitle()),
                dayNumber,
                objectMapper.writeValueAsString(attractionSummary),
                objectMapper.writeValueAsString(routeDigest),
                objectMapper.writeValueAsString(rawRouteData.path("route").path("paths").isArray()
                        && rawRouteData.path("route").path("paths").size() > 0
                        ? rawRouteData.path("route").path("paths").get(0)
                        : rawRouteData));
    }

    private String buildFallbackRouteSummary(Integer dayNumber,
                                             List<AiTravelPlanDTO.AttractionPlanItemDTO> attractions,
                                             DayRoutePlanResponse.RoutePathDTO route) {
        String joinedNames = attractions.stream()
                .map(AiTravelPlanDTO.AttractionPlanItemDTO::getName)
                .collect(Collectors.joining(" -> "));
        if (route == null) {
            return "第 " + dayNumber + " 天将按 " + joinedNames + " 的顺序游览，当前已完成点位规划，可继续查看地图或微调顺序。";
        }
        String distance = humanizeDistance(route.getDistance());
        String duration = humanizeDuration(route.getDuration());
        return "第 " + dayNumber + " 天会按 " + joinedNames + " 的顺序展开，整段路线约 " + distance
                + "，预计通行 " + duration + "，建议按照当前顺序游览以减少折返。";
    }

    private String humanizeDistance(String distance) {
        if (StringUtils.isBlank(distance) || !StringUtils.isNumeric(distance)) {
            return "若干公里";
        }
        BigDecimal meters = new BigDecimal(distance);
        if (meters.compareTo(new BigDecimal("1000")) < 0) {
            return meters.setScale(0, RoundingMode.HALF_UP) + " 米";
        }
        return meters.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP) + " 公里";
    }

    private String humanizeDuration(String duration) {
        if (StringUtils.isBlank(duration) || !StringUtils.isNumeric(duration)) {
            return "若干分钟";
        }
        long seconds = Long.parseLong(duration);
        long minutes = Math.max(1, seconds / 60);
        if (minutes < 60) {
            return minutes + " 分钟";
        }
        return (minutes / 60) + " 小时" + (minutes % 60 == 0 ? "" : (minutes % 60) + " 分钟");
    }

    private List<DayRoutePlanResponse.RouteLinePointDTO> parsePolyline(String polylineText) {
        if (StringUtils.isBlank(polylineText)) {
            return Collections.emptyList();
        }
        return Arrays.stream(polylineText.split(";"))
                .map(point -> point.split(","))
                .filter(parts -> parts.length == 2)
                .map(parts -> {
                    DayRoutePlanResponse.RouteLinePointDTO routePoint = new DayRoutePlanResponse.RouteLinePointDTO();
                    routePoint.setLongitude(scaleCoordinate(new BigDecimal(parts[0])));
                    routePoint.setLatitude(scaleCoordinate(new BigDecimal(parts[1])));
                    return routePoint;
                })
                .toList();
    }

    private String joinCoordinate(AiTravelPlanDTO.AttractionPlanItemDTO item) {
        return scaleCoordinate(item.getLongitude()) + "," + scaleCoordinate(item.getLatitude());
    }

    private void decrementAttractionReference(Long attractionId, Long delta) {
        if (attractionId == null) {
            return;
        }
        Attraction attraction = attractionMapper.selectById(attractionId);
        if (attraction == null) {
            return;
        }
        int current = Optional.ofNullable(attraction.getReferenceCount()).orElse(0);
        attraction.setReferenceCount(Math.max(current - delta.intValue(), 0));
        attraction.setUpdateTime(LocalDateTime.now());
        attractionMapper.updateById(attraction);
    }

    private String buildDefaultDescription(String city, String attractionName) {
        return attractionName + "是" + city + "值得慢慢感受的热门地点，兼顾风景与文化体验，周边通常能顺路安排本地小吃或特色餐馆，适合轻松打卡并体验城市日常氛围。";
    }

    private String extractDestinationFromText(String message) {
        if (StringUtils.isBlank(message)) {
            return null;
        }
        Matcher matcher = CITY_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Integer extractTotalDays(String message) {
        if (StringUtils.isBlank(message)) {
            return null;
        }
        Matcher matcher = DAY_COUNT_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return parseChineseNumber(matcher.group(1));
    }

    private Integer extractDayNumber(String message) {
        if (StringUtils.isBlank(message)) {
            return null;
        }
        Matcher matcher = DAY_NUMBER_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return parseChineseNumber(matcher.group(1));
    }

    private Integer parseChineseNumber(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (StringUtils.isNumeric(value)) {
            return Integer.parseInt(value);
        }
        return switch (value) {
            case "一" -> 1;
            case "二", "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            case "十" -> 10;
            default -> null;
        };
    }

    private LocalDate parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal scaleCoordinate(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private record PlanningContext(String destination, LocalDate startDate, LocalDate endDate, Integer totalDays) {
    }

    private record PoiMetadata(String standardName, BigDecimal longitude, BigDecimal latitude, String imageUrl) {
    }
}
