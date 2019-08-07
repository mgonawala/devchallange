# DevChallenge

Addition of Money Transfer API.

### Transfer Amount

API is capable of transferring amount from one Account to Another account.

It allows only positive amount to be transferred.

It checks for Sufficient balance before making a transfer.

The service is made thread-safe and tries to achieve consistent state all over.
 

### Enhancements


New APIs to deposit & withdraw money from respective Account can be added.

It could  also have a transaction resource. which will enable to extract
Account statements & carry debit or credit transactions.

There might also be a new feature added for Current account creation along with Savings
Account.

Given more time, centralized Exception management should be achieved.

It is also important to have integration tests in place before it moves to production.
Which would require implementing all the pending services.

Jacoco & Sonar should be added in the build step.

CI/CD can also be achieved for seamless development.

