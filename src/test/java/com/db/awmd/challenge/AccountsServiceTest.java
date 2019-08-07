package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import java.math.BigDecimal;
import javax.security.auth.login.AccountNotFoundException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @MockBean
  private NotificationService notificationService;


  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  public void transferMoney(){

    Account a1 = TestUtil.getAccount("ID-123",new BigDecimal("123"));
    Account a2 = TestUtil.getAccount("ID-456",new BigDecimal("240"));

    Mockito.doNothing()
        .when(notificationService)
          .notifyAboutTransfer(Mockito.any(Account.class),Mockito.anyString());

    this.accountsService.createAccount(a1);
    this.accountsService.createAccount(a2);
    try {
      this.accountsService.transferAmount("ID-123","ID-456",new BigDecimal(100));
      Account result = this.accountsService.getAccount("ID-123");
      Account toAccountResult = this.accountsService.getAccount("ID-456");

      Assert.assertEquals(result.getBalance()
          .compareTo(new BigDecimal(23)),0);

      Assert.assertEquals(toAccountResult.getBalance()
          .compareTo(new BigDecimal(340)),0);

      Mockito.verify(notificationService, Mockito.times(2))
          .notifyAboutTransfer(Mockito.any(Account.class),Mockito.anyString());
    } catch (AccountNotFoundException e) {
      fail("It should have transferred the money from one account to another.");
    }
  }

  @Test
  public void transferMoney_allowsZeroBalanceAfterTransfer(){

    Account a1 = TestUtil.getAccount("ID-123",new BigDecimal("123"));
    Account a2 = TestUtil.getAccount("ID-456",new BigDecimal("240"));
    Mockito.doNothing()
        .when(notificationService)
        .notifyAboutTransfer(Mockito.any(Account.class),Mockito.anyString());

    this.accountsService.createAccount(a1);
    this.accountsService.createAccount(a2);
    try {
      this.accountsService.transferAmount("ID-123","ID-456",new BigDecimal("123"));
      Account result = this.accountsService.getAccount("ID-123");
      Assert.assertEquals(result.getBalance()
          .compareTo(BigDecimal.ZERO),0);

      Mockito.verify(notificationService, Mockito.times(2))
          .notifyAboutTransfer(Mockito.any(Account.class),Mockito.anyString());

    } catch (AccountNotFoundException e) {
      fail("It should have allowed to transfer all the amount from the account.");
    }
  }

  @Test
  public void transferMoney_InsufficientBalance(){

    Account a1 = TestUtil.getAccount("ID-123",new BigDecimal("123"));
    Account a2 = TestUtil.getAccount("ID-456",new BigDecimal("240"));
    this.accountsService.createAccount(a1);
    this.accountsService.createAccount(a2);
    try {
      this.accountsService.transferAmount("ID-123","ID-456",new BigDecimal(200));
      Account result = this.accountsService.getAccount("ID-123");
      Assert.assertEquals(result.getBalance()
          .compareTo(new BigDecimal(23)),0);
      fail("Should have not allowed to transfer more than account's current balance");
    } catch (Exception ex) {
      assertThat(ex.getMessage()).isEqualTo("Insufficient Balance in account:ID-123");
    }
  }

  @Test
  public void transferMoney_AccountNotFound(){
    try {
      this.accountsService.transferAmount("ID-123","ID-456",new BigDecimal(200));
      Account result = this.accountsService.getAccount("ID-123");
      Assert.assertEquals(result.getBalance()
          .compareTo(new BigDecimal(23)),0);
      fail("Should have failed with AccountNotFoundException");
    } catch (Exception ex) {
      assertThat(ex.getMessage()).isEqualTo("Account ID-123 does not exist.");
    }
  }

  @Test
  public void transferMoney_failsOnZeroAmount(){
    try {
      this.accountsService.transferAmount("ID-123","ID-456",BigDecimal.ZERO);
      Account result = this.accountsService.getAccount("ID-123");
      Assert.assertEquals(result.getBalance()
          .compareTo(new BigDecimal(23)),0);
      fail("Should have not allowed to transfer zero amount.");
    } catch (Exception ex) {
      assertThat(ex.getMessage()).isEqualTo("Transfer amount must be greater than 0.");
    }
  }


  @Before
  public void clearAll(){
    accountsService.getAccountsRepository().clearAccounts();
  }
}
