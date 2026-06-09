package com.fallguys.itemservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapi_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesPublicItemOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/items/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Item Service API"))
                .andExpect(jsonPath("$.paths['/api/items']").exists())
                .andExpect(jsonPath("$.paths['/api/items/{sku}']").exists())
                .andExpect(jsonPath("$.paths['/api/items/{sku}/activate']").exists())
                .andExpect(jsonPath("$.paths['/api/items/{sku}/deactivate']").exists())
                .andExpect(jsonPath("$.paths['/api/items/code-check']").exists())
                .andExpect(jsonPath("$.paths['/api/items/units']").exists())
                .andExpect(jsonPath("$.paths['/api/items/categories']").exists())
                .andExpect(jsonPath("$.paths['/api/items/categories/{categoryCode}/sub-categories']").exists())
                .andExpect(jsonPath("$.paths['/items']").doesNotExist())
                .andExpect(jsonPath("$.paths['/items/health']").doesNotExist());
    }

    @Test
    void exposesSwaggerUiBehindGatewayItemPrefix() throws Exception {
        mockMvc.perform(get("/items/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/items/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "\"configUrl\" : \"/api/items/v3/api-docs/swagger-config\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "https://petstore.swagger.io/v2/swagger.json"))));

        mockMvc.perform(get("/items/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/api/items/v3/api-docs"))
                .andExpect(jsonPath("$.configUrl").value("/api/items/v3/api-docs/swagger-config"));
    }
}
