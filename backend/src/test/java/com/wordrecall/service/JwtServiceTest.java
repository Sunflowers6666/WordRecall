package com.wordrecall.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void createsAndParsesToken() {
        JwtService jwtService = new JwtService(SECRET, 7);
        String token = jwtService.createToken(42L);

        assertThat(token).isNotBlank();
        assertThat(jwtService.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService("short", 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwtService = new JwtService(SECRET, 7);
        String token = jwtService.createToken(1L);

        assertThatThrownBy(() -> jwtService.parseUserId(token + "x"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, -1);
        String token = jwtService.createToken(1L);

        assertThatThrownBy(() -> jwtService.parseUserId(token))
                .isInstanceOf(Exception.class);
    }
}
