package com.fallguys.itemservice.infrastructure.client;

import com.fallguys.itemservice.domain.InventoryItemSynchronizer;
import com.fallguys.itemservice.domain.ItemUnit;
import com.fallguys.itemservice.domain.exception.InventorySyncFailedException;
import com.fallguys.itemservice.domain.exception.InventorySyncUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;

@Component
public class InventoryItemSynchronizerAdapter implements InventoryItemSynchronizer {

    private final RestClient restClient;
    private final InventoryClientProperties properties;

    public InventoryItemSynchronizerAdapter(
            @Qualifier("inventoryRestClient") RestClient restClient,
            InventoryClientProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public void syncName(String sku, String itemName) {
        String validatedSku = Objects.requireNonNull(sku, "sku");
        String validatedItemName = Objects.requireNonNull(itemName, "itemName");
        execute("itemName", () -> restClient.patch()
                .uri("/internal/inventory/items/{sku}/name", validatedSku)
                .headers(headers -> headers.setBearerAuth(resolveBearerToken()))
                .body(new InventoryItemNameUpdateRequest(validatedItemName))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void syncUnit(String sku, ItemUnit itemUnit) {
        String validatedSku = Objects.requireNonNull(sku, "sku");
        ItemUnit validatedItemUnit = Objects.requireNonNull(itemUnit, "itemUnit");
        execute("itemUnit", () -> restClient.patch()
                .uri("/internal/inventory/items/{sku}/unit", validatedSku)
                .headers(headers -> headers.setBearerAuth(resolveBearerToken()))
                .body(new InventoryItemUnitUpdateRequest(validatedItemUnit.getCode()))
                .retrieve()
                .toBodilessEntity());
    }

    @Override
    public void syncActive(String sku, boolean active) {
        String validatedSku = Objects.requireNonNull(sku, "sku");
        execute("active", () -> restClient.patch()
                .uri("/internal/inventory/items/{sku}/active", validatedSku)
                .headers(headers -> headers.setBearerAuth(resolveBearerToken()))
                .body(new InventoryItemActiveUpdateRequest(active))
                .retrieve()
                .toBodilessEntity());
    }

    private void execute(String fieldName, Runnable request) {
        for (int attempt = 1; attempt <= properties.retryMaxAttempts(); attempt++) {
            try {
                request.run();
                return;
            } catch (RestClientResponseException ex) {
                if (shouldRetry(ex.getStatusCode(), attempt)) {
                    continue;
                }
                throw toBusinessException(fieldName, ex);
            } catch (ResourceAccessException ex) {
                if (attempt < properties.retryMaxAttempts()) {
                    continue;
                }
                throw new InventorySyncUnavailableException("재고 서비스에 연결할 수 없습니다: " + fieldName, ex);
            } catch (RestClientException ex) {
                throw new InventorySyncUnavailableException("재고 서비스 호출에 실패했습니다: " + fieldName, ex);
            }
        }
    }

    private boolean shouldRetry(HttpStatusCode statusCode, int attempt) {
        return statusCode.is5xxServerError() && attempt < properties.retryMaxAttempts();
    }

    private RuntimeException toBusinessException(String fieldName, RestClientResponseException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            return new InventorySyncUnavailableException("재고 서비스가 정상 응답하지 않습니다: " + fieldName, ex);
        }
        return new InventorySyncFailedException("재고 서비스 동기화 요청이 거부되었습니다: " + fieldName, ex);
    }

    private String resolveBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getTokenValue();
        }
        throw new InventorySyncFailedException("재고 서비스 동기화를 위한 인증 토큰이 없습니다.", null);
    }

    private record InventoryItemNameUpdateRequest(String itemName) {
    }

    private record InventoryItemUnitUpdateRequest(String itemUnit) {
    }

    private record InventoryItemActiveUpdateRequest(boolean active) {
    }
}
