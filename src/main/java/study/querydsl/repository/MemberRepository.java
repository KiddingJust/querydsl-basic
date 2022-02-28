package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.MemberDSL;

import java.util.List;

public interface MemberRepository extends JpaRepository<MemberDSL, Long>, MemberRepositoryCustom {
    //스프링 데이터 JPA에서 제공하지 않는 메서드만.
    List<MemberDSL> findByUsername(String username);
}
