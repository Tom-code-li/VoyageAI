package com.example.travelassistant.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SaveItineraryRequest {

    /** 已有行程 ID。为空表示新建，非空表示覆盖更新。 */
    private Long itineraryId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "城市不能为空")
    private String city;

    @JsonAlias("start_date")
    private LocalDate startDate;

    @JsonAlias("end_date")
    private LocalDate endDate;

    /**
     * 前端是否改动过草稿。
     * false：直接使用前端已有 routeSummary / distance / duration 快速保存
     * true：说明景点顺序或内容有变化，保存时需要重算路线
     */
    private Boolean modified;

    /** 前端最终确认后的整份行程结构。 */
    @NotEmpty(message = "行程明细不能为空")
    private List<AiTravelPlanDTO.DailyScheduleDTO> dailySchedules;
}
