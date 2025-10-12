package com.library.system.service;

import com.library.system.domain.Book;
import com.library.system.domain.BookStatus;
import com.library.system.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("978-1234567890");
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setPublisher("Test Publisher");
        testBook.setPublicationDate(LocalDate.of(2023, 1, 1));
        testBook.setDescription("Test Description");
        testBook.setStatus(BookStatus.AVAILABLE);
    }

    @Test
    void getAllBooks() {
        // Given
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findAll()).thenReturn(books);

        // When
        List<Book> result = bookService.getAllBooks();

        // Then
        assertEquals(1, result.size());
        assertEquals(testBook, result.get(0));
        verify(bookRepository).findAll();
    }

    @Test
    void getBookById_Success() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // When
        Optional<Book> result = bookService.getBookById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testBook, result.get());
        verify(bookRepository).findById(1L);
    }

    @Test
    void getBookById_NotFound() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<Book> result = bookService.getBookById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(bookRepository).findById(1L);
    }

    @Test
    void getBookByIsbn_Success() {
        // Given
        when(bookRepository.findByIsbn("978-1234567890")).thenReturn(Optional.of(testBook));

        // When
        Optional<Book> result = bookService.getBookByIsbn("978-1234567890");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testBook, result.get());
        verify(bookRepository).findByIsbn("978-1234567890");
    }

    @Test
    void searchBooks_WithSearchTerm() {
        // Given
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.searchBooks("test")).thenReturn(books);

        // When
        List<Book> result = bookService.searchBooks("test");

        // Then
        assertEquals(1, result.size());
        assertEquals(testBook, result.get(0));
        verify(bookRepository).searchBooks("test");
    }

    @Test
    void searchBooks_EmptySearchTerm() {
        // Given
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findAll()).thenReturn(books);

        // When
        List<Book> result = bookService.searchBooks("");

        // Then
        assertEquals(1, result.size());
        assertEquals(testBook, result.get(0));
        verify(bookRepository).findAll();
    }

    @Test
    void searchBooks_NullSearchTerm() {
        // Given
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findAll()).thenReturn(books);

        // When
        List<Book> result = bookService.searchBooks(null);

        // Then
        assertEquals(1, result.size());
        assertEquals(testBook, result.get(0));
        verify(bookRepository).findAll();
    }

    @Test
    void getAvailableBooks() {
        // Given
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByStatus(BookStatus.AVAILABLE)).thenReturn(books);

        // When
        List<Book> result = bookService.getAvailableBooks();

        // Then
        assertEquals(1, result.size());
        assertEquals(testBook, result.get(0));
        verify(bookRepository).findByStatus(BookStatus.AVAILABLE);
    }

    @Test
    void searchAvailableBooks_WithSearchTerm() {
        // Given
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.searchBooksByStatus("test", BookStatus.AVAILABLE)).thenReturn(books);

        // When
        List<Book> result = bookService.searchAvailableBooks("test");

        // Then
        assertEquals(1, result.size());
        assertEquals(testBook, result.get(0));
        verify(bookRepository).searchBooksByStatus("test", BookStatus.AVAILABLE);
    }

    @Test
    void saveBook() {
        // Given
        when(bookRepository.save(testBook)).thenReturn(testBook);

        // When
        Book result = bookService.saveBook(testBook);

        // Then
        assertEquals(testBook, result);
        verify(bookRepository).save(testBook);
    }

    @Test
    void deleteBook() {
        // When
        bookService.deleteBook(1L);

        // Then
        verify(bookRepository).deleteById(1L);
    }

    @Test
    void updateBookStatus_Success() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(testBook)).thenReturn(testBook);

        // When
        Book result = bookService.updateBookStatus(1L, BookStatus.BORROWED);

        // Then
        assertEquals(BookStatus.BORROWED, result.getStatus());
        verify(bookRepository).findById(1L);
        verify(bookRepository).save(testBook);
    }

    @Test
    void updateBookStatus_BookNotFound() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> bookService.updateBookStatus(1L, BookStatus.BORROWED));
    }

    @Test
    void isBookAvailable_Available() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // When
        boolean result = bookService.isBookAvailable(1L);

        // Then
        assertTrue(result);
        verify(bookRepository).findById(1L);
    }

    @Test
    void isBookAvailable_NotAvailable() {
        // Given
        testBook.setStatus(BookStatus.BORROWED);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // When
        boolean result = bookService.isBookAvailable(1L);

        // Then
        assertFalse(result);
        verify(bookRepository).findById(1L);
    }

    @Test
    void isBookAvailable_BookNotFound() {
        // Given
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        boolean result = bookService.isBookAvailable(1L);

        // Then
        assertFalse(result);
        verify(bookRepository).findById(1L);
    }
}
