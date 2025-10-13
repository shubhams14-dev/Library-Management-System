package com.library.system.controller;

import com.library.system.domain.Book;
import com.library.system.service.BookService;
import com.library.system.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/books")
public class BookController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public String bookDetails(@PathVariable Long id, Model model) {
        // Add current user if authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            userService.getUserByUsername(authentication.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        Optional<Book> book = bookService.getBookById(id);
        if (book.isPresent()) {
            model.addAttribute("book", book.get());
            model.addAttribute("isAvailable", bookService.isBookAvailable(id));
            return "book-details";
        } else {
            return "redirect:/?error=Book not found";
        }
    }
    
    @GetMapping("/isbn/{isbn}")
    public String bookDetailsByIsbn(@PathVariable String isbn, Model model) {
        // Add current user if authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            userService.getUserByUsername(authentication.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        Optional<Book> book = bookService.getBookByIsbn(isbn);
        if (book.isPresent()) {
            model.addAttribute("book", book.get());
            model.addAttribute("isAvailable", bookService.isBookAvailable(book.get().getId()));
            return "book-details";
        } else {
            return "redirect:/?error=Book not found";
        }
    }
}
