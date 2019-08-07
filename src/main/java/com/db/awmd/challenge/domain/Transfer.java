package com.db.awmd.challenge.domain;

import java.math.BigDecimal;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Transfer {

  @NotNull(message = "To Account can not be null.")
  @NotEmpty( message = "To Account can not be empty.")
  private String toAccountId;

  @Min(value = 0, message = "Amount should be greater than 0")
  private BigDecimal amount;

}
