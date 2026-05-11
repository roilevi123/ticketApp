package com.ticketing.ticketapp.Appliction;

public interface IAuth {

    String register(String token, String username, String password,int age);

    String login(String token, String username, String password);

    String logout(String token);

    String getUserInfo(String token);

    String updateUserPassword(String token, String newPassword);

    String updateUserName(String token, String newUsername);

}
