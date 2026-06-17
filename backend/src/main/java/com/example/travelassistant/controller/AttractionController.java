package com.example.travelassistant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.travelassistant.common.Result;
import com.example.travelassistant.dto.AttractionCreateRequest;
import com.example.travelassistant.dto.AttractionUpdateRequest;
import com.example.travelassistant.dto.ChartItemVO;
import com.example.travelassistant.entity.Attraction;
import com.example.travelassistant.service.AttractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attraction")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionService attractionService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String city) {
        IPage<Attraction> page = attractionService.pageAttractions(current, size, keyword, city);
        return Result.success(Map.of("total", page.getTotal(), "records", page.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<Attraction> get(@PathVariable Long id) {
        return Result.success(attractionService.getAttraction(id));
    }

    @GetMapping("/listByCity")
    public Result<List<Attraction>> listByCity(@RequestParam String city) {
        return Result.success(attractionService.listByCity(city));
    }

    @GetMapping("/top")
    public Result<List<Attraction>> top(@RequestParam(defaultValue = "3") Integer limit) {
        return Result.success(attractionService.listTopReferenced(limit));
    }

    @GetMapping("/cityDistribution")
    public Result<List<ChartItemVO>> cityDistribution() {
        return Result.success(attractionService.listCityDistribution());
    }

    @PostMapping
    public Result<Attraction> create(@Valid @RequestBody AttractionCreateRequest request) {
        return Result.success(attractionService.createAttraction(request));
    }

    @PutMapping
    public Result<Attraction> update(@Valid @RequestBody AttractionUpdateRequest request) {
        return Result.success(attractionService.updateAttraction(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Long operatorUserId) {
        attractionService.deleteAttraction(id, operatorUserId);
        return Result.success(null);
    }
}
