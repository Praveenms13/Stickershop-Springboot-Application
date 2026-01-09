
package com.praveen.ecommerce.service.impl;

import com.praveen.ecommerce.dto.ProductDto;
import com.praveen.ecommerce.entity.Product;
import com.praveen.ecommerce.repository.ProductRepository;
import com.praveen.ecommerce.service.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final S3Client s3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.public-base-url}")
    private String publicBaseUrl;

    private final ProductRepository productRepository;

    @Override
    // @Cacheable("products")
    public List<ProductDto> getProducts() {
        getSecret();
        long start = System.currentTimeMillis();
        log.debug("Custom User Logs > getProducts() called");
        List<ProductDto> result = productRepository.findAll()
                .stream()
                .map(this::transformToDto)
                .collect(Collectors.toList());
        log.info("Custom User Logs > Products fetched: count={}, durationMs={}", result.size(), (System.currentTimeMillis() - start));
        return result;
    }

    private ProductDto transformToDto(Product product) {
        ProductDto productDto = new ProductDto();
        BeanUtils.copyProperties(product, productDto);
        productDto.setProductId(product.getId());
        return productDto;
    }

    private Product transformToEntity(ProductDto dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        return product;
    }

    @Override
    @Transactional
    public ProductDto createProduct(ProductDto productDto, MultipartFile image) {
        long start = System.currentTimeMillis();
        log.info("Custom User Logs > createProduct() invoked: imageSize={}, contentType={}",
                image != null ? image.getSize() : -1,
                image != null ? image.getContentType() : "null");

        validateImage(image);

        // 1) Build S3 object key
        String safeName = sanitize(image.getOriginalFilename());
        String uuid = UUID.randomUUID().toString();
        String key = "products/original/%s_%s".formatted(uuid, safeName);

        // 2) Upload to S3
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(image.getContentType())
                .build();

        log.debug("Custom User Logs > Uploading image to S3: bucket={}, keyMasked={}, size={}",
                SafeLog.maskBucket(bucket),
                SafeLog.maskKey(key),
                image.getSize());

        try {
            s3.putObject(put, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));

            // 3) Compose public URL (do NOT log full URL)
            String urlKey = urlEncodePath(key);
            String imageUrl = publicBaseUrl + "/" + urlKey;

            Product entity = transformToEntity(productDto);
            entity.setImageUrl(imageUrl);
            entity.setImageBucket(bucket);
            entity.setImageKey(key);

            Product saved = productRepository.save(entity);

            log.info("Custom User Logs > Product created: id={}, imageStored={}, durationMs={}",
                    saved.getId(),
                    true,
                    (System.currentTimeMillis() - start));

            return transformToDto(saved);

        } catch (RuntimeException re) {
            safeDeleteObject(bucket, key, "RuntimeException during createProduct");
            log.error("Custom User Logs > RuntimeException in createProduct: message={}, bucket={}, keyMasked={}",
                    re.getMessage(),
                    SafeLog.maskBucket(bucket),
                    SafeLog.maskKey(key),
                    re);
            throw re;
        } catch (Exception ex) {
            log.error("Custom User Logs > Image upload failed: bucket={}, keyMasked={}, contentType={}, size={}, durationMs={}",
                    SafeLog.maskBucket(bucket),
                    SafeLog.maskKey(key),
                    image.getContentType(),
                    image.getSize(),
                    (System.currentTimeMillis() - start),
                    ex);
            throw new IllegalStateException("Image upload failed", ex);
        }
    }

    private void safeDeleteObject(String bucket, String key, String reason) {
        try {
            log.warn("Custom User Logs > Attempting S3 cleanup: reason={}, bucket={}, keyMasked={}",
                    reason, SafeLog.maskBucket(bucket), SafeLog.maskKey(key));
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.info("Custom User Logs > S3 cleanup succeeded: bucket={}, keyMasked={}",
                    SafeLog.maskBucket(bucket), SafeLog.maskKey(key));
        } catch (Exception cleanupEx) {
            // Log but do not rethrow to avoid masking the original exception
            log.error("Custom User Logs > S3 cleanup failed: bucket={}, keyMasked={}, error={}",
                    SafeLog.maskBucket(bucket), SafeLog.maskKey(key), cleanupEx.getMessage(), cleanupEx);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            log.warn("Custom User Logs > Image validation failed: empty or null");
            throw new IllegalArgumentException("Image is required");
        }
        if (image.getSize() > 10 * 1024 * 1024) {
            log.warn("Custom User Logs > Image validation failed: size too large size={}", image.getSize());
            throw new IllegalArgumentException("Max image size is 10MB");
        }
        String ct = image.getContentType();
        if (ct == null || !(ct.equals("image/jpeg") || ct.equals("image/png") || ct.equals("image/webp"))) {
            log.warn("Custom User Logs > Image validation failed: unsupported contentType={}", ct);
            throw new IllegalArgumentException("Only JPEG/PNG/WEBP allowed");
        }
        log.debug("Custom User Logs > Image validated: size={}, contentType={}", image.getSize(), ct);
    }

    private String sanitize(String name) {
        String sanitized = name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
        log.debug("Custom User Logs > Sanitized filename: masked={}", SafeLog.maskFilename(sanitized));
        return sanitized;
    }

    private String urlEncodePath(String key) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // FOR SAFE LOGGING ....
    static final class SafeLog {
        private SafeLog() {}

        static String maskBucket(String bucket) {
            if (bucket == null || bucket.isBlank()) return "bucket:****";
            return bucket.length() <= 4 ? "****" : bucket.substring(0, 2) + "****";
        }

        static String maskKey(String key) {
            if (key == null || key.isBlank()) return "key:****";
            int slash = key.lastIndexOf('/');
            String prefix = (slash > 0) ? key.substring(0, Math.min(slash, 6)) : key.substring(0, Math.min(6, key.length()));
            String ext = key.contains(".") ? key.substring(key.lastIndexOf('.')) : "";
            return prefix + "/****" + ext;
        }

        static String maskFilename(String name) {
            if (name == null || name.isBlank()) return "name:****";
            if (name.length() <= 4) return "****";
            return name.substring(0, 2) + "****" + (name.contains(".") ? name.substring(name.lastIndexOf('.')) : "");
        }
    }

    public static void getSecret() {
        String secretName = "EcommerceApp/DBCredentials-raw";
        Region region = Region.of("ap-south-1");
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .build();
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
        GetSecretValueResponse getSecretValueResponse;
        try {
            getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        } catch (Exception e) {
            throw e;
        }
        String secret = getSecretValueResponse.secretString();
        System.out.println(secret);
    }
}