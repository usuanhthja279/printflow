package com.printflow.shared;
import com.printflow.enquiry.TokenUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class RequestLimits {
 private final JdbcTemplate jdbc;private final String namespace;
 public RequestLimits(JdbcTemplate jdbc,@org.springframework.beans.factory.annotation.Value("${APP_RATE_NAMESPACE:default}") String namespace){this.jdbc=jdbc;this.namespace=namespace;}
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,noRollbackFor=ResponseStatusException.class) public void require(String scope,String key,int maximum){
  int hits=jdbc.queryForObject("insert into request_limits(key_hash) values(?) on conflict(key_hash) do update set hits=case when request_limits.window_start<now()-interval '15 minutes' then 1 else request_limits.hits+1 end, window_start=case when request_limits.window_start<now()-interval '15 minutes' then now() else request_limits.window_start end returning hits",Integer.class,TokenUtil.sha256(namespace+":"+scope+":"+key));
  if(hits>maximum)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many requests. Try again in 15 minutes");
 }
}
