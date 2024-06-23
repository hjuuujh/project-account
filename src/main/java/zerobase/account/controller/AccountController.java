package zerobase.account.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zerobase.account.aop.AccountLock;
import zerobase.account.dto.AccountInfo;
import zerobase.account.dto.CreateAccount;
import zerobase.account.dto.DeleteAccount;
import zerobase.account.dto.ErrorResponse;
import zerobase.account.exception.AccountException;
import zerobase.account.service.AccountService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AccountController {
    // 외부에서 컨트롤러로 접속 -> 컨트롤러는 서비스로 -> 서비스는 레포지토리로 접속 : layer 구조
    private final AccountService accountService;

    @PostMapping("/account")
    public CreateAccount.Response createAccount(@RequestBody @Valid CreateAccount.Request request) {

            return CreateAccount.Response.from(accountService.createAccount(request.getUserId(), request.getInitialBalance()));

    }

    @DeleteMapping("/account")
    public DeleteAccount.Response deleteAccount(@RequestBody @Valid DeleteAccount.Request request){
       return DeleteAccount.Response.from(accountService.deleteAccount(request.getUserId(), request.getAccountNumber()));
    }

    @GetMapping("/account")
    public List<AccountInfo> getAccountsByUserId(@RequestParam("user_id") Long userId){
        return accountService.getAccountsByUserId(userId)
                    .stream().map(accountDto ->
                            AccountInfo.builder()
                                    .accountNumber(accountDto.getAccountNumber())
                                    .balance(accountDto.getBalance())
                                    .build())
                    .collect(Collectors.toList());


    }


}
