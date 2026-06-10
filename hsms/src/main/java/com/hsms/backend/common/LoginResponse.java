package com.hsms.backend.common;

public record LoginResponse(String token, HsmsUserDto user) {
    }
