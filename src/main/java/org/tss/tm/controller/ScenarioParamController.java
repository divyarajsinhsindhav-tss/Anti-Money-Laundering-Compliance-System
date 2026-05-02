package org.tss.tm.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.dto.tenant.request.ScenarioParamUploadRequest;
import org.tss.tm.dto.tenant.response.ScenarioParamResponse;
import org.tss.tm.entity.tenant.ScenarioParam;
import org.tss.tm.service.interfaces.ParamService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sceario-param")
public class ScenarioParamController {

    private final ParamService paramService;

    @PutMapping
    public ResponseEntity<ApiResponse<Object>> updateScenarioParams(
            @RequestBody ScenarioParamUploadRequest request,
            HttpServletRequest httpServletRequest) {
        paramService.updateScenarioParams(request);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Parameter updated successfully",
                httpServletRequest.getRequestURI(),
                null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScenarioParamResponse>>> getScenarioParams(
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Scenario parameters retrieved successfully",
                request.getRequestURI(),
                paramService.getAllScenarioParams()));
    }

}
