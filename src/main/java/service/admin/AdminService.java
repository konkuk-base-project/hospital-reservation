package service.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.exception.SearchException;
import util.file.FileUtil;

/**
 * 관리자 기능 (6.4)
 * - 회원 검색: user <아이디> resv
 * - 예약 현황: reserve-list <YYYY-MM-DD>
 *
 * 파일 포맷은 기획서 규격을 따른다.
 */
public class AdminService {

    // ========== 6.4.1 회원 검색 ==========
    public void searchUser(String[] args) throws SearchException {
        if (args.length < 1) {
            throw new SearchException("인자가 부족합니다. (형식: user <아이디> resv)");
        }
        if (args.length >= 2 && !args[1].equals("resv")) {
            throw new SearchException("알 수 없는 인자입니다. (사용 가능한 옵션: resv)");
        }

        final String loginId = args[0];
        final boolean withResv = args.length >= 2;

        final String plist = "data/patient/patientlist.txt";
        if (!FileUtil.resourceExists(plist)) {
            throw new SearchException("'/data/patient/patientlist.txt'이(가) 존재하지 않습니다.");
        }

        String patientNo = null;
        String name = null;
        String phone = null;

        try {
            List<String> lines = FileUtil.readLines(plist);
            for (int i = 1; i < lines.size(); i++) {
                String[] a = lines.get(i).trim().split("\\s+");
                if (a.length >= 5 && a[1].equals(loginId)) {
                    patientNo = a[0];
                    name = a[2];
                    phone = a[4];
                    break;
                }
            }
        } catch (IOException e) {
            throw new SearchException("회원 목록 조회 중 오류가 발생했습니다.");
        }

        if (patientNo == null) {
            throw new SearchException("존재하지 않는 회원 아이디입니다.");
        }

        System.out.println("======================================================================================");
        System.out.println("회원 정보");
        System.out.println("======================================================================================");
        System.out.println("아이디: " + loginId);
        System.out.println("이름: " + name);
        System.out.println("연락처: " + phone);

        if (!withResv) {
            System.out
                    .println("======================================================================================");
            return;
        }

        String pfile = "data/patient/" + patientNo + ".txt";
        if (!FileUtil.resourceExists(pfile)) {
            System.out.println("예약 내역: (없음)");
            System.out
                    .println("======================================================================================");
            return;
        }

        System.out.println("예약 내역:");
        try {
            List<String> lines = FileUtil.readLines(pfile);
            for (int i = 4; i <= lines.size(); i++) {
                String l = lines.get(i - 1).trim();
                if (l.isEmpty())
                    continue;
                String[] a = l.split("\\s+");
                if (a.length >= 7) {
                    String rno = a[0];
                    String date = a[1];
                    String st = a[2];
                    String en = a[3];
                    String dept = a[4];
                    String docNo = a[5];
                    String status = statusText(a[6]);
                    String docName = lookupDoctorName(docNo);
                    String deptName = deptKorean(dept);
                    System.out.printf("- %s | %s | %s-%s | %s | %s | [%s]%n",
                            rno, date, st, en, deptName, (docName == null ? docNo : docName), status);
                }
            }
        } catch (IOException e) {
            throw new SearchException("예약 내역을 읽는 중 오류가 발생했습니다.");
        }
        System.out.println("======================================================================================");
    }

    // ========== 6.4.2 예약 현황 ==========
    public void showReserveList(String[] args) throws SearchException {
        if (args.length != 1) {
            throw new SearchException("인자가 부족합니다. (형식: reserve-list YYYY-MM-DD)");
        }
        String date = args[0];
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new SearchException("날짜 형식이 잘못되었습니다. (예: 2025-10-10)");
        }

        String ymd = date.replace("-", "");
        String apFile = "data/appointment/" + ymd + ".txt";
        if (!FileUtil.resourceExists(apFile)) {
            System.out.println("(예약 없음).");
            return;
        }

