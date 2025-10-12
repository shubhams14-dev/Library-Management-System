package com.library.system.controller;

import com.library.system.domain.Book;
import com.library.system.service.BookService;
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
    
    @GetMapping("/{id}")
    public String bookDetails(@PathVariable Long id, Model model) {
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
