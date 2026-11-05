package io.dennis.inventory.demo.controller;

import io.dennis.inventory.demo.api.ApiRequest;
import io.dennis.inventory.demo.api.ApiResponse;
import io.dennis.inventory.demo.api.RequestIdUtil;
import io.dennis.inventory.demo.dto.CreateInventoryRequest;
import io.dennis.inventory.demo.dto.InventoryView;
import io.dennis.inventory.demo.dto.QueryInventoryRequest;
import io.dennis.inventory.demo.dto.UpdateInventoryRequest;
import io.dennis.inventory.demo.logging.ApiRequestLogger;
import io.dennis.inventory.demo.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/query")
    public ApiResponse<List<InventoryView>> query(
            @RequestBody ApiRequest<QueryInventoryRequest> request,
            HttpServletRequest httpServletRequest) {
        String requestId = RequestIdUtil.requireRequestId(request, httpServletRequest);
        return ApiRequestLogger.log("inventory.query", requestId,
                () -> ApiResponse.success(requestId, inventoryService.query()));
    }

    @PostMapping("/create")
    public ApiResponse<InventoryView> create(
            @RequestBody ApiRequest<CreateInventoryRequest> request,
            HttpServletRequest httpServletRequest) {
        String requestId = RequestIdUtil.requireRequestId(request, httpServletRequest);
        return ApiRequestLogger.log("inventory.create", requestId,
                () -> ApiResponse.success(requestId, inventoryService.create(request.params())));
    }

    @PostMapping("/update")
    public ApiResponse<InventoryView> update(
            @RequestBody ApiRequest<UpdateInventoryRequest> request,
            HttpServletRequest httpServletRequest) {
        String requestId = RequestIdUtil.requireRequestId(request, httpServletRequest);
        return ApiRequestLogger.log("inventory.update", requestId,
                () -> ApiResponse.success(requestId, inventoryService.update(request.params())));
    }
}
