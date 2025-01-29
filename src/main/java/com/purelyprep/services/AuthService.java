//package com.purelyprep.services;
//
//import com.purelyprep.pojo.User;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//
//@Service
//public class AuthService {
//
//    private final PasswordService passwordService;
//    private final JwtService jwtService;
//
//    public AuthService(PasswordService passwordService, JwtService jwtService) {
//        this.passwordService = passwordService;
//        this.jwtService = jwtService;
//    }
//
//    public User getContextUser() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null) {
//            return null;
//        }
//        return (User) authentication.getPrincipal();
//    }
//
//    public String authenticate(String username, String password) {
//        User user = getUser(username);
//        if (user == null) {
//            return null;
//        }
//        if (passwordService.matches(password, user.encodedPassword)) {
//            return jwtService.generateToken(user);
//        }
//        return null;
//    }
//
//    private User getUser(String username) {
//        return new User();
//    }
//
//}
