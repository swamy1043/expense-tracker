package com.teerthu.expensetracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;


import com.teerthu.expensetracker.model.User;
import com.teerthu.expensetracker.repository.UserRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // REGISTER
    @PostMapping("/register")
    public String register(@RequestBody User user) {

        User existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser != null) {
            return "Username already taken!";
        }

        // encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        return "Registration successful!";
    }

    // LOGIN
    @PostMapping("/login")
    public String login(@RequestBody User user) {

        User existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser == null) {
            return "User not found!";
        }

        if (!passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
            return "Wrong password!";
        }

        return "Login success!";
    }
}
