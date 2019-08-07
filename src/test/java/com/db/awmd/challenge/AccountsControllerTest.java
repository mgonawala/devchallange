package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.NotificationService;
import com.db.awmd.challenge.web.AccountsController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class AccountsControllerTest {

  private MockMvc mockMvc;

  @MockBean
  private NotificationService notificationService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AccountsService accountsService;


  @Autowired
  private WebApplicationContext webApplicationContext;

  @Before
  public void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  public void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  public void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  public void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  public void transferMoney() throws Exception {
    Account fromAccount = TestUtil.getAccount("ID-123",new BigDecimal("123.45"));
    Account toAccount = TestUtil.getAccount("ID-456",new BigDecimal("123.45"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);
    Transfer transfer = new Transfer("ID-456",new BigDecimal("100"));
    try {
      this.mockMvc.perform(post("/v1/accounts/ID-123/transfer")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(transfer)))
          .andExpect(status().isOk());
    }
    catch (Exception ex){
      fail(ex.getMessage());
    }
  }

  @Test
  public void transferMoney_allowsZeroBalanceAfterTransfer() throws Exception {
    Account fromAccount = TestUtil.getAccount("ID-123",new BigDecimal("123.45"));
    Account toAccount = TestUtil.getAccount("ID-456",new BigDecimal("123.45"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);
    Transfer transfer = new Transfer("ID-456",new BigDecimal("123.45"));
    try {
      this.mockMvc.perform(post("/v1/accounts/ID-123/transfer")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(transfer)))
          .andExpect(status().isOk());
    }
    catch (Exception ex){
      fail(ex.getMessage());
    }
  }

  @Test
  public void transferMoney_failsOnInsufficientBalance() throws Exception {
    Account fromAccount = TestUtil.getAccount("ID-123",new BigDecimal("123.45"));
    Account toAccount = TestUtil.getAccount("ID-456",new BigDecimal("123.45"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);
    Transfer transfer = new Transfer("ID-456",new BigDecimal("200"));
      this.mockMvc.perform(post("/v1/accounts/ID-123/transfer")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(transfer)))
          .andExpect(status().isBadRequest())
          .andExpect(content().string("Insufficient Balance in account:ID-123"));

  }

  @Test
  public void transferMoney_failsOnAccountNotFound() throws Exception {
    Account fromAccount = TestUtil.getAccount("ID-123",new BigDecimal("123.45"));

    this.accountsService.createAccount(fromAccount);

    Transfer transfer = new Transfer("ID-456",new BigDecimal("200"));
    this.mockMvc.perform(post("/v1/accounts/ID-123/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(transfer)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Account ID-456 does not exist."));
  }

  @Test
  public void transferMoney_failsOnZeroAmount() throws Exception {
    Account fromAccount = TestUtil.getAccount("ID-123",new BigDecimal("123.45"));
    Account toAccount = TestUtil.getAccount("ID-456",new BigDecimal("123.45"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    Transfer transfer = new Transfer("ID-456",BigDecimal.ZERO);
    this.mockMvc.perform(post("/v1/accounts/ID-123/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(transfer)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Transfer amount must be greater than 0."));
  }
}
