package com.hokori.web.dto;

import lombok.Data;

@Data
public class UserPreferencesRequest {
    
    private String currentJlptLevel; // N5, N4, N3, N2, N1
}
