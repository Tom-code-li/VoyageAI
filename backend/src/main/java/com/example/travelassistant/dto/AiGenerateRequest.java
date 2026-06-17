package com.example.travelassistant.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiGenerateRequest {

    /** 当前登录用户 ID，用于把 AI 会话草稿归属到具体用户。 */
    private Long userId;

    /** 已存在的行程 ID。继续追问修改同一份草稿时会传这个值。 */
    private Long itineraryId;

    /** 用户明确指定的目的地。 */
    private String destination;

    @JsonAlias("start_date")
    private LocalDate startDate;

    @JsonAlias("end_date")
    private LocalDate endDate;

    /** 用户偏好，例如“带老人”“多吃火锅”“少走路”。 */
    private String preference;

    /** 用户明确要求必须安排进去的景点。 */
    @JsonAlias("must_visit")
    private List<String> mustVisit;

    /** 当前这一轮对话里用户最新输入的自然语言需求。 */
    private String message;

    /** 当前草稿标题，便于 AI 在续写时延续原风格。 */
    private String draftTitle;

    /** 当前已经存在的草稿结构，用于增量修改而不是重生成。 */
    private AiTravelPlanDTO currentDraft;

    /** 历史对话文本，帮助大模型理解上下文。 */
    private List<String> conversationHistory = new ArrayList<>();
}
