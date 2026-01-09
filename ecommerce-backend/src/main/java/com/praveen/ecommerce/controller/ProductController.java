package com.praveen.ecommerce.controller;

import com.praveen.ecommerce.dto.ProductDto;
import com.praveen.ecommerce.service.IProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
// @CrossOrigin(origins = "http://localhost:5173/")
public class ProductController {
    private final IProductService iProductService;

    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts() {
        return ResponseEntity.ok().body(iProductService.getProducts());
    }

    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ProductDto> createProductWithImage(
            @RequestPart("product") @Valid ProductDto productDto,
            @RequestPart("image") org.springframework.web.multipart.MultipartFile image,
            UriComponentsBuilder uriBuilder
    ) {
        ProductDto created = iProductService.createProduct(productDto, image);
        URI location = uriBuilder
                .path("/api/v1/products/{id}")
                .buildAndExpand(created.getProductId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
