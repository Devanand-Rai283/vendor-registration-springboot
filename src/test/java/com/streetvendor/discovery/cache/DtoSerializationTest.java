package com.streetvendor.discovery.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.streetvendor.discovery.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DtoSerializationTest {

    private GenericJackson2JsonRedisSerializer serializer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.EVERYTHING);
        this.serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    @Test
    void testVendorMenuResponseDtoSerialization() {
        MenuItemResponseDto item = new MenuItemResponseDto(
                UUID.randomUUID(),
                "Burger",
                "Delicious burger",
                new BigDecimal("9.99"),
                "Non-Veg",
                "http://example.com/burger.jpg",
                true
        );

        MenuCategoryResponseDto category = new MenuCategoryResponseDto(
                UUID.randomUUID(),
                "Main Course",
                1,
                List.of(item)
        );

        VendorMenuResponseDto original = new VendorMenuResponseDto(
                UUID.randomUUID(),
                "Burger King",
                List.of(category)
        );

        byte[] serialized = serializer.serialize(original);
        assertThat(serialized).isNotNull();

        Object deserialized = serializer.deserialize(serialized);
        assertThat(deserialized).isInstanceOf(VendorMenuResponseDto.class);

        VendorMenuResponseDto restored = (VendorMenuResponseDto) deserialized;
        assertThat(restored.vendorId()).isEqualTo(original.vendorId());
        assertThat(restored.vendorName()).isEqualTo(original.vendorName());
        assertThat(restored.categories()).hasSize(1);
        assertThat(restored.categories().get(0).name()).isEqualTo("Main Course");
        assertThat(restored.categories().get(0).items()).hasSize(1);
        assertThat(restored.categories().get(0).items().get(0).price()).isEqualTo(new BigDecimal("9.99"));
    }

    @Test
    void testNearbyVendorResponseSerialization() {
        VendorSummaryResponse vendor1 = new VendorSummaryResponse(
                UUID.randomUUID(),
                "Pizza Hut",
                "Italian",
                "123 Pizza St",
                new BigDecimal("4.5"),
                40.7128,
                -74.0060,
                1.5
        );

        VendorSummaryResponse vendor2 = new VendorSummaryResponse(
                UUID.randomUUID(),
                "Taco Bell",
                "Mexican",
                "456 Taco Ave",
                new BigDecimal("4.2"),
                40.7130,
                -74.0065,
                2.0
        );

        NearbyVendorResponse original = new NearbyVendorResponse(
                List.of(vendor1, vendor2),
                0,
                10,
                2,
                1
        );

        byte[] serialized = serializer.serialize(original);
        assertThat(serialized).isNotNull();

        Object deserialized = serializer.deserialize(serialized);
        assertThat(deserialized).isInstanceOf(NearbyVendorResponse.class);

        NearbyVendorResponse restored = (NearbyVendorResponse) deserialized;
        assertThat(restored.totalElements()).isEqualTo(2);
        assertThat(restored.vendors()).hasSize(2);
        assertThat(restored.vendors().get(0).businessName()).isEqualTo("Pizza Hut");
        assertThat(restored.vendors().get(1).businessName()).isEqualTo("Taco Bell");
    }

    @Test
    void testSerializationWithNullFields() {
        MenuItemResponseDto itemWithNulls = new MenuItemResponseDto(
                UUID.randomUUID(),
                "Water",
                null, // description null
                new BigDecimal("1.00"),
                null, // dietary tag null
                null, // image URL null
                true
        );

        byte[] serialized = serializer.serialize(itemWithNulls);
        Object deserialized = serializer.deserialize(serialized);
        
        assertThat(deserialized).isInstanceOf(MenuItemResponseDto.class);
        MenuItemResponseDto restored = (MenuItemResponseDto) deserialized;
        assertThat(restored.description()).isNull();
        assertThat(restored.dietaryTag()).isNull();
        assertThat(restored.imageUrl()).isNull();
    }

    @Test
    void testSerializationWithEmptyCollections() {
        VendorMenuResponseDto emptyMenu = new VendorMenuResponseDto(
                UUID.randomUUID(),
                "Empty Vendor",
                Collections.emptyList()
        );

        NearbyVendorResponse emptySearch = new NearbyVendorResponse(
                Collections.emptyList(),
                0,
                10,
                0,
                0
        );

        assertThatCode(() -> {
            byte[] menuBytes = serializer.serialize(emptyMenu);
            Object restoredMenu = serializer.deserialize(menuBytes);
            assertThat(((VendorMenuResponseDto) restoredMenu).categories()).isEmpty();

            byte[] searchBytes = serializer.serialize(emptySearch);
            Object restoredSearch = serializer.deserialize(searchBytes);
            assertThat(((NearbyVendorResponse) restoredSearch).vendors()).isEmpty();
        }).doesNotThrowAnyException();
    }
}
