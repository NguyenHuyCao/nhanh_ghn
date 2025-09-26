package com.app84soft.check_in.controller;

import com.app84soft.check_in.dto.response.PageResult;
import com.app84soft.check_in.dto.response.sheet.MergedRowDto;
import com.app84soft.check_in.other_service.sheet.MergeSheetService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/v1/sheet")
@RequiredArgsConstructor
public class SheetMergeController {

    private final MergeSheetService service;

    @GetMapping("/merge")
    @Operation(summary = "Merged sheet (Nhanh + GHN) — English fields")
    public ResponseEntity<?> merge(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) throws com.fasterxml.jackson.core.JsonProcessingException {

        PageResult<MergedRowDto> pr = service.merge(from, to, page, limit);

        Map<String,Object> data = new LinkedHashMap<>();
        data.put("total", pr.getTotal());
        data.put("page", pr.getPage());
        data.put("items", pr.getItems()); // items already in English

        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }
}
