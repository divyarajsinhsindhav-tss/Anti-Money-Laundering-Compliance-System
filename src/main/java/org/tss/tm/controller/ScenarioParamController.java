package org.tss.tm.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tss.tm.common.response.ApiResponse;
import org.tss.tm.dto.tenant.request.ScenarioParamUploadRequest;
import org.tss.tm.entity.tenant.ScenarioParam;
import org.tss.tm.service.interfaces.ParamService;

@RestController
@RequestMapping("/api/v1/file")
public class ScenarioParamController {

    @Autowired
    private ParamService paramService;

    public ResponseEntity<ApiResponse<Object>> updateScenarioParams(
            @RequestBody ScenarioParamUploadRequest request,
            HttpServletRequest httpServletRequest
            ){
        paramService.updateScenarioParams(request);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Parameter updated successfully",
                httpServletRequest.getRequestURI(),
                null
        ));
    }

}
