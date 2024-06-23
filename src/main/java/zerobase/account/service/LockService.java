package zerobase.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import zerobase.account.exception.AccountException;
import zerobase.account.type.ErrorCode;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LockService {
    private final RedissonClient redissonClient; // 이름이 같으면 Bean 자동 주입

    public void lock(String accountNumber) {
        // 계좌번호 자체를 lock key로 사용
        RLock lock = redissonClient.getLock(getLockKey(accountNumber));
        log.debug("Trying lock for accountNumber : {}", accountNumber);

        try {
            // 5초동안 안무거도안하면 lock 잃음
            // 1초 기다리는 동안 lock 풀리지않으면 취득 못함
            boolean isLock = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLock) {
                log.error("===Lock acquisition failed===");
                throw new AccountException(ErrorCode.ACCOUNT_TRANSACTION_LOCK);
            }
        }catch (AccountException e){
            throw e;

        }catch (Exception e) {
            log.error("Redis lock failed", e);
        }


    }

    public void unlock(String accountNumber) {
        log.debug("Un lock for accountNumber : {}", accountNumber);
        redissonClient.getLock(getLockKey(accountNumber)).unlock();

    }

    private static String getLockKey(String accountNumber) {
        return "ACLK" + accountNumber;
    }
}
