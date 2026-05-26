package com.ecommerce.service;

import com.ecommerce.dto.request.CategoryRequest;
import com.ecommerce.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse getCategoryBySlug(String slug);
    List<CategoryResponse> getAllCategories();
    List<CategoryResponse> getRootCategories();
    List<CategoryResponse> getChildCategories(Long parentId);
    void deleteCategory(Long id);
}
