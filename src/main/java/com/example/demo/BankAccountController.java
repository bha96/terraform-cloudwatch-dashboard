package com.example.demo;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import static java.math.BigDecimal.valueOf;
import java.math.BigDecimal;
import static java.util.Optional.ofNullable;
@Slf4j
@RestController
public class BankAccountController implements ApplicationListener<ApplicationReadyEvent> {

    private Map<String, Account> theBank = new HashMap();

    private MeterRegistry meterRegistry;

    @Autowired
    public BankAccountController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Transfers money from one account to another. Creates accounts at both ends if they do not exist.
     *
     * @param tx          a transaction object
     * @param fromAccount from account id
     * @param toAccount   to account id
     */
    @PostMapping(path = "/account/{fromAccount}/transfer/{toAccount}", consumes = "application/json", produces = "application/json")
    @Timed
    public void transfer(@RequestBody Transaction tx, @PathVariable String fromAccount, @PathVariable String toAccount) {
        // Increment a metric called "transfer" every time this is called, and tag with from- and to country
        meterRegistry.counter("transfer",
                "from", tx.getFromCountry(), "to", tx.getToCountry()).increment();
        Account from = getOrCreateAccount(fromAccount);
        Account to = getOrCreateAccount(toAccount);
        from.setBalance(from.getBalance().subtract(valueOf(tx.getAmount())));
        to.setBalance(to.getBalance().add(valueOf(tx.getAmount())));
    }

    /**
     * Saves an account. Will create a new account if one does not exist.
     *
     * @param a the account Object
     * @return
     */
    @PostMapping(path = "/account", consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<Account> updateAccount(@RequestBody Account a) {
        meterRegistry.counter("update_account").increment();
        if(theBank.get(a.getId()) != null){
            log.info("Someone tried creating a user with id: " + a.getId() + " but that already exists");
        }
        Account account = getOrCreateAccount(a.getId());
        account.setBalance(a.getBalance());
        account.setCurrency(a.getCurrency());
        
        theBank.put(a.getId(), a);
        log.info("Money in the bank: {}",theBank.values().stream().map(ac -> ac.getBalance()).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
        return new ResponseEntity<>(a, HttpStatus.OK);
    }
    
    /**
     * Gets all accounts
     * @return
     */
     
    @GetMapping(path = "/account", consumes = "application/json",
            produces = "application/json")
    public ResponseEntity<ArrayList<Account>> getAllAccounts(){
        meterRegistry.counter("get_all_accounts").increment();
        var list = new ArrayList<Account>();
        for (Account account : theBank.values()) {
            list.add(account);
        }
        
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    /**
     * Gets account info for an account
     *
     * @param accountId the account ID to get info from
     * @return
     */
    @Timed
    @GetMapping(path = "/account/{accountId}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Account> balance(@PathVariable String accountId) {
        meterRegistry.counter("balance").increment();
        Account account = ofNullable(theBank.get(accountId)).orElseThrow(AccountNotFoundException::new);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    private Account getOrCreateAccount(String accountId) {
        if (theBank.get(accountId) == null) {
            Account a = new Account();
            a.setId(accountId);
            theBank.put(accountId, a);
        }
        return theBank.get(accountId);
    }


    /**
     * Denne meter-typen "Gauge" rapporterer en verdi hver gang noen kaller "size" metoden på
     * Verdisettet til HashMap
     *
     * @param applicationReadyEvent
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        Gauge.builder("account_count", theBank, b -> b.values().size()).register(meterRegistry);
        Gauge.builder("total_money", theBank, b ->  b.values().stream().map(account -> account.getBalance()).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue()).register(meterRegistry);
    }

    @ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "account not found")
    public static class AccountNotFoundException extends RuntimeException {
    }
}