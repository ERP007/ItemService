package com.fallguys.itemservice.infrastructure.client;

import com.fallguys.itemservice.domain.ItemUnit;
import com.fallguys.itemservice.domain.exception.InventorySyncFailedException;
import com.fallguys.itemservice.domain.exception.InventorySyncUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryItemSynchronizerAdapterTest {

    private static final String BASE_URL = "http://inventory-service";
    private static final String SKU = "HMC-EN-00214";
    private static final String TOKEN_VALUE = "inventory-token";

    private MockRestServiceServer server;
    private InventoryItemSynchronizerAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new InventoryItemSynchronizerAdapter(
                builder.build(),
                new InventoryClientProperties(BASE_URL, Duration.ofSeconds(1), Duration.ofSeconds(1), 2)
        );

        Jwt jwt = Jwt.withTokenValue(TOKEN_VALUE)
                .header("alg", "none")
                .claim("sub", "item-service-test")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendsNameUnitAndActivePatchRequestsWithBearerToken() {
        server.expect(requestTo(BASE_URL + "/internal/inventory/items/" + SKU + "/name"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_VALUE))
                .andExpect(content().json("""
                        {"itemName":"엔진오일 필터 (개선형)"}
                        """))
                .andRespond(withSuccess("""
                        {"sku":"HMC-EN-00214","updatedCount":0,"warehouseCodes":[]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/internal/inventory/items/" + SKU + "/unit"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_VALUE))
                .andExpect(content().json("""
                        {"itemUnit":"BOX"}
                        """))
                .andRespond(withSuccess("""
                        {"sku":"HMC-EN-00214","updatedCount":2,"warehouseCodes":["BR-SE-001","HQ-001"]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/internal/inventory/items/" + SKU + "/active"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_VALUE))
                .andExpect(content().json("""
                        {"active":false}
                        """))
                .andRespond(withSuccess("""
                        {"sku":"HMC-EN-00214","updatedCount":2,"warehouseCodes":["BR-SE-001","HQ-001"]}
                        """, MediaType.APPLICATION_JSON));

        adapter.syncName(SKU, "엔진오일 필터 (개선형)");
        adapter.syncUnit(SKU, ItemUnit.BOX);
        adapter.syncActive(SKU, false);

        server.verify();
    }

    @Test
    void mapsForbiddenResponseToInventorySyncFailed() {
        server.expect(requestTo(BASE_URL + "/internal/inventory/items/" + SKU + "/name"))
                .andRespond(withForbiddenRequest());

        assertThrows(InventorySyncFailedException.class, () -> adapter.syncName(SKU, "엔진오일 필터"));
        server.verify();
    }

    @Test
    void retriesServerErrorsAndMapsToInventorySyncUnavailable() {
        server.expect(requestTo(BASE_URL + "/internal/inventory/items/" + SKU + "/active"))
                .andRespond(withServerError());
        server.expect(requestTo(BASE_URL + "/internal/inventory/items/" + SKU + "/active"))
                .andRespond(withServerError());

        assertThrows(InventorySyncUnavailableException.class, () -> adapter.syncActive(SKU, true));
        server.verify();
    }
}
