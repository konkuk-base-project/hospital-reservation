package service.search;

import service.Command;
import util.exception.SearchException;

public class DeptCommand implements Command {
    private final SearchService searchService;

    public DeptCommand(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void execute(String[] args) {
        try {
            searchService.searchByDepartment(args);
        } catch (SearchException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}