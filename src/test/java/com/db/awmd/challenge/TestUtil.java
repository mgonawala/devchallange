package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import java.math.BigDecimal;

public class TestUtil {

  public static Account getAccount(String accountId, BigDecimal amount){
    return new Account(accountId,amount);
  }

}
