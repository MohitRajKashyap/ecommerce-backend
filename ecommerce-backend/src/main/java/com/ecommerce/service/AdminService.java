package com.ecommerce.service;

import com.ecommerce.dto.response.DashboardResponse;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminService {
    DashboardResponse getDashboard();
    PagedResponse<UserResponse> getAllUsers(Pageable pageable);
    void activateUser(Long userId);
    void deactivateUser(Long userId);
    List<ProductResponse> getLowStockProducts(int threshold, int limit);
}
