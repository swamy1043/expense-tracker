package com.teerthu.expensetracker.repository;

import com.teerthu.expensetracker.model.Expense;
import com.teerthu.expensetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUser(User user);
    List<Expense> findByUserAndDateBetween(User user, Date start, Date end);
    // Spring Data supports nested property finders:
    List<Expense> findByUserAndCategory_Name(User user, String categoryName);
}
