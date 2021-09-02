package com.example.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Login;



public interface LoginRepo extends JpaRepository<Login,Long>   
{    
	Login findByUsernameAndPassword(String username,String password);
}