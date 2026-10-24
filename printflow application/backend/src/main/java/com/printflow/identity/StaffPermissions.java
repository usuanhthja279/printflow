package com.printflow.identity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class StaffPermissions {
 private final JdbcTemplate db;
 public StaffPermissions(JdbcTemplate db){this.db=db;}
 public boolean contacts(StaffPrincipal user){return !user.role().equals("CUSTOMER")&&(user.role().equals("OWNER")||Boolean.TRUE.equals(db.queryForObject("select contacts_access from staff_users where id=? and enabled",Boolean.class,user.id())));}
 public void requireContacts(StaffPrincipal user){if(!contacts(user))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"The owner has not granted Contacts access");}
}
