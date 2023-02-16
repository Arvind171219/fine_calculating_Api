package com.example.Student_Library_Management_System.Repositories;

import com.example.Student_Library_Management_System.Models.Book;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    @Modifying
    @Transactional
    @Query("update Book b set b.available =:#{#book.available}, b.card =:#{#book.card} where b.id =:#{#book.id}")
    int updateBook(Book book);

}