        try {
            List<String> lines = FileUtil.readLines(apFile);
            if (lines.size() < 2) {
                throw new SearchException("파일 형식이 올바르지 않습니다.");
            }
            String header = lines.get(1).trim(); // TIME D00001 D00002 ...
            String[] head = header.split("\\s+");
            if (head.length < 2 || !head[0].equals("TIME")) {
                throw new SearchException("파일 형식이 올바르지 않습니다.");
            }
            List<String> doctorNos = new ArrayList<>();
            for (int i = 1; i < head.length; i++)
                doctorNos.add(head[i]);

            Map<String, String> doctorNameMap = loadDoctorNames();
            Map<String, String> doctorDeptMap = loadDoctorDeptCodes();

            List<String> out = new ArrayList<>();
            for (int li = 2; li < lines.size(); li++) {
                String row = lines.get(li).trim();
                if (row.isEmpty())
                    continue;
                String[] cols = row.split("\\s+");
                if (cols.length != (1 + doctorNos.size()))
                    continue;

                String time = cols[0];
                for (int i = 0; i < doctorNos.size(); i++) {
                    String cell = cols[i + 1];
                    if (cell.equals("0") || cell.equalsIgnoreCase("X"))
                        continue;

                    String rno = cell;
                    String docNo = doctorNos.get(i);
                    String docName = doctorNameMap.getOrDefault(docNo, docNo);
                    String deptCode = doctorDeptMap.getOrDefault(docNo, "");
                    String deptName = deptKorean(deptCode);
                    String userId = findUserIdByReservation(rno);
                    String timeEnd = plus10(time);

                    out.add(String.format("%s | %s-%s | %s | %s | %s | [예약중]",
                            rno, time, timeEnd, deptName, docName, (userId == null ? "-" : userId)));
                }
            }

            System.out
                    .println("======================================================================================");
            System.out.printf("%s 예약 현황 (총 %d건)%n", date, out.size());
            System.out
                    .println("======================================================================================");
            if (out.isEmpty())
                System.out.println("(예약 없음)");
            else
                out.forEach(System.out::println);

        } catch (IOException e) {
            throw new SearchException("예약 현황 확인 중 오류가 발생했습니다.");
        }
    }

    // ======= 유틸들 =======

    private String statusText(String code) {
        return switch (code) {
            case "0" -> "예약가능";
            case "1" -> "예약중";
            case "2" -> "진료완료";
            case "3" -> "취소";
            case "4" -> "미방문";
            default -> "알수없음";
        };
    }

    private String deptKorean(String code) {
        return switch (code) {
            case "IM" -> "내과";
            case "GS" -> "일반외과";
            case "OB" -> "산부인과";
            case "PED" -> "소아과";
            case "PSY" -> "정신과";
            case "DERM" -> "피부과";
            case "ENT" -> "이비인후과";
            case "ORTH" -> "정형외과";
            default -> code;
        };
    }

    private String plus10(String hhmm) {
        String[] s = hhmm.split(":");
        int h = Integer.parseInt(s[0]);
        int m = Integer.parseInt(s[1]) + 10;
        if (m >= 60) {
            h++;
            m -= 60;
        }
        return String.format("%02d:%02d", h, m);
    }

    private Map<String, String> loadDoctorNames() throws IOException {
        Map<String, String> map = new HashMap<>();
        List<String> lines = FileUtil.readLines("data/doctor/doctorlist.txt");
        for (int i = 1; i < lines.size(); i++) {
            String[] a = lines.get(i).trim().split("\\s+");
            if (a.length >= 3)
                map.put(a[0], a[1]); // D번호 -> 이름
        }
        return map;
    }

    private Map<String, String> loadDoctorDeptCodes() throws IOException {
        Map<String, String> map = new HashMap<>();
        List<String> lines = FileUtil.readLines("data/doctor/doctorlist.txt");
        for (int i = 1; i < lines.size(); i++) {
            String[] a = lines.get(i).trim().split("\\s+");
            if (a.length >= 3)
                map.put(a[0], a[2]); // D번호 -> 진료과코드
        }
        return map;
    }

    // 예약번호 -> 회원 아이디 찾기 (모든 P*.txt 스캔)
    private String findUserIdByReservation(String rno) throws IOException {
        Map<String, String> pnoToId = new HashMap<>();
        List<String> plist = FileUtil.readLines("data/patient/patientlist.txt");
        for (int i = 1; i < plist.size(); i++) {
            String[] a = plist.get(i).trim().split("\\s+");
            if (a.length >= 2)
                pnoToId.put(a[0], a[1]); // P000001 -> hong123
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(FileUtil.getResourcePath("data/patient"), "P*.txt")) {
            for (Path p : ds) {
                String pno = p.getFileName().toString().replace(".txt", "");
                try (BufferedReader br = Files.newBufferedReader(p)) {
                    String l;
                    int lineNo = 0;
                    while ((l = br.readLine()) != null) {
                        lineNo++;
                        if (lineNo <= 3)
                            continue;
                        l = l.trim();
                        if (l.isEmpty())
                            continue;
                        String[] a = l.split("\\s+");
                        if (a.length >= 1 && a[0].equals(rno)) {
                            return pnoToId.getOrDefault(pno, null);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 의사번호(D00001 등)로 의사 이름을 조회
     */
    private String lookupDoctorName(String docNo) {
        try {
            Map<String, String> doctorNames = loadDoctorNames();
            return doctorNames.getOrDefault(docNo, null);
        } catch (IOException e) {
            return null;
        }
    }
}
