package com.example.travelassistant.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgentSessionResponse {

    /** 本次 AI 会话对应的行程主键。 */
    private Long itineraryId;

    /** 当前最新草稿。前端会直接据此渲染卡片和地图。 */
    private AiTravelPlanDTO currentDraft;

    /** 到当前为止的对话记录，用于前端聊天区回显。 */
    private List<ChatMessageDTO> messages = new ArrayList<>();
}
