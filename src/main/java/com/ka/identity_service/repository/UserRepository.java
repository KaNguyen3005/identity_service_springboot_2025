package com.ka.identity_service.repository;

//Repository dùng tương tác với DBMS(JPA)

import com.ka.identity_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//Tham số thứ 2 là id của class
//1 request qua >= 3 layer
//controller(mapping endpoint) -> service (xử lí logic) -> repository (tương tác dbms)
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
}
