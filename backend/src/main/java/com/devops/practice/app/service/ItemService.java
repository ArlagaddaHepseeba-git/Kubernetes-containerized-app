package com.devops.practice.app.service;

import com.devops.practice.app.entity.Item;
import com.devops.practice.app.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    /**
     * Persists a new item.
     *
     * The name is trimmed before saving so that leading and trailing
     * whitespace never reaches the database. Callers are still expected to
     * reject blank values via bean validation on the request body.
     *
     * @param name item name, surrounding whitespace is removed
     * @return the persisted item, including its generated id
     */
    public Item addItem(String name) {
        Item item = new Item();
        item.setName(name != null ? name.trim() : null);
        return itemRepository.save(item);
    }

    public boolean deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            return false;
        }
        itemRepository.deleteById(id);
        return true;
    }
}