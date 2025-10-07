package util.exception;

public class AppointmentFileException extends Exception {

 
    public enum ErrorType {
        FILE_NOT_FOUND("예약 파일이 존재하지 않습니다"),
        INVALID_DATE_FORMAT("날짜 형식이 올바르지 않습니다 (YYYY-MM-DD 형식)"),
        INVALID_DOCTOR_LIST("의사 번호 리스트 형식이 올바르지 않습니다"),
        INVALID_TIME_SLOT("시간 슬롯 형식이 올바르지 않습니다"),
        INVALID_APPOINTMENT_STATUS("예약 상태 값이 올바르지 않습니다 (0, 예약번호, X)"),
        COLUMN_MISMATCH("의사 수와 예약 상태 열 수가 일치하지 않습니다"),
        MISSING_REQUIRED_LINE("필수 라인이 누락되었습니다"),
        FILE_READ_ERROR("파일 읽기 중 오류가 발생했습니다"),
        FILE_WRITE_ERROR("파일 쓰기 중 오류가 발생했습니다"),
        INVALID_FILE_STRUCTURE("파일 구조가 올바르지 않습니다");

        private final String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final ErrorType errorType;
    private final String detail;
    private final int lineNumber;

    public AppointmentFileException(ErrorType errorType) {
        this(errorType, null, -1);
    }

    public AppointmentFileException(ErrorType errorType, String detail) {
        this(errorType, detail, -1);
    }

    public AppointmentFileException(ErrorType errorType, String detail, int lineNumber) {
        super(buildMessage(errorType, detail, lineNumber));
        this.errorType = errorType;
        this.detail = detail;
        this.lineNumber = lineNumber;
    }

    /**
     * 원인 예외를 포함한 생성자
     */
    public AppointmentFileException(ErrorType errorType, String detail, Throwable cause) {
        super(buildMessage(errorType, detail, -1), cause);
        this.errorType = errorType;
        this.detail = detail;
        this.lineNumber = -1;
    }

    private static String buildMessage(ErrorType errorType, String detail, int lineNumber) {
        StringBuilder sb = new StringBuilder(errorType.getMessage());

        if (lineNumber > 0) {
            sb.append(" (라인 ").append(lineNumber).append(")");
        }

        if (detail != null && !detail.isEmpty()) {
            sb.append(": ").append(detail);
        }

        return sb.toString();
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getDetail() {
        return detail;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
