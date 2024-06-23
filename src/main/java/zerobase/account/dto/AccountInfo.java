package zerobase.account.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountInfo {
    // AccountDto : Controller <-> Service
    // AccountInfo : Client <-> Controller(Application)

    private String accountNumber;
    private Long balance;


}