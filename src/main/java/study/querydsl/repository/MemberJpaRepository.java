package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.MemberDSL;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Repository
//@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em){
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public void save(MemberDSL member){
        em.persist(member);
    }
    public Optional<MemberDSL> findById(Long id){
        MemberDSL findMember = em.find(MemberDSL.class, id);
        return Optional.of(findMember);
    }
    public List<MemberDSL> findAll(){
        return em.createQuery("select m from MemberDSL m", MemberDSL.class)
                .getResultList();
    }
    public List<MemberDSL> findByUsername(String username){
        return em.createQuery("select m from MemberDSL m" +
                " where m.username = :username", MemberDSL.class)
                .setParameter("username", username)
                .getResultList();
    }
}
