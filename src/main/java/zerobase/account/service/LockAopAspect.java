package zerobase.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import zerobase.account.aop.AccountLockIdInterface;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {

    private final LockService lockService;

    /**
     * @Before : 메소드가 실행되기 이전에 실행
     * @After : 베스드의 종료 후 무조건 실행 try-catch 의 finally 같이
     * @After-returning : 메소드가 성공적으로 완료되고 리턴한 다음 실행
     * @After-throwing : 메소드 실행중 예외가 발생하면 실행 try-catch 의 catch 같이
     * @Around : 메소드 호출 자체를 가로채서 메소드 실행 전후에 처리할 로직 삽입
     */
    @Around("@annotation(zerobase.account.aop.AccountLock) && args(request)")
    public Object aroundMethod(ProceedingJoinPoint joinPoint,
                               AccountLockIdInterface request) throws Throwable{
        // lock 취득 시도
        lockService.lock(request.getAccountNumber());
        try {

            return joinPoint.proceed();
        }finally {
// lock 해제
            lockService.unlock(request.getAccountNumber());
        }
    }
}
