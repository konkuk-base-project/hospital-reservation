package service.search;

import service.Command;
import util.exception.SearchException;

public class DoctorCommand implements Command {
    private final SearchService searchService;

    public DoctorCommand(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void execute(String[] args) {
        try {
            searchService.searchByDoctor(args);
        } catch (SearchException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}