package com.library.system.service;

import com.library.system.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class DataInitializationService implements CommandLineRunner {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookService bookService;
    
    @Override
    public void run(String... args) throws Exception {
        initializeUsers();
        initializeBooks();
    }
    
    private void initializeUsers() {
        // Create admin user
        if (!userService.existsByUsername("admin")) {
            userService.createAdmin("admin", "admin123", "System Administrator", "admin@library.com");
        }
        
        // Create member users
        if (!userService.existsByUsername("john.doe")) {
            userService.createMember("john.doe", "password123", "John Doe", "john.doe@email.com");
        }
        
        if (!userService.existsByUsername("jane.smith")) {
            userService.createMember("jane.smith", "password123", "Jane Smith", "jane.smith@email.com");
        }
        
        if (!userService.existsByUsername("bob.wilson")) {
            userService.createMember("bob.wilson", "password123", "Bob Wilson", "bob.wilson@email.com");
        }
        
        if (!userService.existsByUsername("alice.brown")) {
            userService.createMember("alice.brown", "password123", "Alice Brown", "alice.brown@email.com");
        }
    }
    
    private void initializeBooks() {
        List<Book> books = Arrays.asList(
            new Book("978-0134685991", "Effective Java", "Joshua Bloch", "Addison-Wesley", 
                    LocalDate.of(2017, 12, 27), "A comprehensive guide to Java programming best practices."),
            
            new Book("978-0596009205", "Head First Design Patterns", "Eric Freeman, Elisabeth Robson", "O'Reilly Media", 
                    LocalDate.of(2004, 10, 25), "A brain-friendly guide to design patterns."),
            
            new Book("978-0132350884", "Clean Code", "Robert C. Martin", "Prentice Hall", 
                    LocalDate.of(2008, 8, 1), "A handbook of agile software craftsmanship."),
            
            new Book("978-0201633610", "Design Patterns", "Gang of Four", "Addison-Wesley", 
                    LocalDate.of(1994, 10, 21), "Elements of reusable object-oriented software."),
            
            new Book("978-0134685991", "Spring in Action", "Craig Walls", "Manning Publications", 
                    LocalDate.of(2018, 12, 1), "A comprehensive guide to Spring Framework."),
            
            new Book("978-1491950357", "Building Microservices", "Sam Newman", "O'Reilly Media", 
                    LocalDate.of(2015, 2, 20), "Designing fine-grained systems."),
            
            new Book("978-0135957059", "The Pragmatic Programmer", "David Thomas, Andrew Hunt", "Addison-Wesley", 
                    LocalDate.of(2019, 9, 13), "Your journey to mastery."),
            
            new Book("978-0134685991", "Java: The Complete Reference", "Herbert Schildt", "McGraw-Hill", 
                    LocalDate.of(2017, 12, 1), "The definitive guide to Java programming."),
            
            new Book("978-0596007126", "Head First Java", "Kathy Sierra, Bert Bates", "O'Reilly Media", 
                    LocalDate.of(2005, 2, 9), "A brain-friendly guide to Java programming."),
            
            new Book("978-0134685991", "Java Concurrency in Practice", "Brian Goetz", "Addison-Wesley", 
                    LocalDate.of(2006, 5, 9), "A comprehensive guide to concurrent programming in Java.")
        );
        
        for (Book book : books) {
            if (!bookService.getBookByIsbn(book.getIsbn()).isPresent()) {
                bookService.saveBook(book);
            }
        }
    }
}
