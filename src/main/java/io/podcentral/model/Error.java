package io.podcentral.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Error {
  private String code;
  private String message;
}
