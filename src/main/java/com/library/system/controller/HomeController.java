package com.library.system.controller;

import com.library.system.domain.Book;
import com.library.system.domain.BookStatus;
import com.library.system.domain.User;
import com.library.system.service.BookService;
import com.library.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/")
    public String home(@RequestParam(required = false) String search, Model model) {
        List<Book> books;
        if (search != null && !search.trim().isEmpty()) {
            books = bookService.searchBooks(search);
            model.addAttribute("searchTerm", search);
        } else {
            books = bookService.getAllBooks();
        }
        
        // Get current user if authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            userService.getUserByUsername(authentication.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        
        model.addAttribute("books", books);
        return "index";
    }
    
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q, Model model) {
        List<Book> books;
        if (q != null && !q.trim().isEmpty()) {
            books = bookService.searchBooks(q);
            model.addAttribute("searchTerm", q);
        } else {
            books = bookService.getAllBooks();
        }

        // Get current user if authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            userService.getUserByUsername(authentication.getName()).ifPresent(user -> model.addAttribute("user", user));
        }

        model.addAttribute("books", books);
        return "search";
    }

    @GetMapping("/advanced-search")
    public String advancedSearch(@RequestParam(required = false) String title,
                                  @RequestParam(required = false) String author,
                                  @RequestParam(required = false) String isbn,
                                  @RequestParam(required = false) String publisher,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Integer fromYear,
                                  @RequestParam(required = false) Integer toYear,
                                  Model model) {
        // Get current user if authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            userService.getUserByUsername(authentication.getName()).ifPresent(user -> model.addAttribute("user", user));
        }

        // Prepare search parameters
        String titleParam = (title != null && !title.trim().isEmpty()) ? title.trim() : null;
        String authorParam = (author != null && !author.trim().isEmpty()) ? author.trim() : null;
        String isbnParam = (isbn != null && !isbn.trim().isEmpty()) ? isbn.trim() : null;
        String publisherParam = (publisher != null && !publisher.trim().isEmpty()) ? publisher.trim() : null;
        BookStatus statusParam = (status != null && !status.trim().isEmpty()) ? BookStatus.valueOf(status) : null;

        // If at least one search parameter is provided, perform search
        if (titleParam != null || authorParam != null || isbnParam != null ||
            publisherParam != null || statusParam != null || fromYear != null || toYear != null) {
            List<Book> books = bookService.advancedSearch(titleParam, authorParam, isbnParam,
                                                          publisherParam, statusParam, fromYear, toYear);
            model.addAttribute("books", books);
        }

        // Add search parameters back to the model for form retention
        model.addAttribute("title", title);
        model.addAttribute("author", author);
        model.addAttribute("isbn", isbn);
        model.addAttribute("publisher", publisher);
        model.addAttribute("status", status);
        model.addAttribute("fromYear", fromYear);
        model.addAttribute("toYear", toYear);
        model.addAttribute("bookStatuses", BookStatus.values());

        return "advanced-search";
    }
}
