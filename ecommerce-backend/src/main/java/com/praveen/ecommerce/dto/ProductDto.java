package com.praveen.ecommerce.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
public class ProductDto {
    private Long productId;

    @NotBlank(message = "Product name is required")
    @Size(max = 250, message = "Name must be at most 250 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be > 0")
    @Digits(integer = 8, fraction = 2, message = "Price must fit precision 10, scale 2")
    private BigDecimal price;

    @Min(value = 0, message = "Popularity must be >= 0")
    private Integer popularity;

    @Size(max = 500, message = "Image URL must be at most 500 characters")
    private String imageUrl;
    private String imageBucket;
    private String imageKey;
    private Instant createdAt;
}
