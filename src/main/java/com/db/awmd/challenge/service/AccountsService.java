package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.repository.AccountsRepository;
import java.math.BigDecimal;
import javax.security.auth.login.AccountNotFoundException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountsService {

  private final Object lock = new Object();

  @Getter
  private final AccountsRepository accountsRepository;

  @Getter
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository,
      NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) throws AccountNotFoundException {
    return this.accountsRepository.getAccount(accountId);
  }

  public BigDecimal totalBalance() {

    return this.accountsRepository.totalBalance();
  }

  public void transferAmount(String fromId, String toId, BigDecimal amount)
      throws AccountNotFoundException {
    if (amount.equals(BigDecimal.ZERO)) {
      throw new InvalidAmountException("Transfer amount must be greater than 0.");
    }
    int fromHash = System.identityHashCode(fromId);
    int toHash = System.identityHashCode(toId);
    if (fromHash < toHash) {
      synchronized (fromId) {
        synchronized (toId) {
          doTransfer(fromId, toId, amount);
        }
      }
    } else if (toHash < fromHash) {
      synchronized (toId) {
        synchronized (fromId) {
          doTransfer(fromId, toId, amount);
        }
      }
    } else {
      synchronized (lock) {
        synchronized (fromId) {
          synchronized (toId) {
            doTransfer(fromId, toId, amount);
          }
        }
      }
    }
  }

  private void doTransfer(String fromId, String toId, BigDecimal amount)
      throws AccountNotFoundException, InsufficientBalanceException {

    Account fromAccount = accountsRepository.getAccount(fromId);
    Account toAccount = accountsRepository.getAccount(toId);
    try {
      accountsRepository.addAmount(toId, amount);
      accountsRepository.withdrawAmount(fromId, amount);
    } catch (InsufficientBalanceException ex) {
      accountsRepository.withdrawAmount(toId, amount);
      throw new InsufficientBalanceException("Insufficient Balance in account:" + fromId);
    }
    this.notificationService
        .notifyAboutTransfer(fromAccount, "Amount " + amount + " credited to Account " + toId);
    this.notificationService
        .notifyAboutTransfer(toAccount, "Amount " + amount + " credited from Account " + fromId);
  }


}
