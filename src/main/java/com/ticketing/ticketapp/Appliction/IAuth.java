package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.User.UserDTO;

public interface IAuth {

    Response<String> register(String token, String username, String password, int age, String email);

    Response<String> login(String token, String username, String password);

    Response<String> logout(String token);
    
    Response<UserDTO> getUserProfile(String token);

    Response<String> updateUserPassword(String token, String newPassword);

}
