package zerobase.account.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD) // 적용 위치 설정
@Retention(RetentionPolicy.RUNTIME) // 적용범위, 어떤 시점까지 사용될 지 결정하는 옵션
@Documented
@Inherited
public @interface AccountLock { // @interface : 사용자가 커스텀해 사용할 수 있는 어노테이션
    long tryLockTime() default 5000L;
}
