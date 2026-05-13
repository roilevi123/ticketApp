package com.ticketing.ticketapp.Appliction;

public interface IAuth {

    Response<String> register(String token, String username, String password, int age);

    Response<String> login(String token, String username, String password);

    Response<String> logout(String token);

    Response<String> getUserInfo(String token);

    Response<String> updateUserPassword(String token, String newPassword);

}
