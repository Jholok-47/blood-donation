//package com.lifelink.blood_donation.Controllers;
//
//import com.lifelink.blood_donation.Entities.User;
//import com.lifelink.blood_donation.Repositories.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class UserController {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @PostMapping("/user/create")
//    public User createUser(@RequestBody User user) {
//        return userRepository.save(user);
//    }
//}
