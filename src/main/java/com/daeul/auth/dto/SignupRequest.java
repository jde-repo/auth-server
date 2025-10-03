package com.daeul.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupRequest {
    private String email;
    private String password;
}
