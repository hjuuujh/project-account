package zerobase.account.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zerobase.account.domain.Account;
import zerobase.account.domain.AccountUser;
import zerobase.account.domain.Transaction;
import zerobase.account.dto.TransactionDto;
import zerobase.account.exception.AccountException;
import zerobase.account.repository.AccountRepository;
import zerobase.account.repository.AccountUserRepository;
import zerobase.account.repository.TransactionRepository;
import zerobase.account.type.ErrorCode;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static zerobase.account.type.AccountStatus.IN_USE;
import static zerobase.account.type.AccountStatus.UNREGISTERED;
import static zerobase.account.type.TransactionResultType.F;
import static zerobase.account.type.TransactionResultType.S;
import static zerobase.account.type.TransactionType.CANCEL;
import static zerobase.account.type.TransactionType.USE;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 200L;
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    /**
     * 잔액 사용
     * 1. 성공
     * 2. 실패 - 사용자 없는 경우
     * 3. 실패 - 사용자 아이디와 계좌 소유주가 다른 경우
     * 4. 실패 - 계좌가 이미 해지 상태인 경우
     * 5. 실패 - 거래금액이 잔액보다 큰 경우
     * 6. 실패 - 거래금액이 너무 작거나 큰 경우
     */
    @Test
    @DisplayName("잔액 사용 - 성공")
    void successUseBalance(){
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi")
                .build();
        user.setId(10L);
        Account account = Account.builder()
                .accountUser(user)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(IN_USE)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactedAt(LocalDateTime.now())
                        .transactionId("transactionId")
                        .amount(1000L)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .balanceSnapshot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto = transactionService.useBalance(1L, "1234567890", USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 - 실패 - 사용자 없는 경우")
    void useBalanceFail_UserNotFound(){
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", USE_AMOUNT));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 실패 - 사용자 아이디와 계좌 소유주가 다른 경우")
    void useBalanceFail_userUnMatch(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(11L);
        AccountUser harry = AccountUser.builder()
                .name("Harry")
                .build();
        harry.setId(12L);
        Account account = Account.builder()
                .accountUser(harry)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(IN_USE)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", USE_AMOUNT));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 실패 - 계좌가 이미 해지 상태인 경우")
    void useBalanceFail_alreadyUnregistered(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(13L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(UNREGISTERED)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", USE_AMOUNT));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 - 실패 - 거래금액이 잔액보다 큰 경우")
    void useBalanceFail_exceedAmount(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(14L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1234567890")
                .balance(100L)
                .accountStatus(IN_USE)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 1000L));

        //then
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("잔액 사용 - 실패 - 거래금액이 너무 작거나 큰 경우")
    void useBalanceFail_invalidAmount(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1234567890")
                .balance(100L)
                .accountStatus(IN_USE)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        //when

        //then
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1234567890", 5L));
        assertEquals(ErrorCode.INVALID_AMOUNT, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    /**
     * 잔액 사용 취소
     * 1. 성공
     * 2. 실패 - 원거래 금액과 취소 금액이 다른 경우
     * 3. 실패 - 트랜잭션이 해당 계좌의 거래가 아닌 경우
     */
    @Test
    @DisplayName("잔액 사용 취소 - 성공")
    void successCancelBalance(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(16L);
//        given(accountUserRepository.findById(anyLong()))
//                .willReturn(Optional.of(pobi));
        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(IN_USE)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);


        //when
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId", "1234567890", CANCEL_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 취소 - 실패 - 원거래 금액과 취소 금액이 다른 경우")
    void cancelBalanceFail_cancelMustFully(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        pobi.setId(17L);
        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1234567890")
                .balance(10000L)
                .accountStatus(IN_USE)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT + 1000L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1234567890", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 - 실패 - 트랜잭션이 해당 계좌의 거래가 아닌 경우")
    void cancelBalanceFail_transactionAccountUnMatch(){
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(18L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        account.setId(1L);
        Account accountNotUse = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000013").build();
        accountNotUse.setId(2L);
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService
                        .cancelBalance(
                                "transactionId",
                                "1000000000",
                                CANCEL_AMOUNT
                        )
        );

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 - 실패 - 트랜잭션이 없는 경우")
    void cancelBalanceFail_transactionNotFound(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));
        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 - 실패 - 취소는 1년까지만 가능")
    void cancelBalanceFail_tooOldOrder(){
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi")
                .build();
        Account account = Account.builder()
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1234567890").build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactedAt(LocalDateTime.now().minusYears(1))
                .transactionResultType(S)
                .transactionId("transactionId")
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", CANCEL_AMOUNT));

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());

    }

    /**
     * 거래 확인
     * 1. 성공
     * 2. 실패 - 해당 transaction_id 없는 경우
     */
    @Test
    @DisplayName("거래 확인 - 성공")
    void successQueryTransaction(){
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1234567890").build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
//                .transactionResultType(S)
                .transactionResultType(F)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L).build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        //then
        assertEquals(USE, transactionDto.getTransactionType());
//        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(F, transactionDto.getTransactionResultType());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("거래호 확인 - 실패 - 해당 transaction_id 없는 경우")
    void queryTransactionFail_TransactionNotFound(){
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseTransaction() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        Account account = Account.builder()
                .accountUser(user)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResultType());
    }
}