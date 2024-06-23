package zerobase.account.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import zerobase.account.type.AccountStatus;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Account extends BaseEntity {

    @ManyToOne
    private AccountUser accountUser;

    private String accountNumber;
    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    @Enumerated(EnumType.STRING) // 없으면 0,1,2 값으로 저장되어 실제 값을 알기 어려움
    private AccountStatus accountStatus;

    public void useBalance(Long amount) {
        balance -= amount;
    }

    public void cancelBalance(Long amount) {
        balance += amount;
    }
}
