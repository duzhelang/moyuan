package com.moyuan.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String bio;
    private Integer gender;
    private LocalDate birthday;
}
