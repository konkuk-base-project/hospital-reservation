import java.util.Scanner;

import service.CommandHandler;
import util.validation.FileExistValidator;
import util.validation.FileFormatValidator;

public class Main {

    public static void main(String[] args) {
        System.out.println("프로그램을 시작합니다...");

        FileExistValidator fileExistValidator = new FileExistValidator();
        fileExistValidator.validate();

        FileFormatValidator fileFormatValidator = new FileFormatValidator();
        fileFormatValidator.validate();

        CommandHandler commandHandler = new CommandHandler();

        System.out.println("시스템 초기화 완료.");
        System.out.println("======================================================================================");
        System.out.println("대학병원 예약 시스템 v1.0");
        System.out.println("======================================================================================");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print(commandHandler.getPrompt() + " > ");
            String input = scanner.nextLine();

            boolean isExit = commandHandler.handle(input);

            if (isExit) {
                break;
            }
        }
        scanner.close();
    }
}