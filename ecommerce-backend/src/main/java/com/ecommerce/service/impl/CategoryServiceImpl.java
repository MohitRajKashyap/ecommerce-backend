package com.ecommerce.service.impl;

import com.ecommerce.dto.request.CategoryRequest;
import com.ecommerce.dto.response.CategoryResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceAlreadyExistsException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.service.CategoryService;
import com.ecommerce.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SlugUtils slugUtils;

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Category with name '" + request.getName() + "' already exists");
        }

        String slug = slugUtils.toSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", request.getParentId()));
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .parent(parent)
                .active(request.isActive())
                .build();

        return toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (!category.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Category name already taken");
        }

        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new BadRequestException("Category cannot be its own parent");
        }

        category.setName(request.getName());
        category.setSlug(slugUtils.toSlug(request.getName()));
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setActive(request.isActive());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Cacheable(value = "categories", key = "'id:' + #id")
    public CategoryResponse getCategoryById(Long id) {
        return toCategoryResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id)));
    }

    @Override
    @Cacheable(value = "categories", key = "'slug:' + #slug")
    public CategoryResponse getCategoryBySlug(String slug) {
        return toCategoryResponse(categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug)));
    }

    @Override
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllActiveCategories()
                .stream().map(this::toCategoryResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "categories", key = "'roots'")
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNullAndActiveTrue()
                .stream().map(this::toCategoryResponseWithChildren).collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getChildCategories(Long parentId) {
        return categoryRepository.findByParentIdAndActiveTrue(parentId)
                .stream().map(this::toCategoryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        if (!category.getChildren().isEmpty()) {
            throw new BadRequestException("Cannot delete category with subcategories. Remove subcategories first.");
        }
        categoryRepository.delete(category);
    }

    private CategoryResponse toCategoryResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .active(c.isActive())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .parentName(c.getParent() != null ? c.getParent().getName() : null)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private CategoryResponse toCategoryResponseWithChildren(Category c) {
        CategoryResponse response = toCategoryResponse(c);
        List<CategoryResponse> children = c.getChildren().stream()
                .filter(Category::isActive)
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
        response.setChildren(children);
        return response;
    }
}
