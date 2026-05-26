package com.ecommerce.service.impl;

import com.ecommerce.dto.request.AddressRequest;
import com.ecommerce.dto.request.UpdateProfileRequest;
import com.ecommerce.dto.response.AddressResponse;
import com.ecommerce.dto.response.PagedResponse;
import com.ecommerce.dto.response.UserResponse;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.UserService;
import com.ecommerce.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final SecurityUtils securityUtils;

    // ===== PROFILE =====

    @Override
    @Cacheable(value = "user-profile", key = "#root.target.securityUtils.currentUserId")
    public UserResponse getCurrentUserProfile() {
        return toUserResponse(securityUtils.getCurrentUser());
    }

    @Override
    @Transactional
    @CacheEvict(value = "user-profile", allEntries = true)
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = securityUtils.getCurrentUser();

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getPhone()     != null) user.setPhone(request.getPhone());
        if (request.getProfileImageUrl() != null) user.setProfileImageUrl(request.getProfileImageUrl());

        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable).map(this::toUserResponse);
        return PagedResponse.of(page);
    }

    @Override
    @Transactional
    public void toggleUserActive(Long id, boolean active) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.updateActiveStatus(id, active);
        log.info("User {} active status set to {}", id, active);
    }

    // ===== ADDRESSES =====

    @Override
    @Transactional
    public AddressResponse addAddress(AddressRequest request) {
        User user = securityUtils.getCurrentUser();

        if (addressRepository.countByUserId(user.getId()) >= 10) {
            throw new BadRequestException("Maximum of 10 addresses allowed per account");
        }

        // If new address is marked default, clear existing default
        if (request.isDefaultAddress()) {
            addressRepository.clearDefaultAddress(user.getId());
        }

        // If this is the first address, make it default automatically
        boolean isFirst = addressRepository.countByUserId(user.getId()) == 0;

        Address address = Address.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .defaultAddress(request.isDefaultAddress() || isFirst)
                .addressType(request.getAddressType())
                .user(user)
                .build();

        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long addressId, AddressRequest request) {
        User user = securityUtils.getCurrentUser();
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        if (request.isDefaultAddress()) {
            addressRepository.clearDefaultAddress(user.getId());
        }

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPincode(request.getPincode());
        address.setDefaultAddress(request.isDefaultAddress());
        address.setAddressType(request.getAddressType());

        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses() {
        Long userId = securityUtils.getCurrentUserId();
        return addressRepository.findByUserId(userId)
                .stream().map(this::toAddressResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId) {
        Long userId = securityUtils.getCurrentUserId();
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        addressRepository.delete(address);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long addressId) {
        Long userId = securityUtils.getCurrentUserId();
        addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        addressRepository.clearDefaultAddress(userId);
        addressRepository.findById(addressId).ifPresent(a -> {
            a.setDefaultAddress(true);
            addressRepository.save(a);
        });
    }

    // ===== MAPPERS =====

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private AddressResponse toAddressResponse(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .city(a.getCity())
                .state(a.getState())
                .country(a.getCountry())
                .pincode(a.getPincode())
                .defaultAddress(a.isDefaultAddress())
                .addressType(a.getAddressType())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
