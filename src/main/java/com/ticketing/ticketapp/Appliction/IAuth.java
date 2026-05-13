package com.ticketing.ticketapp.Appliction;

import com.ticketing.ticketapp.Domain.User.UserDTO;

public interface IAuth {

    String register(String token, String username, String password,int age);

    String login(String token, String username, String password);

    String logout(String token);

    Response<UserDTO> getUserProfile(String token);

    String updateUserPassword(String token, String newPassword);


}
