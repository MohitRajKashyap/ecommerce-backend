package com.ecommerce.service;

import com.ecommerce.dto.request.AddressRequest;
import com.ecommerce.dto.request.UpdateProfileRequest;
import com.ecommerce.dto.response.AddressResponse;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    UserResponse getCurrentUserProfile();
    UserResponse updateProfile(UpdateProfileRequest request);
    UserResponse getUserById(Long id);
    PagedResponse<UserResponse> getAllUsers(Pageable pageable);
    void toggleUserActive(Long id, boolean active);

    AddressResponse addAddress(AddressRequest request);
    AddressResponse updateAddress(Long addressId, AddressRequest request);
    List<AddressResponse> getMyAddresses();
    void deleteAddress(Long addressId);
    void setDefaultAddress(Long addressId);
}
