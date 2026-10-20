package com.wordrecall.dto;

public record LoginResponse(String token, UserDto user) {
}
