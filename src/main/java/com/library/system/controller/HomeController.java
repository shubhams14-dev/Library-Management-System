package com.library.system.controller;

import com.library.system.domain.Book;
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
}
