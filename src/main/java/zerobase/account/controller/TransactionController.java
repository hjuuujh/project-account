package zerobase.account.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zerobase.account.aop.AccountLock;
import zerobase.account.dto.CancelBalance;
import zerobase.account.dto.ErrorResponse;
import zerobase.account.dto.QueryTransactionResponse;
import zerobase.account.dto.UseBalance;
import zerobase.account.exception.AccountException;
import zerobase.account.service.TransactionService;

import javax.validation.Valid;
import java.nio.channels.InterruptedByTimeoutException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(@Valid @RequestBody UseBalance.Request request)
            throws InterruptedByTimeoutException {
        try {
            Thread.sleep(3000L);
            return UseBalance.Response.from(
                    transactionService.useBalance(request.getUserId(),
                            request.getAccountNumber(), request.getAmount())
            );
        } catch (AccountException e) {
            log.error("Failed to use balance. ");

            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );
            throw e;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(@Valid @RequestBody CancelBalance.Request request) {
        {
            try {
                return CancelBalance.Response.from(
                        transactionService.cancelBalance(request.getTransactionId(),
                                request.getAccountNumber(), request.getAmount())
                );
            } catch (AccountException e) {
                log.error("Failed to use balance. ");

                transactionService.saveFailedCancelTransaction(
                        request.getAccountNumber(),
                        request.getAmount()
                );

                throw e;
            }
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransactions(@PathVariable String transactionId) {

        return QueryTransactionResponse.from(transactionService.queryTransaction(transactionId));

    }
}
