package service.search;

import service.Command;
import util.exception.SearchException;

public class MyListCommand implements Command {
    private final SearchService searchService;

    public MyListCommand(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public void execute(String[] args) {
        try {
            searchService.showMyList(args);
        } catch (SearchException e) {
            System.out.println("[오류] " + e.getMessage());
        }
    }
}