package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.AccountNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ThreadSafeTest {

  private static final int  NUM_THREADS = 20;
  private static final int  NUM_ACCOUNTS = 100;
  private static final int  NUM_ITERATIONS = 100;

  @Autowired
  private AccountsService accountsService;

  @MockBean
  private NotificationService notificationService;

  @Before
  public void setup(){
    this.accountsService.getAccountsRepository().clearAccounts();
  }
  @Test
  public void totalBalanceShouldRemainSame_withMultipleThreadOperations() throws InterruptedException {

    final Random random = new Random();
    //final Account[] accounts = new Account[NUM_ACCOUNTS];

    for( int i = 0 ; i<NUM_ACCOUNTS ; i++){
      String accountId = String.valueOf(i).intern();
      Account account = new Account(accountId,new BigDecimal(100));
      accountsService.createAccount(account);
    }

    class ThreadSafe implements Runnable{

      @Override
      public void run() {
        for(int i=0; i<NUM_ITERATIONS; i++){
          String fromId = String.valueOf(random.nextInt(NUM_ACCOUNTS)).intern();
          String toId = String.valueOf(random.nextInt(NUM_ACCOUNTS)).intern();
          BigDecimal amount = new BigDecimal(random.nextInt(100));
          try {
            accountsService.transferAmount(fromId,toId,amount);
          } catch (AccountNotFoundException e) {
            e.printStackTrace();
          }
        }
      }
    }
    ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
    for(int i=0; i<NUM_THREADS ; i ++){
      executorService.execute(new ThreadSafe());
    }

    executorService.shutdown();
    executorService.awaitTermination(50, TimeUnit.SECONDS);
    Assert.assertEquals(this.accountsService.totalBalance(),new BigDecimal(10000));
  }
}
