package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryRequest;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.exception.InventoryException;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для InventoryService.
 * Покрытие: создание инвентаря, резервирование, обновление остатков.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    private static final String PRODUCT_ID = "507f1f77bcf86cd799439011";

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory testInventory;
    private InventoryRequest testRequest;
    private InventoryResponse testResponse;

    @BeforeEach
    void setUp() {
        testInventory = Inventory.builder()
                .id(1L)
                .productId(PRODUCT_ID)
                .quantityAvailable(50L)
                .quantityReserved(10L)
                .build();

        testRequest = new InventoryRequest();
        testRequest.setProductId(PRODUCT_ID);
        testRequest.setTotalQuantity(50);

        testResponse = InventoryResponse.builder()
                .id(1L)
                .productId(PRODUCT_ID)
                .quantityAvailable(50L)
                .quantityReserved(10L)
                .build();
    }

    // ========== CREATE INVENTORY TESTS ==========

    @Nested
    @DisplayName("createInventory()")
    class CreateInventoryTests {

        @Test
        @DisplayName("должен успешно создать инвентарь для нового товара")
        void shouldCreateInventorySuccessfully() {
            // Given
            when(inventoryRepository.existsByProductId(PRODUCT_ID)).thenReturn(false);
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
                Inventory inv = invocation.getArgument(0);
                inv.setId(1L);
                return inv;
            });
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(testResponse);

            // When
            InventoryResponse response = inventoryService.createInventory(testRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(response.getQuantityAvailable()).isEqualTo(50L);

            ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(captor.capture());

            Inventory savedInventory = captor.getValue();
            assertThat(savedInventory.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(savedInventory.getQuantityAvailable()).isEqualTo(50L);
            assertThat(savedInventory.getQuantityReserved()).isEqualTo(0L);
        }

        @Test
        @DisplayName("должен выбросить исключение если инвентарь уже существует")
        void shouldThrowExceptionWhenInventoryAlreadyExists() {
            // Given
            when(inventoryRepository.existsByProductId(PRODUCT_ID)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> inventoryService.createInventory(testRequest))
                    .isInstanceOf(InventoryException.class)
                    .hasMessageContaining("Inventory already exists");

            verify(inventoryRepository, never()).save(any());
        }
    }

    // ========== GET INVENTORY TESTS ==========

    @Nested
    @DisplayName("getInventory()")
    class GetInventoryTests {

        @Test
        @DisplayName("должен вернуть инвентарь по productId")
        void shouldReturnInventoryByProductId() {
            // Given
            when(inventoryRepository.findByProductId(PRODUCT_ID))
                    .thenReturn(Optional.of(testInventory));
            when(inventoryMapper.toResponse(testInventory)).thenReturn(testResponse);

            // When
            InventoryResponse response = inventoryService.getInventory(PRODUCT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
            assertThat(response.getQuantityAvailable()).isEqualTo(50L);
        }

        @Test
        @DisplayName("должен выбросить исключение если инвентарь не найден")
        void shouldThrowExceptionWhenInventoryNotFound() {
            // Given
            when(inventoryRepository.findByProductId("p999"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryService.getInventory("p999"))
                    .isInstanceOf(InventoryException.class)
                    .hasMessageContaining("Inventory not found");
        }

        @Test
        @DisplayName("должен корректно парсить productId")
        void shouldParseProductIdCorrectly() {
            // Given
            when(inventoryRepository.findByProductId("p123"))
                    .thenReturn(Optional.of(testInventory));
            when(inventoryMapper.toResponse(testInventory)).thenReturn(testResponse);

            // When
            inventoryService.getInventory("p123");

            // Then
            verify(inventoryRepository).findByProductId("p123");
        }
    }

    // ========== GET INVENTORY LOCKED FOR UPDATE TESTS ==========

    @Nested
    @DisplayName("getInventoryLockedForUpdate()")
    class GetInventoryLockedTests {

        @Test
        @DisplayName("должен вернуть инвентарь с блокировкой")
        void shouldReturnLockedInventory() {
            // Given
            when(inventoryRepository.findByProductIdWithLock(PRODUCT_ID))
                    .thenReturn(Optional.of(testInventory));

            // When
            Inventory result = inventoryService.getInventoryLockedForUpdate(PRODUCT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getProductId()).isEqualTo(PRODUCT_ID);
        }

        @Test
        @DisplayName("должен выбросить исключение если инвентарь не найден")
        void shouldThrowExceptionWhenNotFoundLocked() {
            // Given
            when(inventoryRepository.findByProductIdWithLock("p999"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryService.getInventoryLockedForUpdate("p999"))
                    .isInstanceOf(InventoryException.class)
                    .hasMessageContaining("Inventory not found");
        }
    }

    // ========== UPDATE TOTAL QUANTITY TESTS ==========

    @Nested
    @DisplayName("updateTotalQuantity()")
    class UpdateTotalQuantityTests {

        @Test
        @DisplayName("должен обновить количество товара")
        void shouldUpdateTotalQuantity() {
            // Given
            when(inventoryRepository.findByProductIdWithLock(PRODUCT_ID))
                    .thenReturn(Optional.of(testInventory));
            when(inventoryRepository.save(any(Inventory.class)))
                    .thenReturn(testInventory);

            InventoryResponse updatedResponse = InventoryResponse.builder()
                    .id(1L)
                    .productId(PRODUCT_ID)
                    .quantityAvailable(200L)
                    .quantityReserved(10L)
                    .build();
            when(inventoryMapper.toResponse(any(Inventory.class))).thenReturn(updatedResponse);

            // When
            InventoryResponse response = inventoryService.updateTotalQuantity(PRODUCT_ID, 200);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getQuantityAvailable()).isEqualTo(200L);

            verify(inventoryRepository).save(argThat(inv ->
                    inv.getQuantityAvailable() == 200L
            ));
        }

        @Test
        @DisplayName("должен выбросить исключение если инвентарь не найден")
        void shouldThrowExceptionWhenNotFoundOnUpdate() {
            // Given
            when(inventoryRepository.findByProductIdWithLock("p999"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryService.updateTotalQuantity("p999", 100))
                    .isInstanceOf(InventoryException.class);
        }
    }
}

