package com.ecommerce.repository;

import com.ecommerce.entity.User;
import com.ecommerce.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@test.com")
                .password("$2a$12$encodedPassword")
                .role(Role.CUSTOMER)
                .active(true)
                .emailVerified(false)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        Optional<User> found = userRepository.findByEmail("john.doe@test.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        boolean exists = userRepository.existsByEmail("john.doe@test.com");
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("nonexistent@test.com");
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should find users by role")
    void shouldFindUsersByRole() {
        User seller = User.builder()
                .firstName("Jane").lastName("Seller")
                .email("seller@test.com")
                .password("encoded").role(Role.SELLER)
                .active(true).emailVerified(true).build();
        userRepository.save(seller);

        Page<User> customers = userRepository.findByRole(Role.CUSTOMER, PageRequest.of(0, 10));
        Page<User> sellers   = userRepository.findByRole(Role.SELLER, PageRequest.of(0, 10));

        assertThat(customers.getContent()).hasSize(1);
        assertThat(sellers.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should count users by role")
    void shouldCountUsersByRole() {
        long count = userRepository.countByRole(Role.CUSTOMER);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should search users by name or email")
    void shouldSearchUsersByNameOrEmail() {
        Page<User> result = userRepository.searchUsers("john", PageRequest.of(0, 10));
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john.doe@test.com");
    }

    @Test
    @DisplayName("Should update active status")
    void shouldUpdateActiveStatus() {
        userRepository.updateActiveStatus(testUser.getId(), false);
        Optional<User> updated = userRepository.findById(testUser.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().isActive()).isFalse();
    }

    @Test
    @DisplayName("Should audit timestamps on save")
    void shouldSetAuditTimestamps() {
        assertThat(testUser.getCreatedAt()).isNotNull();
        assertThat(testUser.getUpdatedAt()).isNotNull();
    }
}
