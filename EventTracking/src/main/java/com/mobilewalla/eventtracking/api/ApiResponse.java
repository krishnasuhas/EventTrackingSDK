package com.mobilewalla.eventtracking.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiResponse implements Serializable {
    private String token;
    private String message;
}
