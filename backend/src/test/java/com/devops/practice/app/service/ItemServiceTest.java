package com.devops.practice.app.service;

import com.devops.practice.app.entity.Item;
import com.devops.practice.app.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service-layer tests for ItemService, run against in-memory H2.
 */
@SpringBootTest
class ItemServiceTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void clearRepository() {
        itemRepository.deleteAll();
    }

    @Test
    @DisplayName("addItem persists an item and returns it with an id")
    void addItemPersistsAndReturnsItem() {
        Item created = itemService.addItem("Learn Docker");

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Learn Docker");
        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getAllItems returns every persisted item")
    void getAllItemsReturnsAllItems() {
        itemService.addItem("Item one");
        itemService.addItem("Item two");
        itemService.addItem("Item three");

        List<Item> items = itemService.getAllItems();

        assertThat(items).hasSize(3);
        assertThat(items).extracting(Item::getName)
                .containsExactlyInAnyOrder("Item one", "Item two", "Item three");
    }

    @Test
    @DisplayName("getAllItems returns an empty list when nothing is stored")
    void getAllItemsReturnsEmptyList() {
        assertThat(itemService.getAllItems()).isEmpty();
    }

    @Test
    @DisplayName("deleteItem removes the item and returns true")
    void deleteItemRemovesAndReturnsTrue() {
        Item created = itemService.addItem("Item to delete");

        boolean deleted = itemService.deleteItem(created.getId());

        assertThat(deleted).isTrue();
        assertThat(itemService.getAllItems()).isEmpty();
    }

    @Test
    @DisplayName("deleteItem returns false for an id that does not exist")
    void deleteItemReturnsFalseForUnknownId() {
        assertThat(itemService.deleteItem(999999L)).isFalse();
    }

    @Test
    @DisplayName("addItem trims surrounding whitespace from the name")
    void addItemTrimsWhitespace() {
        Item created = itemService.addItem("   Padded name   ");

        assertThat(created.getName()).isEqualTo("Padded name");
    }
}
