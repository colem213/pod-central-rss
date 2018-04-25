package io.podcentral.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AppError {
  private String code;
  private String message;
}
