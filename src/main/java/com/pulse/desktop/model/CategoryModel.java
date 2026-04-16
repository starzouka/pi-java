package com.pulse.desktop.model;

import java.time.LocalDateTime;

public class CategoryModel {
    private Integer categoryId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private String slug;

    public CategoryModel() {
    }

    public CategoryModel(Integer categoryId, String name, String description, LocalDateTime createdAt, String slug) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.slug = slug;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Override
    public String toString() {
        return name != null ? name : "Category";
    }
}
