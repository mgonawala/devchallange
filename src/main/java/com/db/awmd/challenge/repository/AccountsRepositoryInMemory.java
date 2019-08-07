package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.login.AccountNotFoundException;
import org.springframework.stereotype.Repository;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {


  private final Map<String, Account> accounts = new ConcurrentHashMap<>();

  @Override
  public void createAccount(Account account) throws DuplicateAccountIdException {
    Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
    if (previousAccount != null) {
      throw new DuplicateAccountIdException(
          "Account id " + account.getAccountId() + " already exists!");
    }
  }

  @Override
  public Account getAccount(String accountId) throws AccountNotFoundException {
    Account account = accounts.get(accountId);
    if (account == null) {
      throw new AccountNotFoundException("Account " + accountId + " does not exist.");
    }
    return new Account(account);
  }

  @Override
  public void clearAccounts() {
    accounts.clear();
  }

  @Override
  public void addAmount(String accountId, BigDecimal amount) throws AccountNotFoundException {

    synchronized (accountId) {
      Account account = this.getAccount(accountId);
      BigDecimal updatedToBalance = account.getBalance().add(amount);
      accounts.computeIfPresent(accountId, (id, act) -> {
        account.setBalance(updatedToBalance);
        return account;
      });
    }
  }

  @Override
  public void withdrawAmount(String accountId, BigDecimal amount) throws
      AccountNotFoundException, InsufficientBalanceException {

    synchronized (accountId) {
      Account account = this.getAccount(accountId);
      BigDecimal updatedBalance = account.getBalance().subtract(amount);
      if (updatedBalance.compareTo(BigDecimal.ZERO) < 0) {
        throw new InsufficientBalanceException("Insufficient Balance in account:" + accountId);
      }
      accounts.computeIfPresent(accountId, (id, act) -> {
        account.setBalance(updatedBalance);
        return account;
      });

    }

  }

  @Override
  public BigDecimal totalBalance() {

    BigDecimal sum = accounts.values().stream().map(account -> {
      return account.getBalance();
    }).reduce(BigDecimal.ZERO, BigDecimal::add);
    return sum;

  }

}
