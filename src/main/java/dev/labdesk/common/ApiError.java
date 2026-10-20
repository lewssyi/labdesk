package dev.labdesk.common;

/** 业务异常，失败时 @Transactional 默认回滚。 */
public class ApiError extends RuntimeException {
    private final int status;

    public ApiError(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static void require(boolean condition, int status, String message) {
        if (!condition) throw new ApiError(status, message);
    }
}
