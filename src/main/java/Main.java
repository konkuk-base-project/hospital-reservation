import util.validation.FileExistValidator;

public class Main {

    public static void main(String[] args) {
        System.out.println("프로그램을 시작합니다...");

        FileExistValidator validator = new FileExistValidator();
        validator.validate();

        System.out.println("병원 예약 프로그램을 시작합니다.");
    }
}