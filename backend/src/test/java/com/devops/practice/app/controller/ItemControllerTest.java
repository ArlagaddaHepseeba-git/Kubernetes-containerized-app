package com.devops.practice.app.controller;

import com.devops.practice.app.entity.Item;
import com.devops.practice.app.repository.ItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the items API, run against the in-memory H2
 * database configured in src/test/resources/application.properties.
 *
 * These exercise the full stack: controller -> service -> repository -> H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearRepository() {
        itemRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/items returns an empty list when no items exist")
    void getAllItemsReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/items creates an item and returns 201")
    void addItemCreatesItem() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ItemController.ItemRequest("Learn Terraform"));

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Learn Terraform"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("GET /api/items returns created items")
    void getAllItemsReturnsCreatedItems() throws Exception {
        itemRepository.save(newItem("First item"));
        itemRepository.save(newItem("Second item"));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists());
    }

    @Test
    @DisplayName("POST /api/items rejects a blank name with 400")
    void addItemRejectsBlankName() throws Exception {
        String body = objectMapper.writeValueAsString(
                new ItemController.ItemRequest("   "));

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/items/{id} removes the item and returns 204")
    void deleteItemRemovesItem() throws Exception {
        Item saved = itemRepository.save(newItem("Item to delete"));

        mockMvc.perform(delete("/api/items/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /api/items/{id} returns 404 for an unknown id")
    void deleteItemReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/api/items/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Actuator prometheus endpoint is exposed for monitoring")
    void prometheusEndpointIsExposed() throws Exception {
        // This endpoint is what Prometheus scrapes, so a failure here means
        // the monitoring stack has no data to collect.
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    private Item newItem(String name) {
        Item item = new Item();
        item.setName(name);
        return item;
    }
}
