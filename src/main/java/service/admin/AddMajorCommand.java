package service.admin;

import repository.MajorRepository;
import service.AuthContext;
import service.Command;

public class AddMajorCommand implements Command {
    private final MajorRepository majorRepository;
    private final AuthContext authContext;

    public AddMajorCommand(MajorRepository majorRepository, AuthContext authContext) {
        this.majorRepository = majorRepository;
        this.authContext = authContext;
    }

    @Override
    public void execute(String[] args) {
        if (!authContext.getPrompt().equals("Admin")) {
            System.out.println("[오류] 관리자만 사용할 수 있는 명령어입니다.");
            return;
        }

        if (args.length != 2) {
            System.out.println("[오류] 인자의 개수가 올바르지 않습니다. (형식: add-major <진료과코드> <진료과명>)");
            return;
        }

        String code = args[0];
        String name = args[1];

        if (!code.matches("^[A-Z]{2,4}$")) {
            System.out.println("[오류] 진료과 코드는 대문자 로마자 2~4자로 구성되어야 합니다. (예: IM, GS, PED, DERM)");
            return;
        }

        if (majorRepository.isMajorExists(code)) {
            System.out.println("[오류] 이미 존재하는 진료과 코드입니다. (형식: add-major <진료과코드> <진료과명>)");
            return;
        }

        try {
            majorRepository.save(new model.Major(code, name));
            System.out.printf("진료과가 추가되었습니다. [코드: %s, 이름: %s]%n", code, name);
        } catch (java.io.IOException e) {
            System.out.println("[오류] 진료과 추가 중 문제가 발생했습니다.");
        }
    }
}
