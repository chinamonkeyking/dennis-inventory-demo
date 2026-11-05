package io.dennis.inventory.demo.controller;

import io.dennis.inventory.demo.api.BusinessException;
import io.dennis.inventory.demo.api.ErrorCode;
import io.dennis.inventory.demo.dto.CreateInventoryRequest;
import io.dennis.inventory.demo.dto.InventoryView;
import io.dennis.inventory.demo.dto.UpdateInventoryRequest;
import io.dennis.inventory.demo.entity.Inventory;
import io.dennis.inventory.demo.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    //------------------------------------------------------------------
    // query
    //------------------------------------------------------------------
    @Test
    void returnsInventoriesWithRequestId() throws Exception {
        InventoryView inventory = new InventoryView(
                1,
                "Chocolate Cake",
                1,
                "Food",
                3,
                "Cake",
                10,
                0,
                "admin",
                1_793_803_882_746L,
                null,
                null);
        given(inventoryService.query()).willReturn(List.of(inventory));

        mockMvc.perform(post("/inventory/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"q-1","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestId").value("q-1"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Chocolate Cake"))
                .andExpect(jsonPath("$.data[0].categoryId").value(1))
                .andExpect(jsonPath("$.data[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.data[0].subCategoryId").value(3))
                .andExpect(jsonPath("$.data[0].subCategoryName").value("Cake"))
                .andExpect(jsonPath("$.data[0].quantity").value(10))
                .andExpect(jsonPath("$.data[0].version").value(0));
    }

    @Test
    void returnsEmptyDataArrayWhenNoInventoriesExist() throws Exception {
        given(inventoryService.query()).willReturn(List.of());

        mockMvc.perform(post("/inventory/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"q-empty","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("q-empty"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void queryRejectsMissingRequestId() throws Exception {
        mockMvc.perform(post("/inventory/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(nullValue()))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("invalid request: requestId is required"))
                .andExpect(jsonPath("$.data").value(nullValue()));
        verifyNoInteractions(inventoryService);
    }

    @Test
    void queryRejectsBlankRequestId() throws Exception {
        mockMvc.perform(post("/inventory/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"   ","params":{}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("   "))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("invalid request: requestId is required"));
        verifyNoInteractions(inventoryService);
    }

    //------------------------------------------------------------------
    // create
    //------------------------------------------------------------------
    @Test
    void createsInventoryWithRequestId() throws Exception {
        CreateInventoryRequest createRequest =
                new CreateInventoryRequest("Chocolate Cake", 1, 3, 10);
        InventoryView created = new InventoryView(
                1,
                "Chocolate Cake",
                1,
                "Food",
                3,
                "Cake",
                10,
                0,
                "admin",
                1_793_803_882_746L,
                null,
                null);
        given(inventoryService.create(createRequest)).willReturn(created);

        mockMvc.perform(post("/inventory/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"c-1","params":{
                                  "name":"Chocolate Cake",
                                  "categoryId":1,
                                  "subCategoryId":3,
                                  "quantity":10
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("c-1"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Chocolate Cake"))
                .andExpect(jsonPath("$.data.categoryName").value("Food"))
                .andExpect(jsonPath("$.data.subCategoryName").value("Cake"))
                .andExpect(jsonPath("$.data.quantity").value(10))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.createdBy").value("admin"));
    }

    @Test
    void returnsBusinessErrorWhenInventoryNameAlreadyExists() throws Exception {
        CreateInventoryRequest createRequest =
                new CreateInventoryRequest("Chocolate Cake", 1, 3, 10);
        given(inventoryService.create(createRequest)).willThrow(
                new BusinessException(
                        ErrorCode.DUPLICATE_INVENTORY_NAME, "Chocolate Cake"));

        mockMvc.perform(post("/inventory/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"c-dup","params":{
                                  "name":"Chocolate Cake",
                                  "categoryId":1,
                                  "subCategoryId":3,
                                  "quantity":10
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("c-dup"))
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message")
                        .value("inventory name 'Chocolate Cake' already exists"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void createRejectsMissingRequestId() throws Exception {
        mockMvc.perform(post("/inventory/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"params":{
                                  "name":"Chocolate Cake",
                                  "categoryId":1,
                                  "subCategoryId":3,
                                  "quantity":10
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(nullValue()))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("invalid request: requestId is required"))
                .andExpect(jsonPath("$.data").value(nullValue()));
        verifyNoInteractions(inventoryService);
    }

    @Test
    void createRejectsBlankRequestId() throws Exception {
        mockMvc.perform(post("/inventory/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"   ","params":{
                                  "name":"Chocolate Cake",
                                  "categoryId":1,
                                  "subCategoryId":3,
                                  "quantity":10
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("   "))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("invalid request: requestId is required"))
                .andExpect(jsonPath("$.data").value(nullValue()));
        verifyNoInteractions(inventoryService);
    }

    //------------------------------------------------------------------
    // update
    //------------------------------------------------------------------
    @Test
    void updatesInventoryQuantityWithRequestId() throws Exception {
        UpdateInventoryRequest updateRequest = new UpdateInventoryRequest(1, 0, 8);
        InventoryView updated = new InventoryView(
                1,
                "Chocolate Cake",
                1,
                "Food",
                3,
                "Cake",
                8,
                1,
                "admin",
                1_793_803_882_746L,
                "admin",
                1_793_835_000_000L);
        given(inventoryService.update(updateRequest)).willReturn(updated);

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"u-1","params":{
                                  "id":1,
                                  "version":0,
                                  "quantity":8
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("u-1"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Chocolate Cake"))
                .andExpect(jsonPath("$.data.quantity").value(8))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.updatedBy").value("admin"));
    }

    @Test
    void returnsVersionConflictForStaleVersion() throws Exception {
        UpdateInventoryRequest updateRequest = new UpdateInventoryRequest(1, 0, 8);
        given(inventoryService.update(updateRequest))
                .willThrow(new BusinessException(ErrorCode.VERSION_CONFLICT));

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"u-stale","params":{
                                  "id":1,
                                  "version":0,
                                  "quantity":8
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("u-stale"))
                .andExpect(jsonPath("$.code").value(1004))
                .andExpect(jsonPath("$.message").value("version conflict"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void updateRejectsMissingRequestId() throws Exception {
        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"params":{
                                  "id":1,
                                  "version":0,
                                  "quantity":8
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(nullValue()))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("invalid request: requestId is required"))
                .andExpect(jsonPath("$.data").value(nullValue()));
        verifyNoInteractions(inventoryService);
    }

    @Test
    void updateRejectsBlankRequestId() throws Exception {
        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"   ","params":{
                                  "id":1,
                                  "version":0,
                                  "quantity":8
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("   "))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("invalid request: requestId is required"))
                .andExpect(jsonPath("$.data").value(nullValue()));
        verifyNoInteractions(inventoryService);
    }

    //------------------------------------------------------------------
    // exception handling (shared; endpoints are used as drivers)
    //------------------------------------------------------------------
    @Test
    void rejectsMalformedJsonWithHttp400() throws Exception {
        mockMvc.perform(post("/inventory/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.requestId").value(nullValue()))
                .andExpect(jsonPath("$.code").value(1005))
                .andExpect(jsonPath("$.message").value("invalid request body"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsSystemErrorForUnexpectedException() throws Exception {
        given(inventoryService.query()).willThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/inventory/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"q-err","params":{}}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.requestId").value("q-err"))
                .andExpect(jsonPath("$.code").value(9999))
                .andExpect(jsonPath("$.message").value("system error"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void mapsDataIntegrityViolationToBusinessError() throws Exception {
        CreateInventoryRequest createRequest =
                new CreateInventoryRequest("Chocolate Cake", 1, 3, 10);
        given(inventoryService.create(createRequest))
                .willThrow(new DataIntegrityViolationException("unique constraint"));

        mockMvc.perform(post("/inventory/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"c-db","params":{
                                  "name":"Chocolate Cake",
                                  "categoryId":1,
                                  "subCategoryId":3,
                                  "quantity":10
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("c-db"))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message")
                        .value("invalid request: data conflict"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void mapsOptimisticLockingFailureToVersionConflict() throws Exception {
        UpdateInventoryRequest updateRequest = new UpdateInventoryRequest(1, 0, 8);
        given(inventoryService.update(updateRequest))
                .willThrow(new ObjectOptimisticLockingFailureException(Inventory.class, 1));

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"u-opt","params":{
                                  "id":1,
                                  "version":0,
                                  "quantity":8
                                }}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("u-opt"))
                .andExpect(jsonPath("$.code").value(1004))
                .andExpect(jsonPath("$.message").value("version conflict"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
