package com.teerthu.expensetracker.controller;

import com.teerthu.expensetracker.dto.ExpenseRequest;
import com.teerthu.expensetracker.model.Category;
import com.teerthu.expensetracker.model.Expense;
import com.teerthu.expensetracker.model.User;
import com.teerthu.expensetracker.repository.CategoryRepository;
import com.teerthu.expensetracker.repository.ExpenseRepository;
import com.teerthu.expensetracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // ADD
    @PostMapping("/add")
    public String addExpense(@RequestBody ExpenseRequest request) {
        User user = userRepository.findByUsername(request.username);
        if (user == null) return "User not found!";

        Category cat = categoryRepository.findByName(request.category);
        if (cat == null) {
            cat = new Category();
            cat.setName(request.category);
            categoryRepository.save(cat);
        }

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setAmount(request.amount);
        expense.setCategory(cat);
        expense.setDescription(request.description);

        try {
            Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(request.date);
            expense.setDate(parsedDate);
        } catch (Exception e) {
            return "Invalid date format. Use yyyy-MM-dd";
        }

        expenseRepository.save(expense);
        return "Expense added!";
    }

    // LIST
    @GetMapping("/list")
    public List<Expense> list(@RequestParam String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) return Collections.emptyList();
        return expenseRepository.findByUser(user);
    }

    // SUMMARY
    @GetMapping("/summary")
    public Map<String, Object> getSummary(@RequestParam String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) return Collections.emptyMap();
        List<Expense> list = expenseRepository.findByUser(user);

        double total = 0;
        Map<String, Double> categoryBreakdown = new HashMap<>();
        for (Expense e : list) {
            total += e.getAmount();
            String cat = e.getCategory() != null ? e.getCategory().getName() : "Uncategorized";
            categoryBreakdown.put(cat, categoryBreakdown.getOrDefault(cat, 0.0) + e.getAmount());
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("categorySummary", categoryBreakdown);
        return summary;
    }

    // DELETE
    @DeleteMapping("/delete/{id}")
    public String deleteExpense(@PathVariable Long id) {
        if (!expenseRepository.existsById(id)) return "Expense not found!";
        expenseRepository.deleteById(id);
        return "Expense deleted!";
    }

    // UPDATE
    @PutMapping("/update/{id}")
    public String updateExpense(@PathVariable Long id, @RequestBody ExpenseRequest req) {
        if (!expenseRepository.existsById(id)) return "Expense not found!";
        Expense expense = expenseRepository.findById(id).orElse(null);
        if (expense == null) return "Expense not found!";
        User user = userRepository.findByUsername(req.username);
        if (user == null) return "User not found!";

        Category cat = categoryRepository.findByName(req.category);
        if (cat == null) {
            cat = new Category();
            cat.setName(req.category);
            categoryRepository.save(cat);
        }

        expense.setUser(user);
        expense.setAmount(req.amount);
        expense.setCategory(cat);
        expense.setDescription(req.description);

        try {
            Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(req.date);
            expense.setDate(parsedDate);
        } catch (Exception e) {
            return "Invalid date!";
        }

        expenseRepository.save(expense);
        return "Expense updated!";
    }

    // FILTER (same as before)
    @GetMapping("/filter")
    public List<Expense> filter(
            @RequestParam String username,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String category
    ) {
        User user = userRepository.findByUsername(username);
        if (user == null) return Collections.emptyList();
        List<Expense> all = expenseRepository.findByUser(user);

        if (category != null && !category.isBlank()) {
            all = all.stream()
                    .filter(e -> e.getCategory() != null && category.equalsIgnoreCase(e.getCategory().getName()))
                    .collect(Collectors.toList());
        }

        if (month != null && year != null) {
            Calendar start = Calendar.getInstance();
            start.set(year, month - 1, 1, 0, 0, 0); start.set(Calendar.MILLISECOND, 0);
            Calendar end = Calendar.getInstance();
            end.set(year, month - 1, start.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59); end.set(Calendar.MILLISECOND, 999);
            Date s = start.getTime(); Date e = end.getTime();
            all = all.stream().filter(exp -> exp.getDate() != null && !exp.getDate().before(s) && !exp.getDate().after(e)).collect(Collectors.toList());
        }

        if (from != null && to != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date s = sdf.parse(from);
                Date t = sdf.parse(to);
                Calendar c = Calendar.getInstance(); c.setTime(t); c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59); c.set(Calendar.SECOND, 59); t = c.getTime();
                Date finalS = s; Date finalT = t;
                all = all.stream().filter(exp -> exp.getDate() != null && !exp.getDate().before(finalS) && !exp.getDate().after(finalT)).collect(Collectors.toList());
            } catch (Exception ex) { }
        }

        all.sort(Comparator.comparing(Expense::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return all;
    }

    // EXPORT CSV - server-side (returns CSV text)
    @GetMapping("/export")
    public ResponseEntity<String> exportCSV(
            @RequestParam String username,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String category
    ) {
        List<Expense> list = filter(username, from, to, month, year, category);
        List<String> lines = new ArrayList<>();
        lines.add("id,username,category,description,amount,date");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Expense e : list) {
            String uname = e.getUser() != null ? e.getUser().getUsername() : username;
            String cat = e.getCategory() != null ? e.getCategory().getName() : "";
            String desc = e.getDescription() != null ? e.getDescription().replace("\"","\"\"") : "";
            String date = e.getDate() != null ? sdf.format(e.getDate()) : "";
            String row = String.format("%d,\"%s\",\"%s\",\"%s\",%s,%s",
                    e.getId(),
                    uname,
                    cat,
                    desc,
                    String.valueOf(e.getAmount()),
                    date);
            lines.add(row);
        }
        String csv = String.join("\n", lines);
        String filename = String.format("expenses_%s_%s.csv", username, new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
