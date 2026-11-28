package com.bibliomesh.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bibliomesh.auth.AuthDtos.AdminSessionResponse;
import com.bibliomesh.auth.AuthDtos.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "app.jwt.secret=test-secret-key-at-least-32-characters-long!",
    "app.auth.bootstrap-email=superadmin@bibliomesh.test",
    "app.auth.bootstrap-password=SuperSecureTestPass123!",
    "app.auth.bootstrap-name=Test Super Admin"
})
class AuthLoginIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @LocalServerPort
    private int port;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    @Test
    void loginWithBootstrappedSuperAdminReturnsJwtSession() {
        AdminSessionResponse session = client()
            .post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new LoginRequest("superadmin@bibliomesh.test", "SuperSecureTestPass123!"))
            .retrieve()
            .body(AdminSessionResponse.class);

        assertThat(session).isNotNull();
        assertThat(session.email()).isEqualTo("superadmin@bibliomesh.test");
        assertThat(session.fullName()).isEqualTo("Test Super Admin");
        assertThat(session.role()).isEqualTo("SUPER_ADMIN");
        assertThat(session.schoolId()).isNull();
        assertThat(session.token()).isNotBlank();
        assertThat(session.token().split("\\.")).hasSize(3);
    }

    @Test
    void loginWithWrongPasswordReturns401() {
        assertThatThrownBy(() ->
            client()
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("superadmin@bibliomesh.test", "WrongPassword!"))
                .retrieve()
                .toBodilessEntity()
        )
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void loginWithUnknownEmailReturns401() {
        assertThatThrownBy(() ->
            client()
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("nobody@bibliomesh.test", "SuperSecureTestPass123!"))
                .retrieve()
                .toBodilessEntity()
        )
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}