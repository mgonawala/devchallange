package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import java.math.BigDecimal;
import javax.security.auth.login.AccountNotFoundException;

public interface AccountsRepository {

  void createAccount(Account account) throws DuplicateAccountIdException;

  Account getAccount(String accountId) throws AccountNotFoundException;

  void clearAccounts();

  void addAmount(String accountId, BigDecimal amount) throws AccountNotFoundException;

  void withdrawAmount(String accountId, BigDecimal amount) throws AccountNotFoundException;

  BigDecimal totalBalance();
}
