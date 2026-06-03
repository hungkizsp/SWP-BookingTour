package com.tourbooking.booking.backend.mapper;

import com.tourbooking.booking.backend.model.dto.request.CategoryRequest;
import com.tourbooking.booking.backend.model.dto.response.CategoryResponse;
import com.tourbooking.booking.backend.model.entity.Category;

public class CategoryMapper {

    public static CategoryResponse toResponse(Category category) {
        if (category == null) return null;
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setCategoryName(category.getCategoryName());
        response.setDescription(category.getDescription());
        return response;
    }

    public static Category toEntity(CategoryRequest request) {
        if (request == null) return null;
        Category category = new Category();
        updateEntityFromRequest(category, request);
        return category;
    }

    public static void updateEntityFromRequest(Category category, CategoryRequest request) {
        if (request == null || category == null) return;
        if (request.getCategoryName() != null) category.setCategoryName(request.getCategoryName());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
    }
}
