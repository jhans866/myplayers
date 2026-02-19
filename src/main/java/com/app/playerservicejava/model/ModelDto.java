package com.app.playerservicejava.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelDto {
    private String name;
    private Long size;
    private OffsetDateTime modifiedAt;  // ✅ Match actual return type
}
