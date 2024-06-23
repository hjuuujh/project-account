package zerobase.account.dto;

import lombok.*;
import zerobase.account.exception.AccountException;
import zerobase.account.type.ErrorCode;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ErrorResponse {
    private ErrorCode errorCode;
    private String errorMessage;

    public static ErrorResponse from(AccountException e) {
        return ErrorResponse.builder()
                .errorMessage(e.getErrorMessage())
                .errorCode(e.getErrorCode())
                .build();
    }
}
